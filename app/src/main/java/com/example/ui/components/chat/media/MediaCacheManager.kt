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
            val request = ImageRequest.Builder(context)
                .data(url)
                .build()
            context.imageLoader.enqueue(request)
        }
    }

    fun isImageCached(context: Context, url: String): Boolean {
        return context.imageLoader.diskCache?.get(url) != null
    }

    fun clearCache(context: Context) {
        context.imageLoader.memoryCache?.clear()
        context.imageLoader.diskCache?.clear()
    }
}
