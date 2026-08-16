package com.example.data.repository

import android.util.Log
import com.example.data.model.UserKeyDto
import com.example.data.supabase.SessionManager
import com.example.data.supabase.SupabaseClient
import com.example.util.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

object UserKeysRepository {
    private const val TAG = "UserKeysRepository"

    private suspend fun <R> runCall(call: suspend (String) -> Response<R>): Response<R>? {
        return com.example.util.Resilience.retry(
            times = 3,
            initialDelay = 500L,
            retryCondition = { it is java.io.IOException || (it is retrofit2.HttpException && it.code() in 500..599) }
        ) {
            SessionManager.validateAndRefreshSessionIfNeeded()
            var token = SupabaseClient.currentToken ?: return@retry null
            var bearer = "Bearer $token"
            var response = try { call(bearer) } catch (e: Exception) {
                Log.e(TAG, "Network call failed", e)
                throw e
            }
            if (response.code() == 401) {
                Log.i(TAG, "401/JWT expired detected. Triggering refresh session...")
                if (SessionManager.refreshSession()) {
                    token = SupabaseClient.currentToken ?: ""
                    bearer = "Bearer $token"
                    response = call(bearer)
                }
            }
            response
        }
    }

    suspend fun syncPublicKey(): Result<Boolean> = withContext(Dispatchers.IO) {
        val currentUserId = SupabaseClient.currentUser?.id
            ?: return@withContext Result.failure(Exception("Usuario no autenticado"))
        val localPubKey = CryptoManager.getPublicKeyBase64()
        if (localPubKey.isEmpty()) return@withContext Result.failure(Exception("No se pudo obtener la clave pública local"))

        CryptoManager.publicKeyCache[currentUserId] = localPubKey
        if (!SupabaseClient.isConfigured) return@withContext Result.success(true)

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase no configurado"))
            val response = runCall { b ->
                service.upsertUserKey(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    keyData = mapOf("user_id" to currentUserId, "public_key" to localPubKey)
                )
            }
            if (response?.isSuccessful == true) Result.success(true)
            else Result.failure(Exception("Error al sincronizar llave pública: ${response?.errorBody()?.string() ?: "sin respuesta"}"))
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al sincronizar llave pública", e)
            Result.failure(e)
        }
    }

    /** Returns the public key, or throws when E2EE is enabled and a direct-message key is missing. */
    suspend fun getPublicKeyForUser(userId: String): String? = withContext(Dispatchers.IO) {
        if (userId.isEmpty()) return@withContext null
        CryptoManager.publicKeyCache[userId]?.takeIf { it.isNotEmpty() }?.let { return@withContext it }
        if (!SupabaseClient.isConfigured) {
            if (CryptoManager.ENABLE_E2EE) throw MissingPublicKeyException(userId)
            return@withContext null
        }

        try {
            val service = SupabaseClient.apiService ?: run {
                if (CryptoManager.ENABLE_E2EE) throw MissingPublicKeyException(userId)
                return@withContext null
            }
            val response = runCall { b ->
                service.getUserKeys(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    userIdFilter = "eq.$userId"
                )
            }
            if (response?.isSuccessful == true) {
                val body = response.body()
                val pubKey = body?.firstOrNull()?.publicKey?.let(CryptoManager::cleanPublicKey)
                if (!pubKey.isNullOrEmpty()) {
                    CryptoManager.publicKeyCache[userId] = pubKey
                    return@withContext pubKey
                }
            }
        } catch (e: MissingPublicKeyException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al obtener clave pública para usuario $userId", e)
        }

        if (CryptoManager.ENABLE_E2EE) throw MissingPublicKeyException(userId)
        null
    }

    class MissingPublicKeyException(userId: String) :
        IllegalStateException("No public E2EE key is available for recipient $userId; plaintext fallback is disabled")
}
