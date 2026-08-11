package com.example.data.repository

import android.util.Log
import com.example.data.database.PostEntity
import com.example.data.model.PostCommentDto
import com.example.data.model.PostDto
import com.example.data.model.PostLikeDto
import com.example.data.model.Profile
import com.example.data.supabase.SessionManager
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.Response

interface FeedRepository {
    fun getLocalPostsFlow(limit: Int = 20): Flow<List<PostDto>>
    suspend fun getFeed(limit: Int = 20, lastCreatedAt: String? = null): Result<Unit>
    suspend fun createPost(post: PostDto): Result<PostDto>
    suspend fun toggleLike(postId: String, userId: String, isLiked: Boolean): Result<Unit>
    suspend fun addComment(postId: String, userId: String, content: String): Result<PostCommentDto>
    suspend fun getCommentsForPost(postId: String): Result<Unit>
    fun getCommentsFlow(postId: String): Flow<List<PostCommentDto>>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun updatePost(postId: String, content: String): Result<PostDto>
    suspend fun getPostById(postId: String): Result<PostDto>
}

class FeedRepositoryImpl : FeedRepository {

    private val TAG = "FeedRepository"
    private val repoScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    private suspend fun <R> runCall(call: suspend (String) -> Response<R>): Response<R>? {
        return com.example.util.Resilience.retry(
            times = 5,
            initialDelay = 1000L,
            maxDelay = 10000L,
            factor = 2.0,
            retryCondition = { it is java.io.IOException || (it is retrofit2.HttpException && it.code() in 500..599) || (it is retrofit2.HttpException && it.code() == 408) }
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

    override fun getLocalPostsFlow(limit: Int): Flow<List<PostDto>> {
        val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        val postDao = database.postDao()
        val publicProfileDao = database.publicProfileDao()
        val publicProfileRepo = PublicProfileRepository.getInstance()
        return postDao.getPostsFlow(limit).map { entities ->
            val userIds = entities.map { it.authorId }.distinct().filter { it.isNotBlank() }
            
            val cachedEntities = if (userIds.isNotEmpty()) publicProfileDao.getByIds(userIds) else emptyList()
            val profilesMap = cachedEntities.associate { it.id to com.example.data.mapper.PublicProfileMapper.entityToModel(it) }
            
            val missingIds = userIds.filter { !profilesMap.containsKey(it) }
            if (missingIds.isNotEmpty()) {
                repoScope.launch {
                    publicProfileRepo.getPublicProfiles(missingIds)
                }
            }
            
            entities.map { entity ->
                val dto = entity.toPostDto()
                val pub = profilesMap[entity.authorId]
                if (pub != null) {
                    dto.copy(profile = PublicProfileResolver.toProfile(pub))
                } else {
                    dto
                }
            }
        }
    }

    override suspend fun getFeed(limit: Int, lastCreatedAt: String?): Result<Unit> = withContext(Dispatchers.IO) {
        val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        val postDao = database.postDao()

        if (!SupabaseClient.isConfigured) {
            return@withContext Result.success(Unit)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val cursorParam = lastCreatedAt?.let { "lt.$it" }
            
            Log.d(TAG, "Fetching feed: limit=$limit, cursor=$cursorParam")
            
            val response = runCall { b -> 
                service.getFeedPosts(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    limit = limit,
                    createdAtLt = cursorParam,
                    select = "*"
                ) 
            }

            if (response != null && response.isSuccessful) {
                var posts = response.body() ?: emptyList()
                Log.d(TAG, "Fetched ${posts.size} posts from Supabase. IDs: ${posts.map { it.id }}")
                
                if (posts.isNotEmpty()) {
                    val currentUser = SupabaseClient.currentUser
                    if (currentUser != null) {
                        try {
                            val likesResponse = runCall { b -> 
                                service.getUserLikes(
                                    apiKey = SupabaseClient.supabaseAnonKey, 
                                    authorization = b, 
                                    userIdFilter = "eq.${currentUser.id}"
                                ) 
                            }
                            if (likesResponse != null && likesResponse.isSuccessful) {
                                val userLikes = likesResponse.body()?.mapNotNull { it.postId }?.toSet() ?: emptySet()
                                posts = posts.map { it.copy(isLikedByMe = userLikes.contains(it.id)) }
                                Log.d(TAG, "Fetched and mapped ${userLikes.size} user likes")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching user likes (resiliently)", e)
                        }
                    }

                    // Save to Room
                    val entities = posts.map { PostEntity.fromPostDto(it) }
                    postDao.upsertAll(entities)

                    // Trigger resolution of profiles in the background to warm the cache
                    try {
                        val userIds = posts.mapNotNull { it.userId }.filter { it.isNotBlank() }.distinct()
                        if (userIds.isNotEmpty()) {
                            PublicProfileRepository.getInstance().getPublicProfiles(userIds)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Background profiles warming failed", e)
                    }
                }
                
                Result.success(Unit)
            } else {
                val errorBody = response?.errorBody()?.string()
                Log.e(TAG, "Failed to fetch feed: ${response?.code()} - $errorBody")
                Result.failure(Exception("Failed to fetch feed: ${response?.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getFeed", e)
            Result.failure(e)
        }
    }

    override suspend fun createPost(post: PostDto): Result<PostDto> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b -> service.createPost(SupabaseClient.supabaseAnonKey, b, post) }
            
            if (response != null && response.isSuccessful && !response.body().isNullOrEmpty()) {
                val createdPost = response.body()!!.first()
                
                // Save to Room immediately
                val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
                database.postDao().upsert(PostEntity.fromPostDto(createdPost))

                try {
                    com.example.notification.engine.producers.social.PostNotificationAdapter.publishPostCreated(
                        postId = createdPost.id ?: "",
                        authorId = createdPost.userId ?: "",
                        authorName = SupabaseClient.currentProfile?.displayName ?: "",
                        caption = createdPost.content
                    )
                } catch (e: Exception) {
                    Log.e("FeedRepositoryImpl", "Error publishing post created event", e)
                }
                Result.success(createdPost)
            } else {
                val errorBody = response?.errorBody()?.string()
                if (errorBody?.contains("23505") == true) {
                    val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
                    database.postDao().upsert(PostEntity.fromPostDto(post))
                    Result.success(post)
                } else {
                    Result.failure(Exception("Failed to create post: ${response?.code()} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCommentsFlow(postId: String): Flow<List<PostCommentDto>> {
        val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        return database.commentDao().getCommentsFlow(postId, isReel = false).map { entities ->
            entities.map { it.toPostCommentDto() }
        }
    }

    override suspend fun toggleLike(postId: String, userId: String, isLiked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        val postDao = database.postDao()
        val pendingDao = database.pendingSocialActionDao()
        val existing = postDao.getPostById(postId)
        
        // 1. Update Room immediately
        val actualCurrentLike = existing?.currentUserLiked ?: isLiked
        val actualCount = existing?.likesCount ?: 0
        val newLiked = !actualCurrentLike
        val newCount = if (actualCurrentLike) (actualCount - 1).coerceAtLeast(0) else actualCount + 1
        
        if (existing != null) {
            postDao.upsert(existing.copy(currentUserLiked = newLiked, likesCount = newCount))
        }

        // 2. Coalesce/queue the action locally
        pendingDao.deleteLikeActionsForTarget(userId, postId)
        val actionType = if (actualCurrentLike) "UNLIKE" else "LIKE"
        val action = com.example.data.database.PendingSocialActionEntity(
            localActionId = java.util.UUID.randomUUID().toString(),
            userId = userId,
            targetId = postId,
            actionType = actionType,
            payload = null,
            isReel = false
        )
        pendingDao.insertAction(action)

        // 3. Enqueue Background Sync
        com.example.worker.SocialSyncWorker.enqueue(com.example.PanaApplication.instance)

        Result.success(Unit)
    }

    override suspend fun addComment(postId: String, userId: String, content: String): Result<PostCommentDto> = withContext(Dispatchers.IO) {
        val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        val postDao = database.postDao()
        val commentDao = database.commentDao()
        val pendingDao = database.pendingSocialActionDao()

        val tempCommentId = java.util.UUID.randomUUID().toString()
        val timestamp = com.example.data.supabase.SupabaseClient.getNowIsoString()

        // 1. Increment comments count locally
        val existing = postDao.getPostById(postId)
        if (existing != null) {
            postDao.upsert(existing.copy(commentsCount = existing.commentsCount + 1))
        }

        // Get my profile info if available
        val myProfileEntity = database.profileDao().getProfile(userId)
        val myProfile = myProfileEntity?.toProfile()

        // 2. Insert temporary comment locally
        val tempCommentEntity = com.example.data.database.CommentEntity(
            id = tempCommentId,
            targetId = postId,
            authorId = userId,
            authorName = myProfile?.displayName ?: "",
            authorAvatarUrl = myProfile?.avatarUrl,
            content = content,
            createdAt = timestamp,
            isReel = false,
            syncStatus = "pending_add"
        )
        commentDao.upsert(tempCommentEntity)

        // 3. Insert pending action
        val payloadJson = org.json.JSONObject().apply {
            put("text", content)
            put("parentId", org.json.JSONObject.NULL)
            put("localCommentId", tempCommentId)
        }.toString()

        val action = com.example.data.database.PendingSocialActionEntity(
            localActionId = tempCommentId, // associate with temporary comment ID
            userId = userId,
            targetId = postId,
            actionType = "COMMENT",
            payload = payloadJson,
            isReel = false
        )
        pendingDao.insertAction(action)

        // 4. Enqueue Background Sync
        com.example.worker.SocialSyncWorker.enqueue(com.example.PanaApplication.instance)

        Result.success(tempCommentEntity.toPostCommentDto())
    }

    override suspend fun getCommentsForPost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        val commentDao = database.commentDao()

        // 2. Refresh from Supabase if configured
        if (SupabaseClient.isConfigured) {
            try {
                val service = SupabaseClient.apiService
                if (service != null) {
                    val response = runCall { b -> service.getCommentsForPost(SupabaseClient.supabaseAnonKey, b, "eq.$postId") }
                    
                    if (response != null && response.isSuccessful) {
                        var comments = response.body() ?: emptyList()
                        if (comments.isNotEmpty()) {
                            try {
                                val userIds = comments.mapNotNull { it.userId }.filter { it.isNotBlank() }.distinct()
                                if (userIds.isNotEmpty()) {
                                    val publicResult = PublicProfileRepository.getInstance().getPublicProfiles(userIds)
                                    if (publicResult is PublicProfileFetchResult.Success) {
                                        val publicProfilesMap = publicResult.data.mapNotNull { (id, pubResult) ->
                                            if (pubResult is PublicProfileFetchResult.Success) id to pubResult.data else null
                                        }.toMap()
                                        comments = comments.map { comment ->
                                            val pub = if (comment.userId != null) publicProfilesMap[comment.userId] else null
                                            if (pub != null) {
                                                comment.copy(profile = PublicProfileResolver.toProfile(pub))
                                            } else {
                                                comment
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Resilient comment profile mapping failed", e)
                            }

                            // Save remote comments to Room
                            val entities = comments.map { com.example.data.database.CommentEntity.fromPostCommentDto(it) }
                            commentDao.upsertAll(entities)
                            
                            // Delete local comments that were deleted on server
                            val remoteIds = entities.map { it.id }
                            commentDao.deleteStaleComments(postId, false, remoteIds)
                        } else {
                            // No remote comments, delete all local comments that aren't pending
                            commentDao.deleteStaleComments(postId, false, emptyList())
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sync comments failed", e)
            }
        }

        Result.success(Unit)
    }

    override suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            database.postDao().deletePostById(postId)
            
            val userId = SupabaseClient.currentUser?.id ?: return@withContext Result.success(Unit)
            val action = com.example.data.database.PendingSocialActionEntity(
                localActionId = java.util.UUID.randomUUID().toString(),
                userId = userId,
                targetId = postId,
                actionType = "DELETE_POST",
                payload = null,
                isReel = false
            )
            database.pendingSocialActionDao().insertAction(action)
            com.example.worker.SocialSyncWorker.enqueue(com.example.PanaApplication.instance)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePost(postId: String, content: String): Result<PostDto> = withContext(Dispatchers.IO) {
        try {
            val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            val postDao = database.postDao()
            val existing = postDao.getPostById(postId)
            if (existing != null) {
                postDao.upsert(existing.copy(content = content, updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).format(java.util.Date())))
            }

            val userId = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
            val action = com.example.data.database.PendingSocialActionEntity(
                localActionId = java.util.UUID.randomUUID().toString(),
                userId = userId,
                targetId = postId,
                actionType = "UPDATE_POST",
                payload = content,
                isReel = false
            )
            database.pendingSocialActionDao().insertAction(action)
            com.example.worker.SocialSyncWorker.enqueue(com.example.PanaApplication.instance)
            
            val updatedDto = existing?.toPostDto()?.copy(content = content) ?: throw Exception("Post not found locally")
            Result.success(updatedDto)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPostById(postId: String): Result<PostDto> = withContext(Dispatchers.IO) {
        val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        val postDao = database.postDao()
        val local = postDao.getPostById(postId)

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b -> service.getPostById(SupabaseClient.supabaseAnonKey, b, "eq.$postId") }
            if (response != null && response.isSuccessful && !response.body().isNullOrEmpty()) {
                var post = response.body()!!.first()
                
                val currentUser = SupabaseClient.currentUser
                if (currentUser != null) {
                    try {
                        val likesResponse = runCall { b -> 
                            service.getUserLikes(SupabaseClient.supabaseAnonKey, b, "eq.${currentUser.id}")
                        }
                        if (likesResponse != null && likesResponse.isSuccessful) {
                            val userLikes = likesResponse.body()?.map { it.postId }?.toSet() ?: emptySet()
                            post = post.copy(isLikedByMe = userLikes.contains(post.id))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching user likes for single post", e)
                    }
                }
                
                postDao.upsert(PostEntity.fromPostDto(post))
                
                try {
                    val userId = post.userId
                    if (userId != null) {
                        val publicResult = PublicProfileRepository.getInstance().getPublicProfile(userId)
                        if (publicResult is PublicProfileFetchResult.Success) {
                            val pub = publicResult.data
                            post = post.copy(
                                profile = PublicProfileResolver.toProfile(pub)
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching profile for single post", e)
                }

                Result.success(post)
            } else {
                if (local != null) {
                    Result.success(local.toPostDto())
                } else {
                    val errorBody = response?.errorBody()?.string()
                    Result.failure(Exception("Failed to fetch post: ${response?.code()} - $errorBody"))
                }
            }
        } catch (e: Exception) {
            if (local != null) {
                Result.success(local.toPostDto())
            } else {
                Result.failure(e)
            }
        }
    }
}
