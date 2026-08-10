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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.Response

interface FeedRepository {
    fun getLocalPostsFlow(limit: Int = 20): Flow<List<PostDto>>
    suspend fun getFeed(limit: Int = 20, lastCreatedAt: String? = null): Result<List<PostDto>>
    suspend fun createPost(post: PostDto): Result<PostDto>
    suspend fun toggleLike(postId: String, userId: String, isLiked: Boolean): Result<Unit>
    suspend fun addComment(postId: String, userId: String, content: String): Result<PostCommentDto>
    suspend fun getCommentsForPost(postId: String): Result<List<PostCommentDto>>
    suspend fun deletePost(postId: String): Result<Unit>
    suspend fun updatePost(postId: String, content: String): Result<PostDto>
    suspend fun getPostById(postId: String): Result<PostDto>
}

class FeedRepositoryImpl : FeedRepository {

    private val TAG = "FeedRepository"

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
        val publicProfileRepo = PublicProfileRepository.getInstance()
        return postDao.getPostsFlow(limit).map { entities ->
            val userIds = entities.map { it.authorId }.distinct().filter { it.isNotBlank() }
            val profilesMap = if (userIds.isNotEmpty()) {
                val publicResult = publicProfileRepo.getPublicProfiles(userIds)
                if (publicResult is PublicProfileFetchResult.Success) {
                    publicResult.data
                } else {
                    emptyMap()
                }
            } else {
                emptyMap()
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

    override suspend fun getFeed(limit: Int, lastCreatedAt: String?): Result<List<PostDto>> = withContext(Dispatchers.IO) {
        val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        val postDao = database.postDao()

        if (!SupabaseClient.isConfigured) {
            val cachedPosts = if (lastCreatedAt == null) {
                postDao.getPosts(limit)
            } else {
                postDao.getPostsPaged(limit, lastCreatedAt)
            }
            return@withContext Result.success(cachedPosts.map { it.toPostDto() })
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
                
                Result.success(posts)
            } else {
                val errorBody = response?.errorBody()?.string()
                Log.e(TAG, "Failed to fetch feed: ${response?.code()} - $errorBody")
                
                val cachedPosts = if (lastCreatedAt == null) {
                    postDao.getPosts(limit)
                } else {
                    postDao.getPostsPaged(limit, lastCreatedAt)
                }
                Result.success(cachedPosts.map { it.toPostDto() })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in getFeed", e)
            val cachedPosts = if (lastCreatedAt == null) {
                postDao.getPosts(limit)
            } else {
                postDao.getPostsPaged(limit, lastCreatedAt)
            }
            Result.success(cachedPosts.map { it.toPostDto() })
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

    override suspend fun toggleLike(postId: String, userId: String, isLiked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        val postDao = database.postDao()
        val existing = postDao.getPostById(postId)
        
        if (existing != null) {
            val newLiked = !isLiked
            val newCount = if (isLiked) (existing.likesCount - 1).coerceAtLeast(0) else existing.likesCount + 1
            postDao.upsert(existing.copy(currentUserLiked = newLiked, likesCount = newCount))
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            
            val response = if (isLiked) {
                runCall { b -> service.removeLike(SupabaseClient.supabaseAnonKey, b, "eq.$postId", "eq.$userId") }
            } else {
                val likeDto = PostLikeDto(postId = postId, userId = userId)
                runCall { b -> service.addLike(SupabaseClient.supabaseAnonKey, b, likeDto) }
            }

            val isSuccess = response != null && (
                response.isSuccessful || 
                (!isLiked && response.code() == 409) || 
                (isLiked && response.code() == 404)
            )

            if (isSuccess) {
                if (!isLiked) {
                    try {
                        com.example.notification.engine.producers.social.PostNotificationAdapter.publishPostLike(
                            postId = postId,
                            postAuthorId = "",
                            actorId = userId,
                            actorName = userId
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error publishing post like event", e)
                    }
                }
                Result.success(Unit)
            } else {
                if (existing != null) {
                    postDao.upsert(existing)
                }
                val code = response?.code()
                val errorBody = response?.errorBody()?.string()
                Log.e(TAG, "Failed to toggle like: code=$code, error=$errorBody, isLiked=$isLiked")
                Result.failure(Exception("Failed to toggle like: code=$code, error=$errorBody"))
            }
        } catch (e: Exception) {
            if (existing != null) {
                postDao.upsert(existing)
            }
            Log.e(TAG, "Exception in toggleLike", e)
            Result.failure(e)
        }
    }

    override suspend fun addComment(postId: String, userId: String, content: String): Result<PostCommentDto> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val commentDto = PostCommentDto(postId = postId, userId = userId, content = content)
            val response = runCall { b -> service.addComment(SupabaseClient.supabaseAnonKey, b, commentDto) }
            
            if (response != null && response.isSuccessful && !response.body().isNullOrEmpty()) {
                val createdComment = response.body()!!.first()
                
                // Increment local comment count in Room
                val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
                val postDao = database.postDao()
                val existing = postDao.getPostById(postId)
                if (existing != null) {
                    postDao.upsert(existing.copy(commentsCount = existing.commentsCount + 1))
                }

                try {
                    com.example.notification.engine.producers.social.CommentNotificationAdapter.publishPostComment(
                        postId = postId,
                        commentId = createdComment.id ?: java.util.UUID.randomUUID().toString(),
                        postAuthorId = "",
                        actorId = userId,
                        actorName = userId,
                        commentText = content
                    )
                } catch (e: Exception) {
                    Log.e("FeedRepositoryImpl", "Error publishing comment event", e)
                }
                Result.success(createdComment)
            } else {
                Result.failure(Exception("Failed to add comment: ${response?.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCommentsForPost(postId: String): Result<List<PostCommentDto>> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b -> service.getCommentsForPost(SupabaseClient.supabaseAnonKey, b, "eq.$postId") }
            
            if (response != null && response.isSuccessful) {
                var comments = response.body() ?: emptyList()
                if (comments.isNotEmpty()) {
                    try {
                        val userIds = comments.mapNotNull { it.userId }.filter { it.isNotBlank() }.distinct()
                        if (userIds.isNotEmpty()) {
                            val publicResult = PublicProfileRepository.getInstance().getPublicProfiles(userIds)
                            if (publicResult is PublicProfileFetchResult.Success) {
                                val publicProfilesMap = publicResult.data
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
                }
                Result.success(comments)
            } else {
                Result.failure(Exception("Failed to fetch comments: ${response?.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            database.postDao().deletePostById(postId)

            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b -> service.deletePost(SupabaseClient.supabaseAnonKey, b, "eq.$postId") }
            if (response != null && response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorBody = response?.errorBody()?.string()
                Result.failure(Exception("Failed to delete post: ${response?.code()} - $errorBody"))
            }
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

            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val updates = mapOf("content" to content)
            val response = runCall { b -> service.updatePost(SupabaseClient.supabaseAnonKey, b, "eq.$postId", updates) }
            if (response != null && response.isSuccessful && !response.body().isNullOrEmpty()) {
                val updatedDto = response.body()!!.first()
                postDao.upsert(PostEntity.fromPostDto(updatedDto))
                Result.success(updatedDto)
            } else {
                val errorBody = response?.errorBody()?.string()
                Result.failure(Exception("Failed to update post: ${response?.code()} - $errorBody"))
            }
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
