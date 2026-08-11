package com.example.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import com.example.data.model.Message

/**
 * CryptoManager: Handles secure E2EE operations for Panalink using AndroidKeyStore,
 * Elliptic Curve Diffie-Hellman (ECDH) for key agreement, and AES-256-GCM for symmetric encryption.
 */
object CryptoManager {
    const val ENABLE_E2EE = false // Temporalmente desactivado
    private const val TAG = "CryptoManager"
    private const val ALIAS = "panalink_e2ee_key_v2"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"

    private fun logError(tag: String, msg: String, t: Throwable? = null) {
        try {
            if (t != null) {
                Log.e(tag, msg, t)
            } else {
                Log.e(tag, msg)
            }
        } catch (e: Throwable) {
            println("[ERROR] $tag: $msg")
            t?.printStackTrace()
        }
    }

    private fun logDebug(tag: String, msg: String) {
        try {
            Log.d(tag, msg)
        } catch (e: Throwable) {
            println("[DEBUG] $tag: $msg")
        }
    }

    // High-performance in-memory caches to enable synchronous decryption on the fly
    val publicKeyCache = ConcurrentHashMap<String, String>()
    val chatToOtherUserCache = ConcurrentHashMap<String, String>()

    private var localKeyPair: java.security.KeyPair? = null

    init {
        ensureKeyPairExists()
    }

    /**
     * Sanitizes and normalizes a Base64 encoded public key string.
     */
    fun cleanPublicKey(keyStr: String?): String {
        if (keyStr.isNullOrBlank()) return ""
        return keyStr.trim()
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("-----BEGIN EC PUBLIC KEY-----", "")
            .replace("-----END EC PUBLIC KEY-----", "")
            .replace("\"", "")
            .replace("'", "")
            .replace("\\n", "")
            .replace("\\r", "")
            .replace("\n", "")
            .replace("\r", "")
            .replace(" ", "")
            .replace("\t", "")
            .trim()
    }

    /**
     * Safely decodes a Base64 string to ByteArray.
     */
    private fun decodeBase64Key(base64Str: String): ByteArray {
        val cleaned = cleanPublicKey(base64Str)
        return try {
            Base64.decode(cleaned, Base64.NO_WRAP)
        } catch (e: Throwable) {
            Base64.decode(cleaned, Base64.DEFAULT)
        }
    }

    /**
     * Generates and stores the EC key pair securely inside AndroidKeyStore (Hardware-backed).
     */
    @Synchronized
    fun ensureKeyPairExists() {
        if (localKeyPair != null) return

        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER)
            keyStore.load(null)

            if (!keyStore.containsAlias(ALIAS)) {
                logDebug(TAG, "Generating new hardware-backed EC key pair (secp256r1)")
                val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER)
                val spec = KeyGenParameterSpec.Builder(
                    ALIAS,
                    KeyProperties.PURPOSE_AGREE_KEY
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build()
                kpg.initialize(spec)
                kpg.generateKeyPair()
            } else {
                logDebug(TAG, "Loaded existing EC key pair from AndroidKeyStore")
            }

