package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.data.database.PanalinkDatabase
import com.example.data.model.PostCommentDto
import com.example.data.model.PostLikeDto
import com.example.data.supabase.SessionManager
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

        val sessionValid = SessionManager.validateAndRefreshSessionIfNeeded()
        if (!sessionValid) {
            Log.w("SocialSyncWorker", "No valid session available; pending actions remain queued.")
            return if (pendingDao.getPendingActions().isNotEmpty()) Result.retry() else Result.success()
        }

        val pendingActions = pendingDao.getPendingActions()
        if (pendingActions.isEmpty()) {
            Log.i("SocialSyncWorker", "No pending social actions found.")
            return Result.success()
        }

        var anyFailed = false
        var anyStale = false

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
                            val response = service.setReelFavoriteRpc(apiKey, bearer, mapOf<String, Any>("p_reel_id" to action.targetId, "p_favorited" to true))
                            success = response.isSuccessful || response.code() == 409
                        } else {
                            val response = service.setStoryFavoriteRpc(apiKey, bearer, mapOf<String, Any>("p_story_id" to action.targetId, "p_favorited" to true))
                            success = response.isSuccessful || response.code() == 409
                        }
                    }
                    "UNFAVORITE" -> {
                        if (action.isReel) {
                            val response = service.setReelFavoriteRpc(apiKey, bearer, mapOf<String, Any>("p_reel_id" to action.targetId, "p_favorited" to false))
                            success = response.isSuccessful || response.code() == 404 || response.code() == 409
                        } else {
                            val response = service.setStoryFavoriteRpc(apiKey, bearer, mapOf<String, Any>("p_story_id" to action.targetId, "p_favorited" to false))
                            success = response.isSuccessful || response.code() == 404 || response.code() == 409
                        }
                    }
                    "LIKE" -> {
                        if (action.isReel) {
                            val response = service.setReelLikeRpc(apiKey, bearer, mapOf<String, Any>("p_reel_id" to action.targetId, "p_liked" to true))
                            success = response.isSuccessful || response.code() == 409
                        } else {
                            val isStory = statesDao.getStateById(action.targetId) != null
                            if (isStory) {
                                val response = service.setStoryLikeRpc(apiKey, bearer, mapOf<String, Any>("p_story_id" to action.targetId, "p_liked" to true))
                                success = response.isSuccessful || response.code() == 409
                            } else {
                                val response = service.addLike(apiKey, bearer, PostLikeDto(postId = action.targetId, userId = action.userId))
                                success = response.isSuccessful || response.code() == 409
                            }
                        }
                    }
                    "SHARE" -> {
                        if (action.isReel) {
                            val bodyMap = mapOf("reel_id" to action.targetId, "user_id" to action.userId, "created_at" to SupabaseClient.getNowIsoString())
                            val response = service.shareState("reel_shares", apiKey, bearer, bodyMap)
                            success = response.isSuccessful || response.code() == 409
                        } else {
                            val isStory = statesDao.getStateById(action.targetId) != null
                            if (isStory) {
                                val bodyMap = mapOf("story_id" to action.targetId, "user_id" to action.userId, "created_at" to SupabaseClient.getNowIsoString())
                                val response = service.shareState("story_shares", apiKey, bearer, bodyMap)
                                success = response.isSuccessful || response.code() == 409
                            } else {
                                val response = service.addShare(apiKey, bearer, com.example.data.model.PostShareDto(postId = action.targetId, userId = action.userId))
                                success = response.isSuccessful || response.code() == 409
                            }
                        }
                    }
                    "UNLIKE" -> {
                        if (action.isReel) {
                            val response = service.setReelLikeRpc(apiKey, bearer, mapOf<String, Any>("p_reel_id" to action.targetId, "p_liked" to false))
                            success = response.isSuccessful || response.code() == 404 || response.code() == 409
                        } else {
                            val isStory = statesDao.getStateById(action.targetId) != null
                            if (isStory) {
                                val response = service.setStoryLikeRpc(apiKey, bearer, mapOf<String, Any>("p_story_id" to action.targetId, "p_liked" to false))
                                success = response.isSuccessful || response.code() == 404 || response.code() == 409
                            } else {
                                val response = service.removeLike(apiKey, bearer, "eq.${action.targetId}", "eq.${action.userId}")
                                success = response.isSuccessful || response.code() == 404 || response.code() == 409
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
                            } catch (_: Exception) { }
                        }

                        if (action.isReel) {
                            val body = mutableMapOf<String, Any>("id" to parsedLocalCommentId, "reel_id" to action.targetId, "author_id" to action.userId, "body" to parsedCommentText, "created_at" to SupabaseClient.getNowIsoString())
                            if (parsedParentId != null) body["parent_comment_id"] = parsedParentId
                            val response = service.commentState("reel_comments", apiKey, bearer, body)
                            if (response.isSuccessful || response.code() == 409) {
                                success = true
                                commentDao.getCommentById(parsedLocalCommentId)?.let { commentDao.upsert(it.copy(syncStatus = "synced")) }
                            }
                        } else {
                            val isStory = statesDao.getStateById(action.targetId) != null
                            if (isStory) {
                                val body = mutableMapOf<String, Any>("id" to parsedLocalCommentId, "story_id" to action.targetId, "author_id" to action.userId, "body" to parsedCommentText, "created_at" to SupabaseClient.getNowIsoString())
                                if (parsedParentId != null) body["parent_comment_id"] = parsedParentId
                                val response = service.commentState("story_comments", apiKey, bearer, body)
                                if (response.isSuccessful || response.code() == 409) {
                                    success = true
                                    commentDao.getCommentById(parsedLocalCommentId)?.let { commentDao.upsert(it.copy(syncStatus = "synced")) }
                                }
                            } else {
                                val response = service.addComment(apiKey, bearer, PostCommentDto(id = parsedLocalCommentId, postId = action.targetId, userId = action.userId, content = parsedCommentText))
                                if (response.isSuccessful || response.code() == 409) {
                                    success = true
                                    commentDao.getCommentById(parsedLocalCommentId)?.let { commentDao.upsert(it.copy(syncStatus = "synced")) }
                                }
                            }
                        }
                    }
                    "DELETE_COMMENT" -> {
                        val localComment = commentDao.getCommentById(action.targetId)
                        val parentStateId = localComment?.targetId
                        val parentState = parentStateId?.let { statesDao.getStateById(it) }

                        val table = when {
                            !action.isReel -> "post_comments"
                            parentState?.type == "reel" -> "reel_comments"
                            parentState?.type == "story" -> "story_comments"
                            localComment?.isReel == true -> "reel_comments"
                            else -> "story_comments"
                        }

                        val response = service.deleteComment(table, apiKey, bearer, "eq.${action.targetId}")
                        if (response.isSuccessful || response.code() == 404) {
                            success = true
                            commentDao.deleteById(action.targetId)
                        }
                    }
                    "DELETE_POST" -> {
                        val response = service.deletePost(apiKey, bearer, "eq.${action.targetId}")
                        success = response.isSuccessful || response.code() == 404
                    }
                    "UPDATE_POST" -> {
                        val response = service.updatePost(apiKey, bearer, "eq.${action.targetId}", mapOf("content" to (action.payload ?: "")))
                        success = response.isSuccessful
                    }
                }

                if (success) {
                    if (action.actionFamily != null && action.desiredState != null) {
                        val deleted = pendingDao.deleteIfStillCurrent(
                            id = action.localActionId,
                            family = action.actionFamily,
                            desiredState = action.desiredState,
                            revision = action.revision
                        )
                        if (deleted == 0) {
                            // The RPC succeeded for an obsolete snapshot. Do not
                            // delete the newer intention; request another pass.
                            anyStale = true
                            Log.d(
                                "SocialSyncWorker",
                                "Stale declarative action ${action.localActionId} rev=${action.revision}; newer intent remains queued."
                            )
                        } else {
                            Log.d("SocialSyncWorker", "Declarative action ${action.actionType} on ${action.targetId} synced successfully.")
                        }
                    } else {
                        pendingDao.deleteActionById(action.localActionId)
                        Log.d("SocialSyncWorker", "Event action ${action.actionType} on ${action.targetId} synced successfully.")
                    }
                } else {
                    anyFailed = true
                    if (action.actionFamily != null && action.desiredState != null) {
                        val updated = pendingDao.updateStatusIfStillCurrent(
                            id = action.localActionId,
                            family = action.actionFamily,
                            desiredState = action.desiredState,
                            revision = action.revision,
                            status = "pending"
                        )
                        if (updated == 0) {
                            anyStale = true
                            Log.d(
                                "SocialSyncWorker",
                                "Ignoring stale failure for ${action.localActionId} rev=${action.revision}; newer intent remains authoritative."
                            )
                        }
                    } else {
                        pendingDao.updateActionStatus(action.localActionId, "pending")
                    }
                }
            } catch (e: Exception) {
                Log.e("SocialSyncWorker", "Error syncing action ${action.localActionId}", e)
                anyFailed = true
                if (action.actionFamily != null && action.desiredState != null) {
                    val updated = pendingDao.updateStatusIfStillCurrent(
                        id = action.localActionId,
                        family = action.actionFamily,
                        desiredState = action.desiredState,
                        revision = action.revision,
                        status = "pending"
                    )
                    if (updated == 0) {
                        anyStale = true
                        Log.d(
                            "SocialSyncWorker",
                            "Ignoring stale exception for ${action.localActionId} rev=${action.revision}; newer intent remains authoritative."
                        )
                    }
                } else {
                    pendingDao.updateActionStatus(action.localActionId, "pending")
                }
            }
        }

        return if (anyFailed || anyStale) Result.retry() else Result.success()
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
                ExistingWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
