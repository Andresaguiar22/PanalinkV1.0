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

    val publicKeyCache = ConcurrentHashMap<String, String>()
    val chatToOtherUserCache = ConcurrentHashMap<String, String>()
    private var localKeyPair: KeyPair? = null

    init { ensureKeyPairExists() }

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
        val cleaned = cleanPublicKey(value)
        return try { Base64.decode(cleaned, Base64.NO_WRAP) }
        catch (_: Throwable) { Base64.decode(cleaned, Base64.DEFAULT) }
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
            if (entry != null) localKeyPair = KeyPair(entry.certificate.publicKey, entry.privateKey)
        } catch (e: Throwable) {
            Log.e(TAG, "AndroidKeyStore unavailable; using in-memory fallback", e)
            ensureSoftwareKeyPair()
        }
    }

    private fun ensureSoftwareKeyPair() {
        if (localKeyPair != null) return
        try {
            val generator = KeyPairGenerator.getInstance("EC")
            generator.initialize(ECGenParameterSpec("secp256r1"))
            localKeyPair = generator.generateKeyPair()
        } catch (e: Throwable) { Log.e(TAG, "Software key generation failed", e) }
    }

    fun getPublicKeyBase64(): String {
        ensureKeyPairExists()
        val key = localKeyPair?.public?.encoded ?: return ""
        return Base64.encodeToString(key, Base64.NO_WRAP)
    }

    private fun getPrivateKey(): PrivateKey? {
        ensureKeyPairExists()
        return localKeyPair?.private
    }

    private fun getSharedSecret(otherPublicKeyBase64: String): ByteArray? {
        val privateKey = getPrivateKey() ?: return null
        val otherBytes = decodeBase64(otherPublicKeyBase64)
        if (otherBytes.isEmpty()) return null
        val otherPublicKey = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(otherBytes))
        return KeyAgreement.getInstance("ECDH").run {
            init(privateKey)
            doPhase(otherPublicKey, true)
            generateSecret()
        }
    }

    private fun deriveAESKey(sharedSecret: ByteArray): SecretKeySpec {
        return SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(sharedSecret), "AES")
    }

    /** Throws when encryption cannot be performed; plaintext fallback is intentionally forbidden. */
    fun encrypt(plainText: String, receiverPublicKeyBase64: String): String {
        if (!ENABLE_E2EE || plainText.isEmpty()) return plainText
        val cleaned = cleanPublicKey(receiverPublicKeyBase64)
        require(cleaned.isNotEmpty()) { "Missing receiver public key; refusing plaintext fallback" }
        val secret = getSharedSecret(cleaned) ?: error("Unable to derive E2EE shared secret")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        cipher.init(Cipher.ENCRYPT_MODE, deriveAESKey(secret), GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
    }

    fun decrypt(encryptedPayloadBase64: String, senderPublicKeyBase64: String): String {
        if (encryptedPayloadBase64.isEmpty() || senderPublicKeyBase64.isEmpty()) return encryptedPayloadBase64
        val cleaned = cleanPublicKey(senderPublicKeyBase64)
        if (cleaned.isEmpty()) return "[Mensaje cifrado]"
        return try {
            val combined = decodeBase64(encryptedPayloadBase64)
            if (combined.size <= 12) return encryptedPayloadBase64
            val secret = getSharedSecret(cleaned) ?: return "[Mensaje cifrado]"
            val iv = combined.copyOfRange(0, 12)
            val ciphertext = combined.copyOfRange(12, combined.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, deriveAESKey(secret), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (_: Throwable) {
            // Preserve compatibility with messages stored before E2EE was enabled.
            encryptedPayloadBase64
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
        } catch (_: Throwable) { false }
        if (isChannelOrCommunity) return msg

        val currentUid = try { com.example.data.supabase.SupabaseClient.currentUser?.id ?: "" } catch (_: Throwable) { "" }
        val otherUserId = if (msg.senderId != currentUid && msg.senderId.isNotEmpty()) msg.senderId else {
            chatToOtherUserCache[msg.chatId] ?: run {
                try {
                    val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
                    val id = db.chatDao().getChatById(msg.chatId)?.otherUserId
                    if (!id.isNullOrEmpty()) chatToOtherUserCache[msg.chatId] = id
                    id
                } catch (_: Throwable) { null }
            }
        }

        if (otherUserId.isNullOrEmpty()) return msg.copy(content = "[Mensaje cifrado]")
        var publicKey = publicKeyCache[otherUserId]
        repeat(3) { attempt ->
            if (!publicKey.isNullOrEmpty()) return@repeat
            try {
                if (attempt > 0) kotlinx.coroutines.delay(400)
                publicKey = com.example.data.repository.UserKeysRepository.getPublicKeyForUser(otherUserId)
            } catch (_: Throwable) { }
        }
        val clean = cleanPublicKey(publicKey)
        return if (clean.isNotEmpty()) decryptMessagePure(msg, "direct", clean) else msg.copy(content = "[Mensaje cifrado]")
    }
}
