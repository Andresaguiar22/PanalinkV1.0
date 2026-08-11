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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class SocialRepositoryImpl : SocialRepository {
    private val TAG = "SocialRepositoryImpl"
    private val repoScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)
    
    override suspend fun toggleLike(stateId: String, isReel: Boolean): Unit = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext
        
        try {
            val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            val statesDao = database.statesDao()
            val pendingDao = database.pendingSocialActionDao()
            
            val existing = statesDao.getStateById(stateId)
            val actualCurrentLike = existing?.likedByMe ?: false
            val actualCount = existing?.likesCount ?: 0
            
            val newLiked = !actualCurrentLike
            val newCount = if (actualCurrentLike) (actualCount - 1).coerceAtLeast(0) else actualCount + 1
            
            if (existing != null) {
                statesDao.insertState(existing.copy(likedByMe = newLiked, likesCount = newCount))
            }
            
            pendingDao.deleteLikeActionsForTarget(currentUid, stateId)
            val actionType = if (actualCurrentLike) "UNLIKE" else "LIKE"
            val action = com.example.data.database.PendingSocialActionEntity(
                localActionId = java.util.UUID.randomUUID().toString(),
                userId = currentUid,
                targetId = stateId,
                actionType = actionType,
                payload = null,
                isReel = isReel
            )
            pendingDao.insertAction(action)
            
            com.example.worker.SocialSyncWorker.enqueue(com.example.PanaApplication.instance)
            refreshSignal.emit(stateId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in toggleLike", e)
        }
    }

    override suspend fun getLikes(stateId: String, isReel: Boolean): Flow<Int> {
        val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        return database.statesDao().getStateFlowById(stateId)
            .map { it?.likesCount ?: 0 }
            .flowOn(Dispatchers.IO)
    }

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

    override suspend fun getComments(stateId: String, isReel: Boolean): Flow<List<Comment>> {
        val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        val commentDao = database.commentDao()
        
        // Trigger background sync
        repoScope.launch {
            syncComments(stateId, isReel)
        }
        
        return commentDao.getCommentsFlow(stateId, isReel).map { entities -> 
            entities.map { it.toStateComment() }
        }.flowOn(Dispatchers.IO)
    }

    private suspend fun syncComments(stateId: String, isReel: Boolean) {
        try {
            val service = SupabaseClient.apiService ?: return
            val token = SupabaseClient.currentToken ?: return
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
                
                // Fetch public profiles via PublicProfileRepository
                val userIds = commentsDto.map { it.toDomain().userId }.filter { it.isNotBlank() }.distinct()
                val publicResult = PublicProfileRepository.getInstance().getPublicProfiles(userIds)
                val publicProfilesMap = if (publicResult is PublicProfileFetchResult.Success) {
                    publicResult.data
                } else emptyMap()

                val resolvedComments = commentsDto.map { dto -> 
                    val domainComment = dto.toDomain()
                    val profile = publicProfilesMap[domainComment.userId]
                    if (profile != null) {
                        domainComment.copy(
                            authorName = PublicProfileResolver.resolveDisplayName(profile, domainComment.authorName, domainComment.userId),
                            avatarUrl = CdnManager.resolveAvatarUrl(profile.avatarUrl) ?: domainComment.avatarUrl
                        )
                    } else {
                        domainComment
                    }
                }

                val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
                val commentDao = database.commentDao()
                
                if (resolvedComments.isNotEmpty()) {
                    val entities = resolvedComments.map { com.example.data.database.CommentEntity.fromStateComment(it, isReel) }
                    commentDao.upsertAll(entities)
                    
                    // Delete stale local comments
                    val remoteIds = entities.map { it.id }
                    commentDao.deleteStaleComments(stateId, isReel, remoteIds)
                } else {
                    commentDao.deleteStaleComments(stateId, isReel, emptyList())
                }
            } else {
                Log.e("AUDIT_REEL_COMMENT", "Fetch comments failed: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching comments", e)
        }
    }

    override suspend fun addComment(stateId: String, text: String, parentId: String?, isReel: Boolean): Unit = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext
        if (text.isBlank()) return@withContext
        
        try {
            val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            val commentDao = database.commentDao()
            val pendingDao = database.pendingSocialActionDao()
            
            val localCommentId = java.util.UUID.randomUUID().toString()
            val nowStr = SupabaseClient.getNowIsoString()
            
            val profile = SupabaseClient.currentProfile
            
            val comment = com.example.data.database.CommentEntity(
                id = localCommentId,
                targetId = stateId,
                authorId = currentUid,
                authorName = profile?.displayName ?: "",
                authorAvatarUrl = profile?.avatarUrl,
                content = text,
                createdAt = nowStr,
                parentCommentId = parentId,
                isReel = isReel,
                syncStatus = "pending_add"
            )
            commentDao.upsert(comment)
            
            val payloadJson = org.json.JSONObject().apply {
                put("text", text)
                put("parentId", parentId ?: org.json.JSONObject.NULL)
                put("localCommentId", localCommentId)
            }.toString()
            
            val action = com.example.data.database.PendingSocialActionEntity(
                localActionId = java.util.UUID.randomUUID().toString(),
                userId = currentUid,
                targetId = stateId,
                actionType = "COMMENT",
                payload = payloadJson,
                isReel = isReel
            )
            pendingDao.insertAction(action)
            
            com.example.worker.SocialSyncWorker.enqueue(com.example.PanaApplication.instance)
            refreshSignal.emit(stateId)
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
