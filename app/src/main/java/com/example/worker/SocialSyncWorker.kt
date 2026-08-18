package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.database.PanalinkDatabase
import com.example.data.model.PostCommentDto
import com.example.data.model.PostLikeDto
import com.example.data.supabase.SupabaseClient
import java.util.concurrent.TimeUnit

class SocialSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = PanalinkDatabase.getDatabase(context)
    private val pendingDao = db.pendingSocialActionDao()
    private val commentDao = db.commentDao()
    private val statesDao = db.statesDao()

    override suspend fun doWork(): Result {
        Log.i("SocialSyncWorker", "Starting sync of pending social actions...")
        if (!SupabaseClient.isConfigured) return Result.success()

        val pendingActions = pendingDao.getPendingActions()
        if (pendingActions.isEmpty()) return Result.success()

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
                            val response = service.setReelFavoriteRpc(apiKey, bearer, mapOf("p_reel_id" to action.targetId, "p_favorited" to true))
                            success = response.isSuccessful || response.code() == 409
                        } else {
                            val response = service.setStoryFavoriteRpc(apiKey, bearer, mapOf("p_story_id" to action.targetId, "p_favorited" to true))
                            success = response.isSuccessful || response.code() == 409
                        }
                    }
                    "UNFAVORITE" -> {
                        if (action.isReel) {
                            val response = service.setReelFavoriteRpc(apiKey, bearer, mapOf("p_reel_id" to action.targetId, "p_favorited" to false))
                            success = response.isSuccessful || response.code() in listOf(404, 409)
                        } else {
                            val response = service.setStoryFavoriteRpc(apiKey, bearer, mapOf("p_story_id" to action.targetId, "p_favorited" to false))
                            success = response.isSuccessful || response.code() in listOf(404, 409)
                        }
                    }
                    "LIKE" -> {
                        if (action.isReel) {
                            val response = service.setReelLikeRpc(apiKey, bearer, mapOf("p_reel_id" to action.targetId, "p_liked" to true))
                            success = response.isSuccessful || response.code() == 409
                        } else if (statesDao.getStateById(action.targetId) != null) {
                            val response = service.setStoryLikeRpc(apiKey, bearer, mapOf("p_story_id" to action.targetId, "p_liked" to true))
                            success = response.isSuccessful || response.code() == 409
                        } else {
                            val response = service.addLike(apiKey, bearer, PostLikeDto(postId = action.targetId, userId = action.userId))
                            success = response.isSuccessful || response.code() == 409
                        }
                    }
                    "UNLIKE" -> {
                        if (action.isReel) {
                            val response = service.setReelLikeRpc(apiKey, bearer, mapOf("p_reel_id" to action.targetId, "p_liked" to false))
                            success = response.isSuccessful || response.code() in listOf(404, 409)
                        } else if (statesDao.getStateById(action.targetId) != null) {
                            val response = service.setStoryLikeRpc(apiKey, bearer, mapOf("p_story_id" to action.targetId, "p_liked" to false))
                            success = response.isSuccessful || response.code() in listOf(404, 409)
                        } else {
                            val response = service.removeLike(apiKey, bearer, "eq.${action.targetId}", "eq.${action.userId}")
                            success = response.isSuccessful || response.code() in listOf(404, 409)
                        }
                    }
                    "SHARE" -> {
                        if (action.isReel) {
                            val response = service.shareState("reel_shares", apiKey, bearer, mapOf(
                                "reel_id" to action.targetId,
                                "user_id" to action.userId,
                                "created_at" to SupabaseClient.getNowIsoString()
                            ))
                            success = response.isSuccessful || response.code() == 409
                        } else if (statesDao.getStateById(action.targetId) != null) {
                            val response = service.shareState("story_shares", apiKey, bearer, mapOf(
                                "story_id" to action.targetId,
                                "user_id" to action.userId,
                                "created_at" to SupabaseClient.getNowIsoString()
                            ))
                            success = response.isSuccessful || response.code() == 409
                        } else {
                            val response = service.addShare(apiKey, bearer, com.example.data.model.PostShareDto(postId = action.targetId, userId = action.userId))
                            success = response.isSuccessful || response.code() == 409
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
                                val parent = json.optString("parentId", "")
                                if (parent.isNotBlank() && parent != "null") parsedParentId = parent
                                val localId = json.optString("localCommentId", "")
                                if (localId.isNotBlank()) parsedLocalCommentId = localId
                            } catch (_: Exception) {}
                        }

                        if (action.isReel) {
                            val body = mutableMapOf<String, Any>(
                                "id" to parsedLocalCommentId,
                                "reel_id" to action.targetId,
                                "author_id" to action.userId,
                                "body" to parsedCommentText,
                                "created_at" to SupabaseClient.getNowIsoString()
                            )
                            if (parsedParentId != null) body["parent_comment_id"] = parsedParentId
                            val response = service.commentState("reel_comments", apiKey, bearer, body)
                            if (response.isSuccessful || response.code() == 409) {
                                success = true
                                commentDao.getCommentById(parsedLocalCommentId)?.let { commentDao.upsert(it.copy(syncStatus = "synced")) }
                            }
                        } else if (statesDao.getStateById(action.targetId) != null) {
                            val body = mutableMapOf<String, Any>(
                                "id" to parsedLocalCommentId,
                                "story_id" to action.targetId,
                                "author_id" to action.userId,
                                "body" to parsedCommentText,
                                "created_at" to SupabaseClient.getNowIsoString()
                            )
                            if (parsedParentId != null) body["parent_comment_id"] = parsedParentId
                            val response = service.commentState("story_comments", apiKey, bearer, body)
                            if (response.isSuccessful || response.code() == 409) {
                                success = true
                                commentDao.getCommentById(parsedLocalCommentId)?.let { commentDao.upsert(it.copy(syncStatus = "synced")) }
                            }
                        } else {
                            val response = service.addComment(
                                apiKey,
                                bearer,
                                PostCommentDto(id = parsedLocalCommentId, postId = action.targetId, userId = action.userId, content = parsedCommentText)
                            )
                            if (response.isSuccessful || response.code() == 409) {
                                success = true
                                commentDao.getCommentById(parsedLocalCommentId)?.let { commentDao.upsert(it.copy(syncStatus = "synced")) }
                            }
                        }
                    }
                    "DELETE_COMMENT" -> {
                        if (action.isReel) {
                            val response = service.deleteComment("reel_comments", apiKey, bearer, "eq.${action.targetId}")
                            success = response.isSuccessful || response.code() == 404
                            if (success) commentDao.deleteById(action.targetId)
                        } else if (statesDao.getStateById(action.targetId) != null) {
                            val response = service.deleteComment("story_comments", apiKey, bearer, "eq.${action.targetId}")
                            success = response.isSuccessful || response.code() == 404
                            if (success) commentDao.deleteById(action.targetId)
                        } else {
                            val response = service.deleteComment("comments", apiKey, bearer, "eq.${action.targetId}")
                            success = response.isSuccessful || response.code() == 404
                            if (success) commentDao.deleteById(action.targetId)
                        }
                    }
                    "DELETE_POST" -> {
                        val response = service.deletePost(apiKey, bearer, "eq.${action.targetId}")
                        success = response.isSuccessful || response.code() == 404
                    }
                    "UPDATE_POST" -> {
                        val response = service.updatePost(apiKey, bearer, "eq.${action.targetId}", mapOf("content" to (action.payload ?: "")))
                        success = response.isSuccessful || response.code() == 404
                    }
                    else -> Unit
                }

                if (success) {
                    pendingDao.deleteActionById(action.localActionId)
                    Log.d("SocialSyncWorker", "Action ${action.actionType} on ${action.targetId} synced successfully.")
                } else {
                    anyFailed = true
                    pendingDao.updateActionStatus(action.localActionId, "pending")
                }
            } catch (e: Exception) {
                Log.e("SocialSyncWorker", "Error syncing action ${action.localActionId}", e)
                anyFailed = true
                pendingDao.updateActionStatus(action.localActionId, "pending")
            }
        }

        return if (anyFailed) Result.retry() else Result.success()
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
