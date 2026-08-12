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
import java.net.URI

object StoryPreloader {
    private const val TAG = "StoryPreloader"
    private const val LOOKAHEAD_GROUPS = 3

    private fun stableMediaId(state: UserState, remoteUrl: String): String {
        val path = runCatching { URI(remoteUrl).path }
            .getOrNull()
            ?.trim('/')
            ?.takeIf { it.isNotBlank() }
            ?: remoteUrl.substringAfter("?", remoteUrl)
        return "story_${state.id}_${path.hashCode().toUInt().toString(16)}"
    }

    fun preloadStories(
        context: Context,
        userStates: List<UserStateWithUser>,
        currentUserIndex: Int,
        currentStoryIndex: Int,
        scope: CoroutineScope
    ) {
        if (userStates.isEmpty() || currentUserIndex !in userStates.indices) return

        val appCtx = context.applicationContext
        scope.launch(Dispatchers.IO) {
            val repository = MediaRepository(appCtx, MediaStorageManager(appCtx))
            val targets = buildList {
                userStates.getOrNull(currentUserIndex)?.state?.let(::add)
                for (offset in 1..LOOKAHEAD_GROUPS) {
                    userStates.getOrNull(currentUserIndex + offset)?.state?.let(::add)
                }
            }.distinctBy { it.id }

            for (target in targets) {
                val remoteUrl = com.example.data.repository.CdnManager.resolveMediaUrlSync(target.mediaUrl) ?: continue
                if (!remoteUrl.startsWith("http://") && !remoteUrl.startsWith("https://")) continue

                val mediaId = stableMediaId(target, remoteUrl)
                val type = if (
                    target.mediaType.equals("video", ignoreCase = true) ||
                    remoteUrl.substringBefore('?').lowercase().endsWith(".mp4")
                ) "video" else "image"

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
