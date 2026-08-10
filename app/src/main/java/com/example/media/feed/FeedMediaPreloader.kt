package com.example.media.feed

import android.content.Context
import android.util.Log
import com.example.data.model.PostDto
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object FeedMediaPreloader {
    private const val TAG = "FeedMediaPreloader"
    private const val PRELOAD_COUNT = 2

    fun preloadNextPostsMedia(
        context: Context,
        posts: List<PostDto>,
        currentIndex: Int,
        scope: CoroutineScope
    ) {
        if (posts.isEmpty() || currentIndex < 0) return

        val startIndex = currentIndex + 1
        val endIndex = (currentIndex + PRELOAD_COUNT).coerceAtMost(posts.size - 1)

        if (startIndex > endIndex) return

        val contextApp = context.applicationContext
        scope.launch(Dispatchers.IO) {
            val repository = MediaRepository(contextApp, MediaStorageManager(contextApp))

            for (i in startIndex..endIndex) {
                val post = posts.getOrNull(i) ?: continue
                val urls = post.mediaUrls ?: emptyList()

                for (url in urls) {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        val mediaId = "media_${kotlin.math.abs(url.hashCode())}"
                        val type = if (url.endsWith(".mp4") || url.contains("video")) "video" else "image"
                        try {
                            Log.i(TAG, "Preloading post media: $mediaId")
                            repository.syncManager.syncMedia(mediaId, url, type, post.userId)
                        } catch (e: Exception) {
                            Log.e(TAG, "Preload failed for $url", e)
                        }
                    }
                }
            }
        }
    }
}
