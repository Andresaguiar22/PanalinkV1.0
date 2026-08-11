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
                                "reel_id" to action.targetId,
                                "author_id" to action.userId,
                                "body" to parsedCommentText,
                                "created_at" to SupabaseClient.getNowIsoString()
                            )
                            if (parsedParentId != null) body["parent_comment_id"] = parsedParentId
                            val response = service.commentState(tableName, apiKey, bearer, body)
                            if (response.isSuccessful) {
                                success = true
                                // We don't have a returned DTO here directly, but we can clear local temp comment
                                commentDao.deleteById(parsedLocalCommentId)
                            }
                        } else {
                            val isStory = statesDao.getStateById(action.targetId) != null
                            if (isStory) {
                                val tableName = "story_comments"
                                val body = mutableMapOf<String, Any>(
                                    "story_id" to action.targetId,
                                    "author_id" to action.userId,
                                    "body" to parsedCommentText,
                                    "created_at" to SupabaseClient.getNowIsoString()
                                )
                                if (parsedParentId != null) body["parent_comment_id"] = parsedParentId
                                val response = service.commentState(tableName, apiKey, bearer, body)
                                if (response.isSuccessful) {
                                    success = true
                                    commentDao.deleteById(parsedLocalCommentId)
                                }
                            } else {
                                val commentDto = PostCommentDto(postId = action.targetId, userId = action.userId, content = parsedCommentText)
                                val response = service.addComment(apiKey, bearer, commentDto)
                                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                                    val createdRemote = response.body()!!.first()
                                    success = true
                                    // Replace temporary comment with final remote comment in Room
                                    val localTemp = commentDao.getCommentById(parsedLocalCommentId)
                                    if (localTemp != null) {
                                        commentDao.deleteById(parsedLocalCommentId)
                                        val finalEntity = CommentEntity.fromPostCommentDto(createdRemote)
                                        commentDao.upsert(finalEntity)
                                    }
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
