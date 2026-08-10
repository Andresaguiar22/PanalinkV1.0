package com.example.media.social

import android.content.Context
import android.util.Log
import com.example.data.model.UserState
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ReelPreloader {
    private const val TAG = "ReelPreloader"

    fun preloadNextReels(
        context: Context,
        reels: List<UserState>,
        currentIndex: Int,
        scope: CoroutineScope
    ) {
        if (reels.isEmpty() || currentIndex < 0) return

        val nextIndex = currentIndex + 1
        if (nextIndex >= reels.size) return

        val appCtx = context.applicationContext
        scope.launch(Dispatchers.IO) {
            val repository = MediaRepository(appCtx, MediaStorageManager(appCtx))

            val nextReel = reels.getOrNull(nextIndex) ?: return@launch
            val remoteUrl = nextReel.mediaUrl ?: return@launch

            if (remoteUrl.startsWith("http://") || remoteUrl.startsWith("https://")) {
                val mediaId = "reel_${nextReel.id}_${kotlin.math.abs(remoteUrl.hashCode())}"
                try {
                    Log.i(TAG, "Preloading reel: $mediaId")
                    repository.syncManager.syncMedia(mediaId, remoteUrl, "video", nextReel.userId)
                } catch (e: Exception) {
                    Log.e(TAG, "Preload reel failed for $mediaId", e)
                }
            }
        }
    }
}
