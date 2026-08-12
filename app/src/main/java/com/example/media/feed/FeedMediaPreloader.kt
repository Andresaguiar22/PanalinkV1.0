package com.example.media.feed

import android.content.Context
import android.util.Log
import com.example.data.model.PostDto
import com.example.media.repository.MediaRepository
import com.example.media.storage.MediaStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URI

object FeedMediaPreloader {
    private const val TAG = "FeedMediaPreloader"
    private const val PRELOAD_AHEAD = 3

    fun preloadNextPostsMedia(
        context: Context,
        posts: List<PostDto>,
        currentIndex: Int,
        scope: CoroutineScope
    ) {
        if (posts.isEmpty() || currentIndex !in posts.indices) return

        val startIndex = currentIndex.coerceAtLeast(0)
        val endIndex = (currentIndex + PRELOAD_AHEAD).coerceAtMost(posts.lastIndex)
        val contextApp = context.applicationContext

        scope.launch(Dispatchers.IO) {
            val repository = MediaRepository(contextApp, MediaStorageManager(contextApp))

            for (i in startIndex..endIndex) {
                val post = posts.getOrNull(i) ?: continue
                for (url in post.mediaUrls.orEmpty()) {
                    if (!url.startsWith("http://") && !url.startsWith("https://")) continue

                    val mediaId = stableMediaId(url)
                    val type = typeFor(url)

                    try {
                        Log.i(TAG, "Prefetching post=${post.id} media=$mediaId type=$type")
                        repository.syncManager.syncMedia(mediaId, url, type, post.userId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Prefetch failed for $url", e)
                    }
                }
            }
        }
    }

    private fun stableMediaId(url: String): String {
        return runCatching {
            val uri = URI(url)
            val logicalKey = buildString {
                append(uri.path ?: url)
                uri.query?.let { append('?').append(it) }
            }
            "media_${kotlin.math.abs(logicalKey.hashCode())}"
        }.getOrElse {
            "media_${kotlin.math.abs(url.hashCode())}"
        }
    }

    private fun typeFor(url: String): String {
        val normalized = url.lowercase().substringBefore('#').substringBefore('?')
        return if (
            normalized.endsWith(".mp4") ||
            normalized.endsWith(".webm") ||
            normalized.endsWith(".mov") ||
            normalized.endsWith(".m4v") ||
            normalized.contains("/videos/") ||
            normalized.contains("video")
        ) "POST_VIDEO" else "POST_IMAGE"
    }
}
