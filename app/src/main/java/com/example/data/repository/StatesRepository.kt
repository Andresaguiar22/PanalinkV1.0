package com.example.data.repository

import android.util.Log
import com.example.data.model.*
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StatesRepository {
    private val TAG = "StatesRepository"
    
    private val db by lazy { com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance) }
    private val statesDao by lazy { db.statesDao() }

    private var bearer = ""

    fun getLocalStatesFlow(isReel: Boolean): Flow<List<com.example.data.model.UserStateWithUser>> {
        return statesDao.getStatesFlow(isReel).map { entities ->
            entities.map { it.toUserStateWithUser() }
        }
    }

    suspend fun saveStateLocally(item: com.example.data.model.UserStateWithUser, localPath: String? = null) {
        withContext(Dispatchers.IO) {
            statesDao.insertState(com.example.data.database.StateEntity.fromUserStateWithUser(item, localPath))
        }
    }

    // Helper to execute calls and retry once if 401/expired token occurs
    private suspend fun <R> runCall(call: suspend (String) -> retrofit2.Response<R>): retrofit2.Response<R>? {
        return com.example.util.Resilience.retry(
            times = 3,
            initialDelay = 500L,
            retryCondition = { it is java.io.IOException || (it is retrofit2.HttpException && it.code() in 500..599) }
        ) {
            val token = SupabaseClient.currentToken ?: ""
            bearer = "Bearer $token"
            
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

    // Fetch unexpired states, sorted by creation date (filtered by contacts)
    suspend fun getActiveStates(): Result<List<UserStateWithUser>> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            
            // 1. Pre-flight session check
            SessionManager.validateAndRefreshSessionIfNeeded()
            
            val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
            val apiKey = SupabaseClient.supabaseAnonKey

            // Load contacts to filter states
            val contactsResponse = runCall { b -> service.getContacts(apiKey, b, ownerFilter = "eq.$currentUid") }
            val contactIds = if (contactsResponse != null && contactsResponse.isSuccessful) {
                contactsResponse.body()?.map { it.contactUserId }?.toSet() ?: emptySet()
            } else {
                // Return cached list on failure or offline
                val cached = SessionManager.getCacheList("cached_states", UserStateWithUser::class.java)
                if (cached.isNotEmpty()) {
                    Log.i(TAG, "Failed loading contacts, returning cached states")
                    SessionManager.setOffline(true)
                    return@withContext Result.success(cached)
                }
                emptySet()
            }
            val allowedUserIds = contactIds + currentUid

            val nowStr = SupabaseClient.getNowIsoString()
            
            // Fetch reels from user_reels (Public)
            val reelsResponse = runCall { b -> service.getUserReels(apiKey, b) }
            
            // Fetch stories from user_stories (Private via RLS)
            val storiesResponse = runCall { b -> service.getUserStories(apiKey, b, expiresAtFilter = "gt.$nowStr") }

            val activeStates = mutableListOf<UserState>()
            
            if (reelsResponse != null && reelsResponse.isSuccessful) {
                val reels = reelsResponse.body()?.map { it.copy(type = "reel") } ?: emptyList()
                activeStates.addAll(reels)
            }
            
            if (storiesResponse != null && storiesResponse.isSuccessful) {
                val stories = storiesResponse.body()?.map { it.copy(type = "story") } ?: emptyList()
                activeStates.addAll(stories)
            }

            if (activeStates.isNotEmpty() || (reelsResponse?.isSuccessful == true && storiesResponse?.isSuccessful == true)) {
                // RLS now handles the contact filtering, so we don't need to filter here.
                val filteredStates = activeStates.filter { it.type != "story" || allowedUserIds.contains(it.userId) }


                // Fetch likes and favorites for current user (from both reels and stories)
                val likedStateIds = try {
                    val reelLikesRes = runCall { b -> 
                        service.getUserLikes(
                            table = "reel_likes",
                            apiKey = apiKey,
                            authorization = b,
                            filters = mapOf("user_id" to "eq.$currentUid")
                        )
                    }
                    val storyLikesRes = runCall { b -> 
                        service.getUserLikes(
                            table = "story_likes",
                            apiKey = apiKey,
                            authorization = b,
                            filters = mapOf("user_id" to "eq.$currentUid")
                        )
                    } ?: runCall { b ->
                        service.getUserLikes(
                            table = "story_likes",
                            apiKey = apiKey,
                            authorization = b,
                            filters = mapOf("author_id" to "eq.$currentUid")
                        )
                    } ?: runCall { b ->
                        service.getUserLikes(
                            table = "story_likes",
                            apiKey = apiKey,
                            authorization = b,
                            filters = emptyMap()
                        )
                    }
                    
                    val reelLikes = if (reelLikesRes != null && reelLikesRes.isSuccessful) {
                        reelLikesRes.body()?.filter { it.userId.isEmpty() || it.userId == currentUid }?.mapNotNull { it.stateId.takeIf { s -> s.isNotBlank() } }?.toSet() ?: emptySet()
                    } else emptySet()
                    
                    val storyLikes = if (storyLikesRes != null && storyLikesRes.isSuccessful) {
                        storyLikesRes.body()?.filter { it.userId.isEmpty() || it.userId == currentUid }?.mapNotNull { it.stateId.takeIf { s -> s.isNotBlank() } }?.toSet() ?: emptySet()
                    } else emptySet()
                    
                    reelLikes + storyLikes
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching user likes", e)
                    emptySet()
                }

                val favoritedStateIds = try {
                    val reelFavsRes = runCall { b -> 
                        service.getUserFavorites(
                            table = "reel_favorites",
                            apiKey = apiKey,
                            authorization = b,
                            filters = mapOf("user_id" to "eq.$currentUid")
                        )
                    }
                    val storyFavsRes = runCall { b -> 
                        service.getUserFavorites(
                            table = "story_favorites",
                            apiKey = apiKey,
                            authorization = b,
                            filters = mapOf("user_id" to "eq.$currentUid")
                        )
                    } ?: runCall { b ->
                        service.getUserFavorites(
                            table = "story_favorites",
                            apiKey = apiKey,
                            authorization = b,
                            filters = mapOf("author_id" to "eq.$currentUid")
                        )
                    } ?: runCall { b ->
                        service.getUserFavorites(
                            table = "story_favorites",
                            apiKey = apiKey,
                            authorization = b,
                            filters = emptyMap()
                        )
                    }
                    
                    val reelFavs = if (reelFavsRes != null && reelFavsRes.isSuccessful) {
                        reelFavsRes.body()?.filter { it.userId.isEmpty() || it.userId == currentUid }?.mapNotNull { it.stateId.takeIf { s -> s.isNotBlank() } }?.toSet() ?: emptySet()
                    } else emptySet()
                    
                    val storyFavs = if (storyFavsRes != null && storyFavsRes.isSuccessful) {
                        storyFavsRes.body()?.filter { it.userId.isEmpty() || it.userId == currentUid }?.mapNotNull { it.stateId.takeIf { s -> s.isNotBlank() } }?.toSet() ?: emptySet()
                    } else emptySet()
                    
                    reelFavs + storyFavs
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching user favorites", e)
                    emptySet()
                }

                // Query viewed states (story_views and reel_views)
                val viewedStateIds = try {
                    val reelViewsRes = runCall { b -> 
                        service.getUserLikes(
                            table = "reel_views",
                            apiKey = apiKey,
                            authorization = b,
                            filters = mapOf("viewer_id" to "eq.$currentUid")
                        )
                    }
                    val storyViewsRes = runCall { b -> 
                        service.getUserLikes(
                            table = "story_views",
                            apiKey = apiKey,
                            authorization = b,
                            filters = mapOf("viewer_id" to "eq.$currentUid")
                        )
                    } ?: runCall { b ->
                        service.getUserLikes(
                            table = "story_views",
                            apiKey = apiKey,
                            authorization = b,
                            filters = mapOf("user_id" to "eq.$currentUid")
                        )
                    } ?: runCall { b ->
                        service.getUserLikes(
                            table = "story_views",
                            apiKey = apiKey,
                            authorization = b,
                            filters = emptyMap()
                        )
                    }
                    
                    val reelViews = if (reelViewsRes != null && reelViewsRes.isSuccessful) {
                        reelViewsRes.body()?.mapNotNull { it.stateId.takeIf { s -> s.isNotBlank() } }?.toSet() ?: emptySet()
                    } else emptySet()
                    
                    val storyViews = if (storyViewsRes != null && storyViewsRes.isSuccessful) {
                        storyViewsRes.body()?.mapNotNull { it.stateId.takeIf { s -> s.isNotBlank() } }?.toSet() ?: emptySet()
                    } else emptySet()
                    
                    reelViews + storyViews
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching user views", e)
                    emptySet()
                }

                val list = filteredStates.map { state ->
                    val profile = if (state.userId == currentUid && SupabaseClient.currentProfile != null) {
                        SupabaseClient.currentProfile!!
                    } else {
                        Profile(state.userId, "Pana", null) ?: Profile(state.userId, "Pana", null)
                    }
                    val likedByMe = likedStateIds.contains(state.id)
                    val favoritedByMe = favoritedStateIds.contains(state.id)
                    val viewedByMe = viewedStateIds.contains(state.id)
                    val updatedState = state.copy(
                        likedByMe = likedByMe, 
                        favoritedByMe = favoritedByMe,
                        viewedByMe = viewedByMe
                    )
                    UserStateWithUser(updatedState, profile)
                }.sortedByDescending { it.state.createdAt }

                val resolvedList = list.map { item ->
                    val resolvedUrl = CdnManager.resolveMediaUrl(item.state.mediaUrl)
                    val resolvedUrls = item.state.mediaUrls?.map { CdnManager.resolveMediaUrl(it) } ?: emptyList()
                    val resolvedAudioUrl = item.state.audioUrl?.let { CdnManager.resolveMediaUrl(it) }
                    item.copy(
                        state = item.state.copy(
                            mediaUrl = if (resolvedUrl.isNotEmpty()) resolvedUrl else null,
                            mediaUrls = resolvedUrls,
                            audioUrl = resolvedAudioUrl
                        )
                    )
                }

                // Save to local database for SSOT using Smart Merge to prevent local state regression
                try {
                    val finalEntities = resolvedList.map { item ->
                        val newEntity = com.example.data.database.StateEntity.fromUserStateWithUser(item)
                        val existingEntity = statesDao.getStateById(item.state.id)
                        if (existingEntity != null) {
                            // Smart Merge: Preserve user interactions if backend view has temporary sync delay
                            val mergedLiked = if (existingEntity.likedByMe) true else newEntity.likedByMe
                            val mergedLikesCount = if (existingEntity.likedByMe && !newEntity.likedByMe) {
                                maxOf(existingEntity.likesCount, newEntity.likesCount, 1)
                            } else {
                                maxOf(existingEntity.likesCount, newEntity.likesCount)
                            }
                            val mergedFavorited = if (existingEntity.favoritedByMe) true else newEntity.favoritedByMe
                            val mergedFavoritesCount = if (existingEntity.favoritedByMe && !newEntity.favoritedByMe) {
                                maxOf(existingEntity.favoritesCount, newEntity.favoritesCount, 1)
                            } else {
                                maxOf(existingEntity.favoritesCount, newEntity.favoritesCount)
                            }
                            val mergedCommentsCount = maxOf(existingEntity.commentsCount, newEntity.commentsCount)

                            newEntity.copy(
                                likedByMe = mergedLiked,
                                likesCount = mergedLikesCount,
                                favoritedByMe = mergedFavorited,
                                favoritesCount = mergedFavoritesCount,
                                commentsCount = mergedCommentsCount
                            )
                        } else {
                            newEntity
                        }
                    }
                    statesDao.insertStates(finalEntities)
                    // Also delete expired locally
                    val now = SupabaseClient.getNowIsoString()
                    statesDao.deleteExpired(now)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to save states to local DB", e)
                }

                SessionManager.saveCacheList("cached_states", resolvedList, UserStateWithUser::class.java)
                SessionManager.setOffline(false)
                Result.success(resolvedList)
            } else {
                val cached = SessionManager.getCacheList("cached_states", UserStateWithUser::class.java)
                if (cached.isNotEmpty()) {
                    Log.i(TAG, "Table query failed, returning cached states")
                    SessionManager.setOffline(true)
                    return@withContext Result.success(cached)
                }
                val errorMsg = if (reelsResponse?.isSuccessful == false) reelsResponse.errorBody()?.string()
                              else storiesResponse?.errorBody()?.string()
                Result.failure(Exception("Error al cargar estados: $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getActiveStates exception", e)
            val cached = SessionManager.getCacheList("cached_states", UserStateWithUser::class.java)
            if (cached.isNotEmpty()) {
                Log.i(TAG, "Exception caught. Returning cached states instead of error.")
                SessionManager.setOffline(true)
                return@withContext Result.success(cached)
            }
            Result.failure(e)
        }
    }

    suspend fun getStatesByHashtag(hashtag: String): Result<List<UserStateWithUser>> = withContext(Dispatchers.IO) {
        getActiveStates().map { states ->
            states.filter { item ->
                item.state.caption?.contains(hashtag, ignoreCase = true) == true
            }
        }
    }

    // Upload state (supports text-only, or with image/media)
    suspend fun createState(
        mediaType: String, // "text" | "image" | "video"
        caption: String?,
        mediaBytes: ByteArray? = null,
        mediaMimeType: String? = null,
        isReel: Boolean = false,
        presetMediaUrl: String? = null,
        audioUrl: String? = null,
        mediaFile: java.io.File? = null
    ): Result<UserState> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
        
        // Calculate expiration: 24h if story, NULL if reel
        val expiresAtStr: String? = if (isReel) {
            null
        } else {
            val cal = Calendar.getInstance()
            cal.add(Calendar.HOUR, 24)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(cal.time)
        }
        val stateType = if (isReel) "reel" else "story"
 
        val nowStr = SupabaseClient.getNowIsoString()
        val stateId = if (SupabaseClient.isConfigured) {
            UUID.randomUUID().toString()
        } else {
            "state_${UUID.randomUUID()}"
        }
 
        var mediaUrl: String? = presetMediaUrl
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"
 
            // 1. Upload to dynamic CDN tunnel using UploadRepository
            if (mediaUrl == null && mediaMimeType != null) {
                val uploadResult = if (mediaFile != null && mediaFile.exists()) {
                    UploadRepository().uploadVideo(mediaFile, mediaMimeType, caption ?: "", currentUid)
                } else if (mediaBytes != null) {
                    UploadRepository().uploadVideo(mediaBytes, mediaMimeType, caption ?: "", currentUid)
                } else {
                    null
                }

                if (uploadResult != null) {
                    if (uploadResult.isSuccess) {
                        mediaUrl = uploadResult.getOrThrow().url
                        Log.d(TAG, "Uploaded successfully to CDN: $mediaUrl")
                    } else {
                        val uploadError = uploadResult.exceptionOrNull()?.localizedMessage ?: "Unknown error"
                        Log.e(TAG, "CDN upload failed: $uploadError")
                        return@withContext Result.failure(Exception("Error al subir archivo al CDN dinámico: $uploadError"))
                    }
                }
            }

            // 2. Insert record in user_reels or user_stories table using a map with only valid DB columns
            val stateMap = mutableMapOf<String, Any?>(
                "id" to stateId,
                "author_id" to currentUid,
                "media_url" to mediaUrl,
                "audio_url" to audioUrl,
                "media_type" to mediaType,
                "caption" to caption,
                "created_at" to nowStr
            )
            
            if (!isReel) {
                stateMap["expires_at"] = expiresAtStr
            }

            // Insert into the appropriate table
            val createResponse = if (isReel) {
                service.createReel(apiKey, bearer, stateMap)
            } else {
                service.createStory(apiKey, bearer, stateMap)
            }

            if (createResponse.isSuccessful) {
                val newState = UserState(
                    id = stateId,
                    authorId = currentUid,
                    userIdField = currentUid,
                    mediaUrl = mediaUrl,
                    audioUrl = audioUrl,
                    mediaType = mediaType,
                    caption = caption,
                    visibility = "public",
                    expiresAt = expiresAtStr,
                    type = stateType,
                    createdAt = nowStr
                )
                Result.success(newState)
            } else {
                val errorBody = createResponse.errorBody()?.string()
                if (errorBody?.contains("23505") == true) {
                     // If it already exists, return success with what we have
                     val newState = UserState(
                        id = stateId,
                        authorId = currentUid,
                        userIdField = currentUid,
                        mediaUrl = mediaUrl,
                        mediaType = mediaType,
                        caption = caption,
                        visibility = "public",
                        expiresAt = expiresAtStr,
                        type = stateType,
                        createdAt = nowStr
                    )
                    Result.success(newState)
                } else {
                    Result.failure(Exception("Error creating database state: $errorBody"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "createState exception", e)
            Result.failure(e)
        }
    }
    suspend fun toggleLike(stateId: String, currentLikeState: Boolean, isReel: Boolean): Result<com.example.data.model.ToggleLikeResponseDto> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            if (isReel) {
                val params = mapOf("p_reel_id" to stateId)
                Log.d("AUDIT_LIKE", "Calling toggle_reel_like RPC: reelId=$stateId")
                val response = service.toggleReelLikeRpc(apiKey, bearer, params)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("AUDIT_LIKE", "toggle_reel_like RPC success: liked=${body.liked}, count=${body.likesCount}")
                        if (body.liked) {
                            try {
                                com.example.notification.engine.core.NotificationProducerAdapters.publishReelLikeOrComment(
                                    reelId = stateId,
                                    actorId = currentUid,
                                    actorName = currentUid,
                                    isComment = false
                                )
                            } catch (e: Exception) {
                                Log.e("StatesRepository", "Error emitting reel like event", e)
                            }
                        }
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Empty response body from toggle_reel_like RPC"))
                    }
                } else {
                    val errorStr = response.errorBody()?.string()
                    Log.e("AUDIT_LIKE", "toggle_reel_like RPC error: $errorStr")
                    Result.failure(Exception(SupabaseClient.parseSupabaseError(errorStr, "toggle_reel_like RPC failed")))
                }
            } else {
                var response = service.toggleStoryLikeRpc(apiKey, bearer, mapOf("p_story_id" to stateId))
                if (!response.isSuccessful) {
                    val alt1 = service.toggleStoryLikeRpc(apiKey, bearer, mapOf("p_state_id" to stateId))
                    if (alt1.isSuccessful) response = alt1
                    else {
                        val alt2 = service.toggleStoryLikeRpc(apiKey, bearer, mapOf("p_status_id" to stateId))
                        if (alt2.isSuccessful) response = alt2
                    }
                }
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("AUDIT_LIKE", "toggle_story_like RPC success: liked=${body.liked}, count=${body.likesCount}")
                        if (body.liked) {
                            try {
                                com.example.notification.engine.core.NotificationProducerAdapters.publishStoryViewOrReaction(
                                    storyId = stateId,
                                    actorId = currentUid,
                                    actorName = currentUid,
                                    isReaction = true,
                                    reactionEmoji = "❤️"
                                )
                            } catch (e: Exception) {
                                Log.e("StatesRepository", "Error emitting story reaction event", e)
                            }
                        }
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Empty response body from toggle_story_like RPC"))
                    }
                } else {
                    val errorStr = response.errorBody()?.string()
                    Log.e("AUDIT_LIKE", "toggle_story_like RPC error: $errorStr")
                    Result.failure(Exception(SupabaseClient.parseSupabaseError(errorStr, "toggle_story_like RPC failed")))
                }
            }
        } catch (e: Exception) {
            Log.e("AUDIT_LIKE", "Exception in toggleLike", e)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(stateId: String, currentFavState: Boolean, isReel: Boolean): Result<com.example.data.model.ToggleFavoriteResponseDto> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            if (isReel) {
                var response = service.toggleReelFavoriteRpc(apiKey, bearer, mapOf("p_reel_id" to stateId))
                if (!response.isSuccessful) {
                    val alt = service.toggleReelFavoriteRpc(apiKey, bearer, mapOf("p_state_id" to stateId))
                    if (alt.isSuccessful) response = alt
                }
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("AUDIT_FAVORITE", "toggle_reel_favorite RPC success: favorited=${body.favorited}, count=${body.favoritesCount}")
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Empty response body from toggle_reel_favorite RPC"))
                    }
                } else {
                    val errorStr = response.errorBody()?.string()
                    Log.e("AUDIT_FAVORITE", "toggle_reel_favorite RPC error: $errorStr")
                    Result.failure(Exception(SupabaseClient.parseSupabaseError(errorStr, "toggle_reel_favorite RPC failed")))
                }
            } else {
                var response = service.toggleStoryFavoriteRpc(apiKey, bearer, mapOf("p_story_id" to stateId))
                if (!response.isSuccessful) {
                    val alt1 = service.toggleStoryFavoriteRpc(apiKey, bearer, mapOf("p_state_id" to stateId))
                    if (alt1.isSuccessful) response = alt1
                    else {
                        val alt2 = service.toggleStoryFavoriteRpc(apiKey, bearer, mapOf("p_status_id" to stateId))
                        if (alt2.isSuccessful) response = alt2
                    }
                }
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        Log.d("AUDIT_FAVORITE", "toggle_story_favorite RPC success: favorited=${body.favorited}, count=${body.favoritesCount}")
                        Result.success(body)
                    } else {
                        Result.failure(Exception("Empty response body from toggle_story_favorite RPC"))
                    }
                } else {
                    val errorStr = response.errorBody()?.string()
                    Log.e("AUDIT_FAVORITE", "toggle_story_favorite RPC error: $errorStr")
                    Result.failure(Exception(SupabaseClient.parseSupabaseError(errorStr, "toggle_story_favorite RPC failed")))
                }
            }
        } catch (e: Exception) {
            Log.e("AUDIT_FAVORITE", "Exception in toggleFavorite", e)
            Result.failure(e)
        }
    }

    suspend fun getSavedStates(): Result<List<UserStateWithUser>> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val reelFavsRes = runCall { b ->
                service.getUserFavorites(
                    table = "reel_favorites",
                    apiKey = apiKey,
                    authorization = b,
                    filters = mapOf("user_id" to "eq.$currentUid")
                )
            }
            val storyFavsRes = runCall { b ->
                service.getUserFavorites(
                    table = "story_favorites",
                    apiKey = apiKey,
                    authorization = b,
                    filters = mapOf("user_id" to "eq.$currentUid")
                )
            }

            val reelFavIds = reelFavsRes?.body()?.mapNotNull { it.stateId.takeIf { s -> s.isNotBlank() } }?.toSet() ?: emptySet()
            val storyFavIds = storyFavsRes?.body()?.mapNotNull { it.stateId.takeIf { s -> s.isNotBlank() } }?.toSet() ?: emptySet()
            val allFavIds = reelFavIds + storyFavIds

            val allStatesRes = getActiveStates()
            if (allStatesRes.isSuccess) {
                val allStates = allStatesRes.getOrNull() ?: emptyList()
                val saved = allStates.filter { it.state.id in allFavIds || it.state.favoritedByMe == true }
                Result.success(saved)
            } else {
                allStatesRes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getSavedStates", e)
            Result.failure(e)
        }
    }

    suspend fun incrementShare(stateId: String, isReel: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val tableName = if (isReel) "reel_shares" else "story_shares"
            val idColumn = if (isReel) "reel_id" else "story_id"
            val bodyMap = mapOf(
                idColumn to stateId,
                "user_id" to currentUid,
                "created_at" to SupabaseClient.getNowIsoString()
            )
            Log.d("AUDIT_SHARE", "Proceeding to SHARE. POST /rest/v1/$tableName with body: $bodyMap")
            val response = service.shareState(tableName, apiKey, bearer, bodyMap)
            Log.d("AUDIT_SHARE", "SHARE Response: HTTP ${response.code()} ${response.message()}")
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorStr = response.errorBody()?.string()
                Log.e("AUDIT_SHARE", "SHARE Error Body: $errorStr")
                Result.failure(Exception(SupabaseClient.parseSupabaseError(errorStr, "Error incrementing share")))
            }
        } catch (e: Exception) {
            Log.e("AUDIT_SHARE", "Exception in incrementShare", e)
            Result.failure(e)
        }
    }

    suspend fun addComment(stateId: String, commentText: String, isReel: Boolean, parentId: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        if (commentText.isBlank()) return@withContext Result.failure(Exception("Comment cannot be empty"))
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
        
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val tableName = if (isReel) "reel_comments" else "story_comments"
            val idColumns = if (isReel) listOf("reel_id") else listOf("story_id", "status_id", "state_id")
            val userColumns = listOf("author_id", "user_id")
            val textColumns = listOf("body", "text", "content", "comment_text")

            var lastError = "Error adding comment"
            for (idCol in idColumns) {
                for (userCol in userColumns) {
                    for (textCol in textColumns) {
                        val bodyMap = mutableMapOf<String, String>(
                            idCol to stateId,
                            userCol to currentUid,
                            textCol to commentText,
                            "created_at" to SupabaseClient.getNowIsoString()
                        )
                        if (parentId != null) {
                            bodyMap["parent_comment_id"] = parentId
                        }
                        Log.d("AUDIT_COMMENT", "Attempting COMMENT POST /rest/v1/$tableName with keys: $idCol, $userCol, $textCol")
                        val response = service.commentState(tableName, apiKey, bearer, bodyMap)
                        if (response.isSuccessful) {
                            Log.d("AUDIT_COMMENT", "addComment succeeded with keys: $idCol, $userCol, $textCol")
                            try {
                                if (isReel) {
                                    com.example.notification.engine.core.NotificationProducerAdapters.publishReelLikeOrComment(
                                        reelId = stateId,
                                        actorId = currentUid,
                                        actorName = currentUid,
                                        isComment = true,
                                        commentText = commentText
                                    )
                                } else {
                                    com.example.notification.engine.producers.social.CommentNotificationAdapter.publishPostComment(
                                        postId = stateId,
                                        commentId = java.util.UUID.randomUUID().toString(),
                                        postAuthorId = "",
                                        actorId = currentUid,
                                        actorName = currentUid,
                                        commentText = commentText,
                                        isReply = parentId != null
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e("StatesRepository", "Error publishing comment notification", e)
                            }
                            return@withContext Result.success(Unit)
                        } else {
                            val err = response.errorBody()?.string() ?: ""
                            lastError = err
                            if (response.code() == 401 || response.code() == 403) break
                        }
                    }
                }
            }
            Result.failure(Exception(SupabaseClient.parseSupabaseError(lastError, "Error adding comment")))
        } catch (e: Exception) {
            Log.e("AUDIT_COMMENT", "Exception in addComment", e)
            Result.failure(e)
        }
    }

    suspend fun getStateComments(stateId: String, isReel: Boolean): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val tableName = if (isReel) "reel_comments" else "story_comments"
            val idColumns = if (isReel) listOf("reel_id") else listOf("story_id", "status_id", "state_id")

            var response: retrofit2.Response<List<com.example.data.model.StateCommentDto>>? = null
            for (idCol in idColumns) {
                response = service.getStateComments(
                    table = tableName,
                    apiKey = apiKey,
                    authorization = bearer,
                    filters = mapOf(idCol to "eq.$stateId")
                )
                if (response.isSuccessful) break
            }

            if (response != null && response.isSuccessful) {
                val commentsDto = response.body() ?: emptyList()
                val comments = commentsDto.map { it.toDomain() }
                Result.success(comments)
            } else {
                val errorStr = response?.errorBody()?.string()
                Result.failure(Exception(SupabaseClient.parseSupabaseError(errorStr, "Error fetching comments")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(commentId: String, isReel: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val tableName = if (isReel) "reel_comments" else "story_comments"
            val updates = mapOf("deleted_at" to SupabaseClient.getNowIsoString())
            val response = service.patchComment(tableName, apiKey, bearer, "eq.$commentId", updates)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorStr = response.errorBody()?.string()
                Result.failure(Exception(SupabaseClient.parseSupabaseError(errorStr, "Error deleting comment")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStatusViews(stateId: String, isReel: Boolean): Result<List<StatusViewer>> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val tableName = if (isReel) "reel_views" else "story_views"
            val idColumn = if (isReel) "reel_id" else "story_id"
            val response = service.getStatusViews(
                table = tableName,
                apiKey = apiKey,
                authorization = bearer,
                filters = mapOf(idColumn to "eq.$stateId")
            )
            if (response.isSuccessful) {
                val viewsDto = response.body() ?: emptyList()
                val viewerIds = viewsDto.map { it.viewerId }.filter { it.isNotBlank() }.distinct()
                val publicResult = PublicProfileRepository.getInstance().getPublicProfiles(viewerIds)
                val publicProfilesMap = if (publicResult is PublicProfileFetchResult.Success) {
                    publicResult.data
                } else emptyMap()

                val views = viewsDto.map { dto ->
                    val pub = publicProfilesMap[dto.viewerId]
                    StatusViewer(
                        viewerId = dto.viewerId,
                        name = pub?.displayName ?: pub?.firstName ?: dto.profiles?.displayName ?: "Usuario",
                        avatarUrl = CdnManager.resolveAvatarUrl(pub?.avatarUrl ?: dto.profiles?.avatarUrl),
                        viewedAt = dto.createdAt
                    )
                }
                Result.success(views)
            } else {
                val errorStr = response.errorBody()?.string()
                Result.failure(Exception(SupabaseClient.parseSupabaseError(errorStr, "Error fetching views")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUserStatus(stateId: String, isReel: Boolean, mediaUrl: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Physically delete from CDN if mediaUrl is provided
            mediaUrl?.let { url ->
                val fileId = url.substringAfterLast("/")
                if (fileId.isNotEmpty() && fileId.contains("media-")) {
                    try {
                        val cdnUrl = CdnManager.getCDNUrl()
                        val deleteEndpoint = if (cdnUrl.endsWith("/")) "${cdnUrl}delete/$fileId" else "$cdnUrl/delete/$fileId"
                        Log.i(TAG, "Procediendo a BORRADO FÍSICO CDN de archivo: $fileId en endpoint: $deleteEndpoint")
                        val client = OkHttpClient()
                        val request = Request.Builder()
                            .url(deleteEndpoint)
                            .delete()
                            .build()
                        client.newCall(request).execute().use { deleteRes ->
                            Log.i(TAG, "Resultado de borrado físico en CDN: HTTP ${deleteRes.code}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error intentando borrar físicamente el archivo del CDN: ${e.message}", e)
                    }
                }
            }

            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val response = if (isReel) {
                service.deleteReel(apiKey, bearer, "eq.$stateId")
            } else {
                service.deleteStory(apiKey, bearer, "eq.$stateId")
            }
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorStr = response.errorBody()?.string()
                Result.failure(Exception(SupabaseClient.parseSupabaseError(errorStr, "Error deleting state")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerView(stateId: String, isReel: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val token = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("Session expired"))
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val tableName = if (isReel) "reel_views" else "story_views"
            val idColumn = if (isReel) "reel_id" else "story_id"
            val bodyMap = mapOf(
                idColumn to stateId,
                "viewer_id" to currentUid
            )
            Log.d("AUDIT_VIEW", "Proceeding to VIEW. POST /rest/v1/$tableName with body: $bodyMap")
            val response = service.viewStatus(tableName, apiKey, bearer, bodyMap)
            
            if (response.isSuccessful) {
                Log.d("AUDIT_VIEW", "VIEW Response: HTTP ${response.code()} ${response.message()}")
                Result.success(Unit)
            } else {
                val errorStr = response.errorBody()?.string()
                if (errorStr?.contains("23505") == true || errorStr?.contains("duplicate") == true) {
                    Log.d("AUDIT_VIEW", "VIEW Response: Already viewed (duplicate key 23505). HTTP ${response.code()}")
                    Result.success(Unit)
                } else {
                    Log.e("AUDIT_VIEW", "VIEW Error Body: $errorStr")
                    Result.failure(Exception(SupabaseClient.parseSupabaseError(errorStr, "Error registering view")))
                }
            }
        } catch (e: Exception) {
            Log.e("AUDIT_VIEW", "Exception in registerView", e)
            Result.failure(e)
        }
    }

    suspend fun getProfileForUser(userId: String): Profile = withContext(Dispatchers.IO) {
        if (userId == SupabaseClient.currentUser?.id && SupabaseClient.currentProfile != null) {
            return@withContext SupabaseClient.currentProfile!!
        }
        try {
            val service = SupabaseClient.apiService ?: return@withContext Profile(userId, "Pana", null)
            val token = SupabaseClient.currentToken ?: return@withContext Profile(userId, "Pana", null)
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"
            
            val response = service.getProfile(apiKey, bearer, "eq.$userId")
            if (response.isSuccessful) {
                response.body()?.firstOrNull() ?: Profile(userId, "Pana", null)
            } else {
                Profile(userId, "Pana", null)
            }
        } catch (e: Exception) {
            Profile(userId, "Pana", null)
        }
    }

    // Fetch reels for a specific user
    suspend fun getUserReels(userId: String): Result<List<UserStateWithUser>> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            SessionManager.validateAndRefreshSessionIfNeeded()
            val apiKey = SupabaseClient.supabaseAnonKey

            // Filter by author_id
            val response = runCall { b -> service.getUserReels(apiKey, b, authorFilter = "eq.$userId") }

            if (response != null && response.isSuccessful) {
                val allStates = response.body()?.map { it.copy(type = "reel") } ?: emptyList()
                val reels = allStates.filter { it.isReel }

                // Map to UserStateWithUser and resolve URLs
                val profile = Profile(userId, "Pana", null)
                val resolvedList = reels.map { state ->
                    val resolvedUrl = CdnManager.resolveMediaUrl(state.mediaUrl)
                    val resolvedState = state.copy(mediaUrl = if (resolvedUrl.isNotEmpty()) resolvedUrl else null)
                    UserStateWithUser(resolvedState, profile)
                }.sortedByDescending { it.state.createdAt }
                Result.success(resolvedList)
                Result.success(resolvedList)
            } else {
                Result.failure(Exception("Error loading reels: ${response?.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
