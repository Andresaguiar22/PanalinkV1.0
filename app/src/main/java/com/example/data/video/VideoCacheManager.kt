package com.example.data.video

import android.content.Context
import android.util.Log
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import kotlinx.coroutines.launch

import androidx.media3.common.util.UnstableApi

@UnstableApi
object VideoCacheManager {
    private const val CACHE_DIR_NAME = "media3_video_cache"
    private const val MAX_CACHE_SIZE_BYTES = 1024 * 1024 * 1024L // 1 GB for premium experience

    @Volatile
    private var cache: SimpleCache? = null

    fun removeVideoCache(url: String) {
        try {
            val key = androidx.media3.datasource.cache.CacheKeyFactory.DEFAULT.buildCacheKey(
                androidx.media3.datasource.DataSpec(android.net.Uri.parse(url))
            )
            cache?.removeResource(key)
        } catch (e: Exception) {
            Log.e("VideoCacheManager", "Error clearing cache for url: $url", e)
        }
    }

    @Synchronized
    fun getCache(context: Context): SimpleCache? {
        if (cache == null) {
            try {
                val cacheDir = File(context.cacheDir, CACHE_DIR_NAME)
                val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
                val databaseProvider = StandaloneDatabaseProvider(context)
                cache = SimpleCache(cacheDir, evictor, databaseProvider)
            } catch (e: Throwable) {
                Log.e("VideoCacheManager", "Error creating SimpleCache", e)
                cache = null
            }
        }
        return cache
    }
}
