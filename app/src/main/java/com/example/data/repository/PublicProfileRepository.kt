package com.example.data.repository

import android.util.Log
import com.example.data.database.PublicProfileDao
import com.example.data.mapper.PublicProfileMapper
import com.example.data.model.PublicProfile
import com.example.data.supabase.SupabaseApiService
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result state wrapper for public profile fetch operations.
 * Allows differentiating between NotFound, NetworkError, AuthError, and Success
 * without fabricating generic fallback names when network or authorization fails.
 */
sealed class PublicProfileFetchResult<out T> {
    data class Success<T>(val data: T) : PublicProfileFetchResult<T>()
    object NotFound : PublicProfileFetchResult<Nothing>()
    data class NetworkError(val exception: Throwable? = null, val code: Int? = null, val message: String? = null) : PublicProfileFetchResult<Nothing>()
    data class AuthError(val message: String? = null, val code: Int? = null) : PublicProfileFetchResult<Nothing>()
}

/**
 * Repository for Public Profiles.
 * Single source of truth for public user identity.
 * Features: deduplication, cache-first local Room storage, batch network calls (anti N+1).
 */
class PublicProfileRepository(
    private val publicProfileDao: PublicProfileDao,
    private val apiServiceSupplier: () -> SupabaseApiService? = { SupabaseClient.apiService }
) {
    companion object {
        private const val TAG = "PublicProfileRepo"
    }

    /**
     * Retrieves a single PublicProfile by [userId].
     */
    suspend fun getPublicProfile(
        userId: String,
        forceRefresh: Boolean = false
    ): PublicProfileFetchResult<PublicProfile> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) {
            return@withContext PublicProfileFetchResult.NotFound
        }

        val batchResult = getPublicProfiles(listOf(userId), forceRefresh = forceRefresh)
        when (batchResult) {
            is PublicProfileFetchResult.Success -> {
                val profile = batchResult.data[userId]
                if (profile != null) {
                    PublicProfileFetchResult.Success(profile)
                } else {
                    PublicProfileFetchResult.NotFound
                }
            }
            is PublicProfileFetchResult.NotFound -> PublicProfileFetchResult.NotFound
            is PublicProfileFetchResult.AuthError -> PublicProfileFetchResult.AuthError(batchResult.message, batchResult.code)
            is PublicProfileFetchResult.NetworkError -> PublicProfileFetchResult.NetworkError(batchResult.exception, batchResult.code, batchResult.message)
        }
    }

    /**
     * Retrieves multiple PublicProfiles by a list of [userIds].
     * Deduplicates input IDs and batches missing network requests to avoid N+1 issues.
     */
    suspend fun getPublicProfiles(
        userIds: List<String>,
        forceRefresh: Boolean = false
    ): PublicProfileFetchResult<Map<String, PublicProfile>> = withContext(Dispatchers.IO) {
        // 1. Deduplicate & filter blank IDs
        val cleanIds = userIds.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanIds.isEmpty()) {
            return@withContext PublicProfileFetchResult.Success(emptyMap())
        }

        val resultMap = mutableMapOf<String, PublicProfile>()

        // 2. Cache-First check in Room
        if (!forceRefresh) {
            val cachedEntities = publicProfileDao.getByIds(cleanIds)
            for (entity in cachedEntities) {
                resultMap[entity.id] = PublicProfileMapper.entityToModel(entity)
            }
        }

        // 3. Identify missing IDs
        val missingIds = cleanIds.filter { !resultMap.containsKey(it) }
        if (missingIds.isEmpty()) {
            return@withContext PublicProfileFetchResult.Success(resultMap)
        }

        // 4. Batch fetch from remote API
        val service = apiServiceSupplier()
        if (service == null) {
            return@withContext if (resultMap.isNotEmpty()) {
                PublicProfileFetchResult.Success(resultMap)
            } else {
                PublicProfileFetchResult.NetworkError(message = "Supabase service uninitialized")
            }
        }

        val apiKey = SupabaseClient.supabaseAnonKey
        val bearerToken = "Bearer ${SupabaseClient.currentToken ?: apiKey}"
        val idFilter = "in.(${missingIds.joinToString(",")})"

        try {
            val response = service.getPublicProfiles(apiKey = apiKey, authorization = bearerToken, idFilter = idFilter)
            val statusCode = response.code()

            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val fetchedEntities = dtos.map { PublicProfileMapper.dtoToEntity(it) }

                if (fetchedEntities.isNotEmpty()) {
                    publicProfileDao.upsertAll(fetchedEntities)
                    for (entity in fetchedEntities) {
                        resultMap[entity.id] = PublicProfileMapper.entityToModel(entity)
                    }
                }

                return@withContext PublicProfileFetchResult.Success(resultMap)
            } else {
                when (statusCode) {
                    401, 403 -> {
                        Log.e(TAG, "Authorization error $statusCode while fetching public profiles: ${response.errorBody()?.string()}")
                        if (resultMap.isNotEmpty()) {
                            PublicProfileFetchResult.Success(resultMap)
                        } else {
                            PublicProfileFetchResult.AuthError(message = "Authorization failed ($statusCode)", code = statusCode)
                        }
                    }
                    404 -> {
                        Log.w(TAG, "Public profiles endpoint or view not found ($statusCode)")
                        if (resultMap.isNotEmpty()) {
                            PublicProfileFetchResult.Success(resultMap)
                        } else {
                            PublicProfileFetchResult.NotFound
                        }
                    }
                    else -> {
                        val err = response.errorBody()?.string()
                        Log.e(TAG, "Server error $statusCode: $err")
                        if (resultMap.isNotEmpty()) {
                            PublicProfileFetchResult.Success(resultMap)
                        } else {
                            PublicProfileFetchResult.NetworkError(code = statusCode, message = err)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching public profiles", e)
            if (resultMap.isNotEmpty()) {
                PublicProfileFetchResult.Success(resultMap)
            } else {
                PublicProfileFetchResult.NetworkError(exception = e, message = e.localizedMessage)
            }
        }
    }
}
