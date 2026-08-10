package com.example.data.repository

import android.util.Log
import com.example.data.model.PostCommentDto
import com.example.data.model.PostDto
import com.example.data.model.PostLikeDto
import com.example.data.supabase.SessionManager
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

interface FeedRepository {
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

    override suspend fun getFeed(limit: Int, lastCreatedAt: String?): Result<List<PostDto>> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val cursorParam = lastCreatedAt?.let { "lt.$it" }
            
            Log.d(TAG, "Fetching feed: limit=$limit, cursor=$cursorParam")
            
            // Step 1: Fetch posts without the profile join to be as robust as possible
            val response = runCall { b -> 
                service.getFeedPosts(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    limit = limit,
                    createdAtLt = cursorParam,
                    select = "*" // Explicitly fetch everything from posts only first
                ) 
            }

            if (response != null && response.isSuccessful) {
                var posts = response.body() ?: emptyList()
                Log.d(TAG, "Fetched ${posts.size} posts from Supabase. IDs: ${posts.map { it.id }}")
                
                if (posts.isEmpty()) {
                    Log.w(TAG, "Supabase returned an empty list of posts. This usually means no records match the criteria or RLS is blocking them.")
                }
                
                if (posts.isNotEmpty()) {
                    // Step 2: Try to map profiles manually to avoid cross-schema join issues
                    try {
                        val userIds = posts.mapNotNull { it.userId }.distinct()
                        if (userIds.isNotEmpty()) {
                            val idFilter = "in.(${userIds.joinToString(",")})"
                            val profilesResponse = runCall { b -> 
                                service.getProfiles(
                                    apiKey = SupabaseClient.supabaseAnonKey,
                                    authorization = b,
                                    idFilter = idFilter
                                ) 
                            }
                            if (profilesResponse != null && profilesResponse.isSuccessful) {
                                val profilesMap = profilesResponse.body()?.associateBy { it.id } ?: emptyMap()
                                posts = posts.map { it.copy(profile = if (it.userId != null) profilesMap[it.userId] else null) }
                                Log.d(TAG, "Mapped profiles for ${profilesMap.size} users")
                            } else {
                                Log.w(TAG, "Manual profile mapping failed: ${profilesResponse?.code()}")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Resilient profile mapping failed", e)
                    }

                    // Step 3: Get likes for the current user to populate isLikedByMe (Resiliently)
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
                            } else {
                                Log.w(TAG, "User likes fetch failed (resiliently): ${likesResponse?.code()}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error fetching user likes (resiliently)", e)
                        }
                    }
                }
                
                Result.success(posts)
            } else {
                val errorBody = response?.errorBody()?.string()
                Log.e(TAG, "Failed to fetch feed: ${response?.code()} - $errorBody")
                Result.failure(Exception("Failed to fetch feed: ${response?.code()} - $errorBody"))
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
                try {
                    com.example.notification.engine.producers.social.PostNotificationAdapter.publishPostCreated(
                        postId = createdPost.id ?: "",
                        authorId = createdPost.userId ?: "",
                        authorName = createdPost.userId ?: "Usuario",
                        caption = createdPost.content
                    )
                } catch (e: Exception) {
                    android.util.Log.e("FeedRepositoryImpl", "Error publishing post created event", e)
                }
                Result.success(createdPost)
            } else {
                val errorBody = response?.errorBody()?.string()
                if (errorBody?.contains("23505") == true) {
                    // Duplicate key means it was already created, treat as success
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
                        android.util.Log.e("FeedRepositoryImpl", "Error publishing post like event", e)
                    }
                }
                Result.success(Unit)
            } else {
                val code = response?.code()
                val errorBody = response?.errorBody()?.string()
                Log.e(TAG, "Failed to toggle like: code=$code, error=$errorBody, isLiked=$isLiked")
                Result.failure(Exception("Failed to toggle like: code=$code, error=$errorBody"))
            }
        } catch (e: Exception) {
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
                    android.util.Log.e("FeedRepositoryImpl", "Error publishing comment event", e)
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
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed to fetch comments: ${response?.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
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
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val updates = mapOf("content" to content)
            val response = runCall { b -> service.updatePost(SupabaseClient.supabaseAnonKey, b, "eq.$postId", updates) }
            if (response != null && response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                val errorBody = response?.errorBody()?.string()
                Result.failure(Exception("Failed to update post: ${response?.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPostById(postId: String): Result<PostDto> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b -> service.getPostById(SupabaseClient.supabaseAnonKey, b, "eq.$postId") }
            if (response != null && response.isSuccessful && !response.body().isNullOrEmpty()) {
                var post = response.body()!!.first()
                
                // Fetch profile manually
                try {
                    val userId = post.userId
                    if (userId != null) {
                        val profilesResponse = runCall { b -> 
                            service.getProfiles(SupabaseClient.supabaseAnonKey, b, "eq.$userId")
                        }
                        if (profilesResponse != null && profilesResponse.isSuccessful) {
                            val profile = profilesResponse.body()?.firstOrNull()
                            post = post.copy(profile = profile)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching profile for single post", e)
                }

                // Fetch isLikedByMe
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
                
                Result.success(post)
            } else {
                val errorBody = response?.errorBody()?.string()
                Result.failure(Exception("Failed to fetch post: ${response?.code()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
