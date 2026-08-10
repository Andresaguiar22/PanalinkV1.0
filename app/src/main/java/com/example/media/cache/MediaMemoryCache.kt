package com.example.media.cache

import android.util.LruCache
import com.example.media.model.MediaAssetEntity

object MediaMemoryCache {
    private const val MAX_ENTRIES = 500

    private val cache = object : LruCache<String, MediaAssetEntity>(MAX_ENTRIES) {}

    fun put(id: String, entity: MediaAssetEntity) {
        cache.put(id, entity)
    }

    fun get(id: String): MediaAssetEntity? {
        return cache.get(id)
    }

    fun clear() {
        cache.evictAll()
    }
}
