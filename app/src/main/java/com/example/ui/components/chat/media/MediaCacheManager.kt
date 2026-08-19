package com.example.ui.components.chat.media

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object MediaCacheManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    fun prefetchImage(context: Context, url: String) {
        if (url.isEmpty()) return
        scope.launch {
            val request = ImageRequest.Builder(context.applicationContext)
                .data(url)
                .build()
            context.applicationContext.imageLoader.enqueue(request)
        }
    }

    fun isImageCached(context: Context, url: String): Boolean {
        return context.applicationContext.imageLoader.diskCache?.get(url) != null
    }

    /**
     * Clears only RAM. Disk media is intentionally preserved so an offline
     * restart does not turn previously loaded chat media into blank cards.
     */
    fun clearMemoryCache(context: Context) {
        context.applicationContext.imageLoader.memoryCache?.clear()
    }

    /**
     * Explicit destructive operation. Call this only from a user-confirmed
     * "clear media cache" action.
     */
    fun clearAllCache(context: Context) {
        context.applicationContext.imageLoader.memoryCache?.clear()
        context.applicationContext.imageLoader.diskCache?.clear()
    }

    @Deprecated(
        message = "Use clearMemoryCache() to preserve offline media, or clearAllCache() only after explicit user confirmation",
        replaceWith = ReplaceWith("clearMemoryCache(context)")
    )
    fun clearCache(context: Context) {
        clearMemoryCache(context)
    }
}
