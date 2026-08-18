package com.example.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.data.model.Message
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.MessageDigest

/** E2EE for direct messages: ECDH P-256 + AES-256-GCM. */
object CryptoManager {
    const val ENABLE_E2EE = true
    private const val TAG = "CryptoManager"
    private const val ALIAS = "panalink_e2ee_key_v2"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val IV_SIZE = 12
    private const val TAG_SIZE_BITS = 128
    private const val ENCRYPTION_ERROR = "E2EE encryption unavailable"

    val publicKeyCache = ConcurrentHashMap<String, String>()
    val chatToOtherUserCache = ConcurrentHashMap<String, String>()
    private var localKeyPair: KeyPair? = null

    init {
        runCatching { ensureKeyPairExists() }
            .onFailure { Log.e(TAG, "Secure E2EE key initialization deferred", it) }
    }

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

    private fun decodeBase64(value: String): ByteArray {
        val cleaned = value.trim()
        require(cleaned.isNotEmpty()) { "Empty Base64 value" }
        return try {
            Base64.decode(cleaned, Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            Base64.decode(cleaned, Base64.DEFAULT)
        }
    }

    @Synchronized
    fun ensureKeyPairExists() {
        if (localKeyPair != null) return
        try {
            val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (!ks.containsAlias(ALIAS)) {
                val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, KEYSTORE_PROVIDER)
                generator.initialize(
                    KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_AGREE_KEY)
                        .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .build()
                )
                generator.generateKeyPair()
            }
            val entry = ks.getEntry(ALIAS, null) as? KeyStore.PrivateKeyEntry
                ?: error("E2EE KeyStore entry is missing")
            localKeyPair = KeyPair(entry.certificate.publicKey, entry.privateKey)
        } catch (e: Throwable) {
            Log.e(TAG, "AndroidKeyStore unavailable; refusing non-persistent E2EE key", e)
            localKeyPair = null
            throw IllegalStateException("Secure E2EE key storage is unavailable", e)
        }
    }

    fun getPublicKeyBase64(): String {
        ensureKeyPairExists()
        return Base64.encodeToString(localKeyPair!!.public.encoded, Base64.NO_WRAP)
    }

    private fun getPrivateKey(): PrivateKey {
        ensureKeyPairExists()
        return localKeyPair!!.private
    }

    private fun getSharedSecret(otherPublicKeyBase64: String): ByteArray {
        val otherBytes = decodeBase64(otherPublicKeyBase64)
        val otherPublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(otherBytes))
        return KeyAgreement.getInstance("ECDH").run {
            init(getPrivateKey())
            doPhase(otherPublicKey, true)
            generateSecret()
        }
    }

    private fun deriveAESKey(sharedSecret: ByteArray): SecretKeySpec =
        SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(sharedSecret), "AES")

    fun encrypt(plainText: String, receiverPublicKeyBase64: String): String {
        if (!ENABLE_E2EE || plainText.isEmpty()) return plainText
        val cleaned = cleanPublicKey(receiverPublicKeyBase64)
        require(cleaned.isNotEmpty()) { "Missing receiver public key; refusing plaintext fallback" }
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val iv = ByteArray(IV_SIZE).also(SecureRandom()::nextBytes)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                deriveAESKey(getSharedSecret(cleaned)),
                GCMParameterSpec(TAG_SIZE_BITS, iv)
            )
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
        } catch (e: Throwable) {
            Log.e(TAG, ENCRYPTION_ERROR, e)
            throw IllegalStateException(ENCRYPTION_ERROR, e)
        }
    }

    fun decrypt(encryptedPayloadBase64: String, senderPublicKeyBase64: String): String {
        if (encryptedPayloadBase64.isEmpty()) return encryptedPayloadBase64
        val cleaned = cleanPublicKey(senderPublicKeyBase64)
        if (cleaned.isEmpty()) return "[Mensaje cifrado]"
        return try {
            val combined = decodeBase64(encryptedPayloadBase64)
            if (combined.size <= IV_SIZE) return "[Mensaje cifrado]"
            val iv = combined.copyOfRange(0, IV_SIZE)
            val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                deriveAESKey(getSharedSecret(cleaned)),
                GCMParameterSpec(TAG_SIZE_BITS, iv)
            )
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Throwable) {
            Log.w(TAG, "E2EE decryption failed", e)
            "[Mensaje cifrado]"
        }
    }

    fun decryptMessagePure(msg: Message, chatType: String?, otherUserPublicKey: String?): Message {
        if (!ENABLE_E2EE || msg.content.isNullOrEmpty()) return msg
        if (chatType == "channel" || chatType == "community" || chatType == "group") return msg
        val key = cleanPublicKey(otherUserPublicKey)
        if (key.isEmpty()) return msg.copy(content = "[Mensaje cifrado]")
        return msg.copy(content = decrypt(msg.content, key))
    }

    suspend fun decryptMessageIfNeeded(msg: Message): Message {
        if (!ENABLE_E2EE || msg.content.isNullOrEmpty()) return msg
        val isChannelOrCommunity = try {
            val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            val type = db.chatDao().getChatById(msg.chatId)?.type
            type == "channel" || type == "community" || type == "group"
        } catch (_: Throwable) {
            false
        }
        if (isChannelOrCommunity) return msg

        val currentUid = try {
            com.example.data.supabase.SupabaseClient.currentUser?.id ?: ""
        } catch (_: Throwable) {
            ""
        }
        val otherUserId = if (msg.senderId != currentUid && msg.senderId.isNotEmpty()) {
            msg.senderId
        } else {
            chatToOtherUserCache[msg.chatId] ?: run {
                try {
                    val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
                    val id = db.chatDao().getChatById(msg.chatId)?.otherUserId
                    if (!id.isNullOrEmpty()) chatToOtherUserCache[msg.chatId] = id
                    id
                } catch (_: Throwable) {
                    null
                }
            }
        }
        if (otherUserId.isNullOrEmpty()) return msg.copy(content = "[Mensaje cifrado]")

        var publicKey = publicKeyCache[otherUserId]
        repeat(3) { attempt ->
            if (!publicKey.isNullOrEmpty()) return@repeat
            try {
                if (attempt > 0) kotlinx.coroutines.delay(400)
                publicKey = com.example.data.repository.UserKeysRepository.getPublicKeyForUser(otherUserId)
            } catch (_: Throwable) {
                // Fail closed below.
            }
        }
        val clean = cleanPublicKey(publicKey)
        return if (clean.isNotEmpty()) {
            decryptMessagePure(msg, "direct", clean)
        } else {
            msg.copy(content = "[Mensaje cifrado]")
        }
    }
}
