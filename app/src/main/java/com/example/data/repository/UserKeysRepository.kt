package com.example.data.repository

import android.util.Log
import com.example.data.model.UserKeyDto
import com.example.data.supabase.SessionManager
import com.example.data.supabase.SupabaseClient
import com.example.util.CryptoManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
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
            
            var response = try {
                call(bearer)
            } catch (e: Exception) {
                Log.e(TAG, "Network call failed", e)
                throw e
            }
            
            if (response != null && response.code() == 401) {
                Log.i(TAG, "401/JWT expired detected. Triggering refresh session...")
                val refreshed = SessionManager.refreshSession()
                if (refreshed) {
                    val newToken = SupabaseClient.currentToken ?: ""
                    bearer = "Bearer $newToken"
                    response = try {
                        call(bearer)
                    } catch (e: Exception) {
                        Log.e(TAG, "Retry call failed", e)
                        throw e
                    }
                }
            }
            response
        }
    }

    /**
     * Sincroniza la llave pública local con la tabla public.user_keys en Supabase.
     */
    suspend fun syncPublicKey(): Result<Boolean> = withContext(Dispatchers.IO) {
        val currentUserId = SupabaseClient.currentUser?.id
        if (currentUserId.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("Usuario no autenticado"))
        }

        val localPubKey = CryptoManager.getPublicKeyBase64()
        if (localPubKey.isEmpty()) {
            return@withContext Result.failure(Exception("No se pudo obtener la clave pública local"))
        }

        // Cache local copy
        CryptoManager.publicKeyCache[currentUserId] = localPubKey

        if (!SupabaseClient.isConfigured) {
            Log.d(TAG, "Supabase no está configurado. Llave pública sincronizada en caché local.")
            return@withContext Result.success(true)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase no configurado"))
            val body = mapOf(
                "user_id" to currentUserId,
                "public_key" to localPubKey
            )
            val response = runCall { b ->
                service.upsertUserKey(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    keyData = body
                )
            }
            if (response != null && response.isSuccessful) {
                Log.d(TAG, "Llave pública sincronizada con éxito en Supabase")
                Result.success(true)
            } else {
                val errorStr = response?.errorBody()?.string() ?: "Error desconocido"
                Log.e(TAG, "Error al sincronizar llave pública: $errorStr")
                Result.failure(Exception("Error en respuesta del servidor: $errorStr"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al sincronizar llave pública", e)
            Result.failure(e)
        }
    }

    /**
     * Obtiene la llave pública de otro usuario en Supabase (con caché en memoria).
     */
    suspend fun getPublicKeyForUser(userId: String): String? = withContext(Dispatchers.IO) {
        if (userId.isEmpty()) return@withContext null

        // Check cache first
        val cachedKey = CryptoManager.publicKeyCache[userId]
        if (!cachedKey.isNullOrEmpty()) {
            return@withContext cachedKey
        }

        if (!SupabaseClient.isConfigured) {
            // For testing/mock environments, return local cache or null
            return@withContext null
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext null
            val response = runCall { b ->
                service.getUserKeys(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    userIdFilter = "eq.$userId"
                )
            }
            if (response != null && response.isSuccessful) {
                val body = response.body()
                if (!body.isNullOrEmpty()) {
                    val pubKey = CryptoManager.cleanPublicKey(body[0].publicKey)
                    if (pubKey.isNotEmpty()) {
                        CryptoManager.publicKeyCache[userId] = pubKey
                        return@withContext pubKey
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción al obtener clave pública para usuario $userId", e)
        }

        null
    }
}
