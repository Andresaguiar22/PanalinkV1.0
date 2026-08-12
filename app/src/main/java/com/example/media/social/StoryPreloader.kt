package com.example.media.social

import android.content.Context
import android.util.Log
import com.example.data.model.UserState
import com.example.data.model.UserStateWithUser
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object StoryPreloader {
    private const val TAG = "StoryPreloader"

    fun preloadStories(
        context: Context,
        userStates: List<UserStateWithUser>,
        currentUserIndex: Int,
        currentStoryIndex: Int,
        scope: CoroutineScope
    ) {
        if (userStates.isEmpty() || currentUserIndex < 0 || currentUserIndex >= userStates.size) return

        val appCtx = context.applicationContext
        scope.launch(Dispatchers.IO) {
            val repository = MediaRepository(appCtx, MediaStorageManager(appCtx))

            val currentGroup = userStates.getOrNull(currentUserIndex)
            val nextGroup = userStates.getOrNull(currentUserIndex + 1)

            val targets = mutableListOf<UserState>()

            // Current user story
            currentGroup?.state?.let { state ->
                targets.add(state)
            }

            // Next user story
            nextGroup?.state?.let { state ->
                targets.add(state)
            }

            for (target in targets) {
                val remoteUrl = com.example.data.repository.CdnManager.resolveMediaUrlSync(target.mediaUrl) ?: continue
                if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
                    val mediaId = "story_${target.id}_${kotlin.math.abs(remoteUrl.hashCode())}"
                    val type = if (target.mediaType == "video" || remoteUrl.endsWith(".mp4")) "video" else "image"
                    try {
                        Log.i(TAG, "Preloading story: $mediaId")
                        repository.syncManager.syncMedia(mediaId, remoteUrl, type, target.userId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Preload story failed for $mediaId", e)
                    }
                }
            }
        }
    }
}
