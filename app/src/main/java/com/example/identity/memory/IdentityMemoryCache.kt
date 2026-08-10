package com.example.identity.memory

import android.util.LruCache
import com.example.identity.model.CachedProfile

object IdentityMemoryCache {
    private const val MAX_PROFILES = 200
    // Optional: Max items for paths
    private const val MAX_AVATAR_PATHS = 500
    private const val MAX_COVER_PATHS = 200

    val profiles = object : LruCache<String, CachedProfile>(MAX_PROFILES) {}
    
    // Store userId -> local file path string (not Bitmap, keeping it light)
    val avatars = object : LruCache<String, String>(MAX_AVATAR_PATHS) {}
    val covers = object : LruCache<String, String>(MAX_COVER_PATHS) {}
    
    fun clear() {
        profiles.evictAll()
        avatars.evictAll()
        covers.evictAll()
    }
}
