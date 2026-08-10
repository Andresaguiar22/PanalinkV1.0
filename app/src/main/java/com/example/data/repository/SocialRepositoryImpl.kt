package com.example.data.repository

import android.util.Log
import com.example.data.model.Comment
import com.example.data.model.Like
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

class SocialRepositoryImpl : SocialRepository {
    private val TAG = "SocialRepositoryImpl"
    
    override suspend fun toggleLike(stateId: String, isReel: Boolean): Unit = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext
        
        try {
            val service = SupabaseClient.apiService ?: return@withContext
            val token = SupabaseClient.currentToken ?: return@withContext
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            if (isReel) {
                val params = mapOf("p_reel_id" to stateId)
                Log.d("AUDIT_REEL_LIKE", "Calling toggle_reel_like RPC: reelId=$stateId")
                val response = service.toggleReelLikeRpc(
                    apiKey = apiKey,
                    authorization = bearer,
                    params = params
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("AUDIT_REEL_LIKE", "toggle_reel_like RPC Success: liked=${body?.liked}, likesCount=${body?.likesCount}")
                } else {
                    Log.e("AUDIT_REEL_LIKE", "toggle_reel_like RPC Error: ${response.errorBody()?.string()}")
                }
            } else {
                val params = mapOf("p_story_id" to stateId)
                Log.d("AUDIT_REEL_LIKE", "Calling toggle_story_like RPC: storyId=$stateId")
                val response = service.toggleStoryLikeRpc(
                    apiKey = apiKey,
                    authorization = bearer,
                    params = params
                )
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("AUDIT_REEL_LIKE", "toggle_story_like RPC Success: liked=${body?.liked}, likesCount=${body?.likesCount}")
                } else {
                    Log.e("AUDIT_REEL_LIKE", "toggle_story_like RPC Error: ${response.errorBody()?.string()}")
                }
            }
            refreshSignal.emit(stateId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in toggleLike", e)
        }
    }

    override suspend fun getLikes(stateId: String, isReel: Boolean): Flow<Int> = flow {
        emit(fetchLikesCount(stateId, isReel))
        refreshSignal.collect { refreshedStateId ->
            if (refreshedStateId == stateId) {
                emit(fetchLikesCount(stateId, isReel))
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchLikesCount(stateId: String, isReel: Boolean): Int {
        return try {
            val service = SupabaseClient.apiService ?: return 0
            val token = SupabaseClient.currentToken ?: return 0
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val tableName = if (isReel) "reel_likes" else "story_likes"
            val idColumn = if (isReel) "reel_id" else "story_id"
            
            val response = service.getStateLikes(
                table = tableName,
                apiKey = apiKey,
                authorization = bearer,
                filters = mapOf(idColumn to "eq.$stateId")
            )
            if (response.isSuccessful) {
                response.body()?.size ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching likes count", e)
            0
        }
    }

    override suspend fun getComments(stateId: String, isReel: Boolean): Flow<List<Comment>> = flow {
        emit(fetchComments(stateId, isReel))
        refreshSignal.collect { refreshedStateId ->
            if (refreshedStateId == stateId) {
                emit(fetchComments(stateId, isReel))
            }
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun fetchComments(stateId: String, isReel: Boolean): List<Comment> {
        return try {
            val service = SupabaseClient.apiService ?: return emptyList()
            val token = SupabaseClient.currentToken ?: return emptyList()
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val tableName = if (isReel) "reel_comments" else "story_comments"
            val idColumn = if (isReel) "reel_id" else "story_id"
            
            val response = service.getStateComments(
                table = tableName,
                apiKey = apiKey,
                authorization = bearer,
                filters = mapOf(idColumn to "eq.$stateId")
            )
            if (response.isSuccessful) {
                val commentsDto = response.body() ?: emptyList()
                
                // Fetch profiles manually to avoid cross-schema join errors
                val profilesResp = service.getProfiles(apiKey, bearer)
                val profilesMap = if (profilesResp.isSuccessful) {
                    profilesResp.body()?.associateBy { it.id } ?: emptyMap()
                } else emptyMap()

                commentsDto.map { dto -> 
                    val domainComment = dto.toDomain()
                    val profile = profilesMap[domainComment.userId]
                    if (profile != null) {
                        domainComment.copy(
                            authorName = profile.displayName,
                            avatarUrl = profile.avatarUrl
                        )
                    } else {
                        domainComment
                    }
                }
            } else {
                Log.e("AUDIT_REEL_COMMENT", "Fetch comments failed: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching comments", e)
            emptyList()
        }
    }

    override suspend fun addComment(stateId: String, text: String, parentId: String?, isReel: Boolean): Unit = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext
        if (text.isBlank()) return@withContext
        
        try {
            val service = SupabaseClient.apiService ?: return@withContext
            val token = SupabaseClient.currentToken ?: return@withContext
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val tableName = if (isReel) "reel_comments" else "story_comments"
            val idColumn = if (isReel) "reel_id" else "story_id"
            
            val body = mutableMapOf(
                idColumn to stateId,
                "author_id" to currentUid,
                "body" to text,
                "created_at" to SupabaseClient.getNowIsoString()
            )
            if (parentId != null) {
                body["parent_comment_id"] = parentId
            }

            Log.d("AUDIT_REEL_COMMENT", "Proceeding to COMMENT. POST /rest/v1/$tableName with body: $body")
            val response = service.commentState(
                table = tableName,
                apiKey = apiKey,
                authorization = bearer,
                body = body
            )
            Log.d("AUDIT_REEL_COMMENT", "COMMENT Response: HTTP ${response.code()} ${response.message()}")
            if (response.isSuccessful) {
                refreshSignal.emit(stateId)
            } else {
                Log.e("AUDIT_REEL_COMMENT", "Add comment failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding comment", e)
        }
    }

    override suspend fun getVideoUrl(stateId: String, isReel: Boolean): String = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext ""
            val token = SupabaseClient.currentToken ?: return@withContext ""
            val apiKey = SupabaseClient.supabaseAnonKey
            val bearer = "Bearer $token"

            val response = if (isReel) {
                service.getUserReels(apiKey, bearer)
            } else {
                service.getUserStories(apiKey, bearer)
            }
            if (response.isSuccessful) {
                response.body()?.find { it.id == stateId }?.mediaUrl ?: ""
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting video url", e)
            ""
        }
    }

    companion object {
        private val refreshSignal = MutableSharedFlow<String>(replay = 1)
    }
}
