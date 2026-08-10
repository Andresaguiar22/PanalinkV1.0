package com.example.notification.cache

import android.graphics.Bitmap
import android.util.LruCache
import androidx.annotation.Keep

@Keep
object NotificationMediaCache {

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 10

    private val lruCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    fun getMedia(url: String): Bitmap? {
        if (url.isBlank()) return null
        return synchronized(lruCache) { lruCache.get(url) }
    }

    fun putMedia(url: String, bitmap: Bitmap) {
        if (url.isBlank()) return
        synchronized(lruCache) {
            if (lruCache.get(url) == null) {
                lruCache.put(url, bitmap)
            }
        }
    }

    fun clear() {
        synchronized(lruCache) { lruCache.evictAll() }
    }
}
