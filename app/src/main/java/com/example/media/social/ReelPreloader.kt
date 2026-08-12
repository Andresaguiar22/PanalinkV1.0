package com.example.media.social

import android.content.Context
import android.util.Log
import com.example.data.model.UserState
import com.example.data.repository.CdnManager
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ReelPreloader {
    private const val TAG = "ReelPreloader"
    private const val PRELOAD_AHEAD = 3

    /**
     * TikTok-style predictive preloading: persist the next three reels locally.
     * The current reel is also included so an already viewed reel remains available
     * when the device goes offline later.
     */
    fun preloadNextReels(
        context: Context,
        reels: List<UserState>,
        currentIndex: Int,
        scope: CoroutineScope
    ) {
        if (reels.isEmpty() || currentIndex !in reels.indices) return

        val first = currentIndex.coerceAtLeast(0)
        val last = (currentIndex + PRELOAD_AHEAD).coerceAtMost(reels.lastIndex)
        val appCtx = context.applicationContext

        scope.launch(Dispatchers.IO) {
            val repository = MediaRepository(appCtx, MediaStorageManager(appCtx))

            for (index in first..last) {
                val reel = reels.getOrNull(index) ?: continue
                val remoteUrl = CdnManager.resolveMediaUrlSync(reel.mediaUrl)
                if (remoteUrl.isBlank() || !remoteUrl.startsWith("http")) continue

                // Stable identity deliberately excludes the CDN host. This allows the
                // same offline file to survive Cloudflare quick-tunnel rotation.
                val mediaId = "reel_${reel.id}"
                try {
                    Log.i(TAG, "Persisting reel index=$index id=$mediaId")
                    repository.syncManager.syncMedia(
                        id = mediaId,
                        remoteUrl = remoteUrl,
                        type = "REEL",
                        ownerId = reel.userId
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Reel preload failed id=$mediaId", e)
                }
            }
        }
    }
}
