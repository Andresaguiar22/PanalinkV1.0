package com.example.data.repository

import android.util.Log
import com.example.data.database.PublicProfileDao
import com.example.data.mapper.PublicProfileMapper
import com.example.data.model.PublicProfile
import com.example.data.supabase.SupabaseApiService
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val inFlightRequests = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.Deferred<PublicProfileFetchResult<Map<String, PublicProfile>>>>()

    companion object {
        private const val TAG = "PublicProfileRepo"

        @Volatile
        private var instance: PublicProfileRepository? = null

        fun getInstance(context: android.content.Context = com.example.PanaApplication.instance): PublicProfileRepository {
            return instance ?: synchronized(this) {
                instance ?: PublicProfileRepository(
                    publicProfileDao = com.example.data.database.PanalinkDatabase.getDatabase(context.applicationContext).publicProfileDao()
                ).also { instance = it }
            }
        }
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

        // 4. Single-flight deduplication check
        coroutineScope {
            var newDeferred: Deferred<PublicProfileFetchResult<Map<String, PublicProfile>>>? = null
            var idsToFetch: List<String> = emptyList()
            val existingDeferredsMap = mutableMapOf<String, Deferred<PublicProfileFetchResult<Map<String, PublicProfile>>>>()

            synchronized(inFlightRequests) {
                val missing = mutableListOf<String>()
                for (id in missingIds) {
                    val inFlight = inFlightRequests[id]
                    if (inFlight != null) {
                        existingDeferredsMap[id] = inFlight
                    } else {
                        missing.add(id)
                    }
                }

                if (missing.isNotEmpty()) {
                    idsToFetch = missing
                    val deferred = async(Dispatchers.IO) {
                        fetchRemoteProfilesBatch(missing)
                    }
                    newDeferred = deferred
                    missing.forEach { id -> inFlightRequests[id] = deferred }
                }
            }

            try {
                // Await newly created deferred if any
                val createdDef = newDeferred
                if (createdDef != null) {
                    val res = createdDef.await()
                    if (res is PublicProfileFetchResult.Success<*>) {
                        @Suppress("UNCHECKED_CAST")
                        resultMap.putAll(res.data as Map<String, PublicProfile>)
                    }
                }
                // Await existing in-flight deferreds
                for ((_, def) in existingDeferredsMap) {
                    val res = def.await()
                    if (res is PublicProfileFetchResult.Success<*>) {
                        @Suppress("UNCHECKED_CAST")
                        resultMap.putAll(res.data as Map<String, PublicProfile>)
                    }
                }
            } finally {
                val createdDef = newDeferred
                if (createdDef != null && idsToFetch.isNotEmpty()) {
                    synchronized(inFlightRequests) {
                        idsToFetch.forEach { id ->
                            if (inFlightRequests[id] === createdDef) {
                                inFlightRequests.remove(id)
                            }
                        }
                    }
                }
            }
        }

        if (resultMap.isNotEmpty()) {
            PublicProfileFetchResult.Success(resultMap)
        } else {
            PublicProfileFetchResult.NotFound
        }
    }

    private suspend fun fetchRemoteProfilesBatch(
        missingIds: List<String>
    ): PublicProfileFetchResult<Map<String, PublicProfile>> = withContext(Dispatchers.IO) {
        val resultMap = mutableMapOf<String, PublicProfile>()
        val sessionToken = SupabaseClient.currentToken
        if (sessionToken.isNullOrBlank()) {
            Log.e(TAG, "No valid session JWT available for PublicProfile request")
            return@withContext PublicProfileFetchResult.AuthError("No valid session token")
        }

        val service = apiServiceSupplier()
            ?: return@withContext PublicProfileFetchResult.NetworkError(message = "Supabase service uninitialized")

        val apiKey = SupabaseClient.supabaseAnonKey
        val bearerToken = "Bearer $sessionToken"
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

                PublicProfileFetchResult.Success(resultMap)
            } else {
                when (statusCode) {
                    401, 403 -> PublicProfileFetchResult.AuthError("Authorization failed ($statusCode)", statusCode)
                    404 -> PublicProfileFetchResult.NotFound
                    else -> PublicProfileFetchResult.NetworkError(code = statusCode, message = response.errorBody()?.string())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception fetching public profiles", e)
            PublicProfileFetchResult.NetworkError(exception = e, message = e.localizedMessage)
        }
    }

    /**
     * Searches public profiles by [query] matching display_name, first_name, or last_name.
     * Caches fetched entities in Room (PublicProfileDao) and returns a deduplicated list of PublicProfiles.
     */
    suspend fun searchPublicProfiles(
        query: String,
        limit: Int = 20
    ): PublicProfileFetchResult<List<PublicProfile>> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext PublicProfileFetchResult.Success(emptyList())
        }

        val sessionToken = SupabaseClient.currentToken
        if (sessionToken.isNullOrBlank()) {
            Log.e(TAG, "No valid session JWT available for searchPublicProfiles")
            return@withContext PublicProfileFetchResult.AuthError("No valid session token")
        }

        val service = apiServiceSupplier()
            ?: return@withContext PublicProfileFetchResult.NetworkError(message = "Supabase service uninitialized")

        val apiKey = SupabaseClient.supabaseAnonKey
        val bearerToken = "Bearer $sessionToken"
        val orFilter = "(display_name.ilike.*$cleanQuery*,first_name.ilike.*$cleanQuery*,last_name.ilike.*$cleanQuery*)"

        try {
            val response = service.searchPublicProfiles(
                apiKey = apiKey,
                authorization = bearerToken,
                orFilter = orFilter,
                limit = limit
            )
            val statusCode = response.code()

            if (response.isSuccessful) {
                val dtos = response.body() ?: emptyList()
                val entities = dtos.map { PublicProfileMapper.dtoToEntity(it) }

                if (entities.isNotEmpty()) {
                    publicProfileDao.upsertAll(entities)
                }

                val models = entities.map { PublicProfileMapper.entityToModel(it) }.distinctBy { it.id }
                PublicProfileFetchResult.Success(models)
            } else {
                when (statusCode) {
                    401, 403 -> PublicProfileFetchResult.AuthError("Authorization failed ($statusCode)", statusCode)
                    404 -> PublicProfileFetchResult.NotFound
                    else -> PublicProfileFetchResult.NetworkError(code = statusCode, message = response.errorBody()?.string())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception searching public profiles", e)
            PublicProfileFetchResult.NetworkError(exception = e, message = e.localizedMessage)
        }
    }
}