            val entry = keyStore.getEntry(ALIAS, null) as? KeyStore.PrivateKeyEntry
            if (entry != null) {
                localKeyPair = java.security.KeyPair(entry.certificate.publicKey, entry.privateKey)
            }
        } catch (e: Throwable) {
            logError(TAG, "KeyStore failed, falling back to software keys", e)
            ensureSoftwareKeyPair()
        }
    }

    private fun ensureSoftwareKeyPair() {
        if (localKeyPair != null) return
        try {
            val keyPairGenerator = KeyPairGenerator.getInstance("EC")
            keyPairGenerator.initialize(ECGenParameterSpec("secp256r1"))
            localKeyPair = keyPairGenerator.generateKeyPair()
        } catch (e: Throwable) {
            logError(TAG, "Software key generation failed", e)
        }
    }

    /**
     * Gets the local public key encoded in Base64.
     */
    fun getPublicKeyBase64(): String {
        return try {
            ensureKeyPairExists()
            val keyPair = localKeyPair ?: return ""
            cleanPublicKey(Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP))
        } catch (e: Throwable) {
            logError(TAG, "Error getting public key", e)
            ""
        }
    }

    /**
     * Retrieves the private key securely.
     */
    private fun getPrivateKey(): PrivateKey? {
        ensureKeyPairExists()
        return localKeyPair?.private
    }

    /**
     * Performs ECDH Key Agreement to derive a shared secret.
     */
    private fun getSharedSecret(otherPublicKeyBase64: String): ByteArray? {
        val privateKey = getPrivateKey() ?: return null
        val cleanedKeyStr = cleanPublicKey(otherPublicKeyBase64)
        if (cleanedKeyStr.isEmpty()) return null
        
        val keyFactory = KeyFactory.getInstance("EC")
        val otherKeyBytes = decodeBase64Key(cleanedKeyStr)
        val otherPublicKey = keyFactory.generatePublic(X509EncodedKeySpec(otherKeyBytes))

        val keyAgreement = KeyAgreement.getInstance("ECDH")
        keyAgreement.init(privateKey)
        keyAgreement.doPhase(otherPublicKey, true)
        return keyAgreement.generateSecret()
    }

    /**
     * Hashes the shared secret using SHA-256 to derive a high-entropy AES-256 key.
     */
    private fun deriveAESKey(sharedSecret: ByteArray): SecretKeySpec {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashedSecret = messageDigest.digest(sharedSecret)
        return SecretKeySpec(hashedSecret, "AES")
    }

    /**
     * Encrypts plain text using the receiver's public key (via ECDH + AES-256-GCM).
     * Returns "IV + Ciphertext" in Base64 format.
     */
    fun encrypt(plainText: String, receiverPublicKeyBase64: String): String {
        if (!ENABLE_E2EE) return plainText
        if (plainText.isEmpty() || receiverPublicKeyBase64.isEmpty()) return plainText
        val cleanedKey = cleanPublicKey(receiverPublicKeyBase64)
        if (cleanedKey.isEmpty()) return plainText
        return try {
            val sharedSecret = getSharedSecret(cleanedKey) ?: return plainText
            val aesKey = deriveAESKey(sharedSecret)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(12)
            SecureRandom().nextBytes(iv)
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
            
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Throwable) {
            logError(TAG, "Encryption failed for public key: ${cleanedKey.take(15)}...", e)
            throw e
        }
    }

    /**
     * Decrypts ciphertext (Base64 IV + Ciphertext) using the sender's public key (via ECDH + AES-256-GCM).
     */
    fun decrypt(encryptedPayloadBase64: String, senderPublicKeyBase64: String): String {
        if (encryptedPayloadBase64.isEmpty() || senderPublicKeyBase64.isEmpty()) return encryptedPayloadBase64
        val cleanedKey = cleanPublicKey(senderPublicKeyBase64)
        if (cleanedKey.isEmpty()) return encryptedPayloadBase64
        val combined = try {
            decodeBase64Key(encryptedPayloadBase64)
        } catch (e: Throwable) {
            return encryptedPayloadBase64
        }
        if (combined.size < 12) return encryptedPayloadBase64

        return try {
            val sharedSecret = getSharedSecret(cleanedKey) ?: return "[Mensaje cifrado]"
            val aesKey = deriveAESKey(sharedSecret)

            val iv = ByteArray(12)
            System.arraycopy(combined, 0, iv, 0, 12)
            
            val cipherText = ByteArray(combined.size - 12)
            System.arraycopy(combined, 12, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
            
            val decryptedBytes = cipher.doFinal(cipherText)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Throwable) {
            encryptedPayloadBase64
        }
    }

    /**
     * Pure function to decrypt a message without performing any database/network queries.
     */
    fun decryptMessagePure(msg: Message, chatType: String?, otherUserPublicKey: String?): Message {
        if (!ENABLE_E2EE) return msg
        val rawContent = msg.content
        if (rawContent.isNullOrEmpty()) return msg

        if (chatType == "channel" || chatType == "community") return msg

        if (otherUserPublicKey.isNullOrBlank()) {
            return msg.copy(content = "[Mensaje cifrado]")
        }

        val cleanKey = cleanPublicKey(otherUserPublicKey)
        if (cleanKey.isEmpty()) {
            return msg.copy(content = "[Mensaje cifrado]")
        }

        logDebug("ChatE2ETrace", "decryptMessagePure | pubKey=${cleanKey.take(15)}...")
        val decrypted = decrypt(rawContent, cleanKey)
        if (decrypted != rawContent) {
            logDebug("ChatE2ETrace", "decryptMessagePure result OK")
        }
        return msg.copy(content = decrypted)
    }

    /**
     * Decrypts a message's text_content if needed using the other participant's public key.
     */
    suspend fun decryptMessageIfNeeded(msg: Message): Message {
        if (!ENABLE_E2EE) return msg
        val rawContent = msg.content
        if (rawContent.isNullOrEmpty()) return msg

        // Check if this chat is a channel or community to skip decryption
        val isChannelOrCommunity = try {
            val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            val chatEntity = db.chatDao().getChatById(msg.chatId)
            chatEntity?.type == "channel" || chatEntity?.type == "community"
        } catch (e: Throwable) {
            false
        }
        if (isChannelOrCommunity) return msg

        val currentUid = try { com.example.data.supabase.SupabaseClient.currentUser?.id ?: "" } catch (e: Throwable) { "" }
 
        val otherUserId = if (msg.senderId != currentUid && msg.senderId.isNotEmpty()) {
            msg.senderId
        } else {
            chatToOtherUserCache[msg.chatId] ?: run {
                try {
                    val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
                    val chatEntity = db.chatDao().getChatById(msg.chatId)
                    val uid = chatEntity?.otherUserId
                    if (!uid.isNullOrEmpty()) chatToOtherUserCache[msg.chatId] = uid
                    uid
                } catch (e: Throwable) {
                    null
                }
            }
        }
 
        if (!otherUserId.isNullOrEmpty()) {
            var otherPubKey = publicKeyCache[otherUserId]
            var retries = 3
            while (otherPubKey.isNullOrEmpty() && retries > 0) {
                if (retries < 3) {
                    try {
                        kotlinx.coroutines.delay(400)
                    } catch (e: Throwable) {}
                }
                if (otherPubKey.isNullOrEmpty()) {
                    try {
                        otherPubKey = com.example.data.repository.UserKeysRepository.getPublicKeyForUser(otherUserId)
                        if (!otherPubKey.isNullOrEmpty()) {
                            val cleanKey = cleanPublicKey(otherPubKey)
                            if (cleanKey.isNotEmpty()) {
                                publicKeyCache[otherUserId] = cleanKey
                                otherPubKey = cleanKey
                                break
                            }
                        }
                    } catch (e: Throwable) {}
                }

                retries--
            }
 
            val cleanedPubKey = cleanPublicKey(otherPubKey)
            if (cleanedPubKey.isNotEmpty()) {
                return decryptMessagePure(msg, if (isChannelOrCommunity) "channel" else "direct", cleanedPubKey)
            } else {
                logError("ChatE2ETrace", "3. Resultado decrypt ERROR: Clave pública vacía para usuario $otherUserId")
            }
        } else {
            logError("ChatE2ETrace", "3. Resultado decrypt ERROR: No se encontró otro usuario para chatId=${msg.chatId}")
            return msg
        }
        return msg.copy(content = "[Mensaje cifrado]")
    }
}
