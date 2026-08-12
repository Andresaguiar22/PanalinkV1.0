package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.database.PanalinkDatabase
import com.example.data.database.CommentEntity
import com.example.data.database.PendingSocialActionEntity
import com.example.data.model.PostCommentDto
import com.example.data.model.PostLikeDto
import com.example.data.supabase.SupabaseClient
import com.example.PanaApplication
import java.io.IOException
import java.util.concurrent.TimeUnit

class SocialSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = PanalinkDatabase.getDatabase(context)
    private val pendingDao = db.pendingSocialActionDao()
    private val commentDao = db.commentDao()
    private val postDao = db.postDao()
    private val statesDao = db.statesDao()

    override suspend fun doWork(): Result {
        Log.i("SocialSyncWorker", "Starting sync of pending social actions...")
        if (!SupabaseClient.isConfigured) {
            return Result.success()
        }

        val pendingActions = pendingDao.getPendingActions()
        if (pendingActions.isEmpty()) {
            Log.i("SocialSyncWorker", "No pending social actions found.")
            return Result.success()
        }

        var anyFailed = false

        for (action in pendingActions) {
            try {
                val service = SupabaseClient.apiService ?: throw Exception("ApiService not available")
                val token = SupabaseClient.currentToken ?: throw Exception("Auth token not available")
                val apiKey = SupabaseClient.supabaseAnonKey
                val bearer = "Bearer $token"

                var success = false

                when (action.actionType) {
                    "FAVORITE" -> {
                        if (action.isReel) {
                            val params = mapOf<String, Any>("p_reel_id" to action.targetId, "p_favorited" to true)
                            val response = service.setReelFavoriteRpc(apiKey, bearer, params)
                            if (response.isSuccessful || response.code() == 409) {
                                success = true
                            }
                        } else {
                            val params = mapOf<String, Any>("p_story_id" to action.targetId, "p_favorited" to true)
                            val response = service.setStoryFavoriteRpc(apiKey, bearer, params)
                            if (response.isSuccessful || response.code() == 409) {
                                success = true
                            }
                        }
                    }
                    "UNFAVORITE" -> {
                        if (action.isReel) {
                            val params = mapOf<String, Any>("p_reel_id" to action.targetId, "p_favorited" to false)
                            val response = service.setReelFavoriteRpc(apiKey, bearer, params)
                            if (response.isSuccessful || response.code() == 404 || response.code() == 409) {
                                success = true
                            }
                        } else {
                            val params = mapOf<String, Any>("p_story_id" to action.targetId, "p_favorited" to false)
                            val response = service.setStoryFavoriteRpc(apiKey, bearer, params)
                            if (response.isSuccessful || response.code() == 404 || response.code() == 409) {
                                success = true
                            }
                        }
                    }
                    "LIKE" -> {
                        if (action.isReel) {
                            val params = mapOf<String, Any>("p_reel_id" to action.targetId, "p_liked" to true)
                            val response = service.setReelLikeRpc(apiKey, bearer, params)
                            if (response.isSuccessful || response.code() == 409) {
                                success = true
                            }
                        } else {
                            val isStory = statesDao.getStateById(action.targetId) != null
                            if (isStory) {
                                val params = mapOf<String, Any>("p_story_id" to action.targetId, "p_liked" to true)
                                val response = service.setStoryLikeRpc(apiKey, bearer, params)
                                if (response.isSuccessful || response.code() == 409) {
                                    success = true
                                }
                            } else {
                                val likeDto = PostLikeDto(postId = action.targetId, userId = action.userId)
                                val response = service.addLike(apiKey, bearer, likeDto)
                                if (response.isSuccessful || response.code() == 409) {
                                    success = true
                                }
                            }
                        }
                    }
                    "SHARE" -> {
                        if (action.isReel) {
                            val bodyMap = mapOf(
                                "reel_id" to action.targetId,
                                "user_id" to action.userId,
                                "created_at" to com.example.data.supabase.SupabaseClient.getNowIsoString()
                            )
                            val response = service.shareState("reel_shares", apiKey, bearer, bodyMap)
                            if (response.isSuccessful || response.code() == 409) {
                                success = true
                            }
                        } else {
                            val isStory = statesDao.getStateById(action.targetId) != null
                            if (isStory) {
                                val bodyMap = mapOf(
                                    "story_id" to action.targetId,
                                    "user_id" to action.userId,
                                    "created_at" to com.example.data.supabase.SupabaseClient.getNowIsoString()
                                )
                                val response = service.shareState("story_shares", apiKey, bearer, bodyMap)
                                if (response.isSuccessful || response.code() == 409) {
                                    success = true
                                }
                            } else {
                                val shareDto = com.example.data.model.PostShareDto(postId = action.targetId, userId = action.userId)
                                val response = service.addShare(apiKey, bearer, shareDto)
                                if (response.isSuccessful || response.code() == 409) {
                                    success = true
                                }
                            }
                        }
                    }
                    "UNLIKE" -> {
                        if (action.isReel) {
                            val params = mapOf<String, Any>("p_reel_id" to action.targetId, "p_liked" to false)
                            val response = service.setReelLikeRpc(apiKey, bearer, params)
                            if (response.isSuccessful || response.code() == 404 || response.code() == 409) {
                                success = true
                            }
                        } else {
                            val isStory = statesDao.getStateById(action.targetId) != null
                            if (isStory) {
                                val params = mapOf<String, Any>("p_story_id" to action.targetId, "p_liked" to false)
                                val response = service.setStoryLikeRpc(apiKey, bearer, params)
                                if (response.isSuccessful || response.code() == 404 || response.code() == 409) {
                                    success = true
                                }
                            } else {
                                val response = service.removeLike(apiKey, bearer, "eq.${action.targetId}", "eq.${action.userId}")
                                if (response.isSuccessful || response.code() == 404 || response.code() == 409) {
                                    success = true
                                }
                            }
                        }
                    }
                    "COMMENT" -> {
                        var parsedCommentText = action.payload ?: ""
                        var parsedParentId: String? = null
                        var parsedLocalCommentId = action.localActionId
                        if (parsedCommentText.startsWith("{")) {
                            try {
                                val json = org.json.JSONObject(parsedCommentText)
                                parsedCommentText = json.optString("text", "")
                                val pId = json.optString("parentId", "")
                                if (pId.isNotBlank() && pId != "null") parsedParentId = pId
                                val lcId = json.optString("localCommentId", "")
                                if (lcId.isNotBlank()) parsedLocalCommentId = lcId
                            } catch (e: Exception) { }
                        }

                        if (action.isReel) {
                            val tableName = "reel_comments"
                            val body = mutableMapOf<String, Any>(
                                "id" to parsedLocalCommentId,
                                "reel_id" to action.targetId,
                                "author_id" to action.userId,
                                "body" to parsedCommentText,
                                "created_at" to SupabaseClient.getNowIsoString()
                            )
                            if (parsedParentId != null) body["parent_comment_id"] = parsedParentId
                            try {
                                val response = service.commentState(tableName, apiKey, bearer, body)
                                if (response.isSuccessful || response.code() == 409) {
                                    success = true
                                    val finalEntity = commentDao.getCommentById(parsedLocalCommentId)?.copy(syncStatus = "synced")
                                    if (finalEntity != null) commentDao.upsert(finalEntity)
                                }
                            } catch (e: Exception) {
                                if (e is retrofit2.HttpException && e.code() == 409) {
                                    success = true
                                    val finalEntity = commentDao.getCommentById(parsedLocalCommentId)?.copy(syncStatus = "synced")
                                    if (finalEntity != null) commentDao.upsert(finalEntity)
                                } else throw e
                            }
                        } else {
                            val isStory = statesDao.getStateById(action.targetId) != null
                            if (isStory) {
                                val tableName = "story_comments"
                                val body = mutableMapOf<String, Any>(
                                    "id" to parsedLocalCommentId,
                                    "story_id" to action.targetId,
                                    "author_id" to action.userId,
                                    "body" to parsedCommentText,
                                    "created_at" to SupabaseClient.getNowIsoString()
                                )
                                if (parsedParentId != null) body["parent_comment_id"] = parsedParentId
                                try {
                                    val response = service.commentState(tableName, apiKey, bearer, body)
                                    if (response.isSuccessful || response.code() == 409) {
                                        success = true
                                        val finalEntity = commentDao.getCommentById(parsedLocalCommentId)?.copy(syncStatus = "synced")
                                        if (finalEntity != null) commentDao.upsert(finalEntity)
                                    }
                                } catch (e: Exception) {
                                    if (e is retrofit2.HttpException && e.code() == 409) {
                                        success = true
                                        val finalEntity = commentDao.getCommentById(parsedLocalCommentId)?.copy(syncStatus = "synced")
                                        if (finalEntity != null) commentDao.upsert(finalEntity)
                                    } else throw e
                                }
                            } else {
                                val commentDto = PostCommentDto(id = parsedLocalCommentId, postId = action.targetId, userId = action.userId, content = parsedCommentText)
                                try {
                                    val response = service.addComment(apiKey, bearer, commentDto)
                                    if (response.isSuccessful || response.code() == 409) {
                                        success = true
                                        val finalEntity = commentDao.getCommentById(parsedLocalCommentId)?.copy(syncStatus = "synced")
                                        if (finalEntity != null) commentDao.upsert(finalEntity)
                                    }
                                } catch (e: Exception) {
                                    if (e is retrofit2.HttpException && e.code() == 409) {
                                        success = true
                                        val finalEntity = commentDao.getCommentById(parsedLocalCommentId)?.copy(syncStatus = "synced")
                                        if (finalEntity != null) commentDao.upsert(finalEntity)
                                    } else throw e
                                }
                            }
                        }
                    }
                    "DELETE_COMMENT" -> {
                        if (action.isReel) {
                            val tableName = "reel_comments"
                            val response = service.deleteComment(tableName, apiKey, bearer, "eq.${action.targetId}")
                            if (response.isSuccessful || response.code() == 404) {
                                success = true
                                commentDao.deleteById(action.targetId)
                            }
                        } else {
                            success = true
                            commentDao.deleteById(action.targetId)
                        }
                    }
                    "DELETE_POST" -> {
                        val response = service.deletePost(apiKey, bearer, "eq.${action.targetId}")
                        if (response.isSuccessful || response.code() == 404) {
                            success = true
                            // Already deleted locally
                        }
                    }
                    "UPDATE_POST" -> {
                        val content = action.payload ?: ""
                        val updates = mapOf("content" to content)
                        val response = service.updatePost(apiKey, bearer, "eq.${action.targetId}", updates)
                        if (response.isSuccessful || response.code() == 404) {
                            success = true
                        }
                    }
                    else -> {
                        success = false
                    }
                }
                if (success) {
                    pendingDao.deleteActionById(action.localActionId)
                    Log.d("SocialSyncWorker", "Action ${action.actionType} on ${action.targetId} synced successfully.")
                } else {
                    anyFailed = true
                    pendingDao.updateActionStatus(action.localActionId, "pending")
                }

            } catch (e: Exception) {
                // If it's a completely unknown error, keep it as pending and retry.
                Log.e("SocialSyncWorker", "Error syncing action ${action.localActionId}", e)
                anyFailed = true
                pendingDao.updateActionStatus(action.localActionId, "pending")
            }
        }

        return if (anyFailed) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<SocialSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "social_sync_work",
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                workRequest
            )
        }
    }
}
