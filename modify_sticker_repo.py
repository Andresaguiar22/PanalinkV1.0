import re

with open('app/src/main/java/com/example/data/repository/StickerRepository.kt', 'r') as f:
    content = f.read()

start_str = "// --- SharedPreferences Helpers for Recents & Favorites ---"
parts = content.split(start_str)

new_code = """// --- RPC Helpers for Recents, Favorites & Saved ---
    
    // In-memory cache to prevent constant fetching
    private var cachedSaved = mutableListOf<StickerResult>()
    private var cachedFavorites = mutableListOf<StickerResult>()
    private var cachedRecents = mutableListOf<StickerResult>()
    private var cacheInitialized = false

    suspend fun syncStickersFromRemote(context: Context) = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) return@withContext
        try {
            val service = SupabaseClient.apiService ?: return@withContext
            val token = SupabaseClient.currentUser?.token ?: return@withContext
            val apiKey = SupabaseClient.supabaseAnonKey
            val auth = "Bearer $token"

            val favRes = service.getFavoriteStickers(apiKey, auth)
            if (favRes.isSuccessful) {
                cachedFavorites = parseStickerResultList(favRes.body())
            }

            val savedRes = service.getSavedStickers(apiKey, auth)
            if (savedRes.isSuccessful) {
                cachedSaved = parseStickerResultList(savedRes.body())
            }

            val recentsRes = service.getRecentStickers(apiKey, auth)
            if (recentsRes.isSuccessful) {
                cachedRecents = parseStickerResultList(recentsRes.body())
            }
            cacheInitialized = true
            
            // Also sync to shared preferences for offline mode
            saveToPrefs(context, KEY_FAVORITES, cachedFavorites)
            saveToPrefs(context, KEY_RECENTS, cachedRecents)
            saveToPrefs(context, "saved_stickers", cachedSaved)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing stickers from remote", e)
        }
    }
    
    private fun parseStickerResultList(body: List<Map<String, Any>>?): MutableList<StickerResult> {
        val list = mutableListOf<StickerResult>()
        body?.forEach { map ->
            val url = map["url"] as? String ?: map["sticker_url"] as? String
            val preview = map["preview"] as? String ?: map["preview_url"] as? String ?: url
            if (url != null) {
                list.add(StickerResult(url, preview!!))
            }
        }
        return list
    }

    suspend fun getRecentStickers(context: Context): List<StickerResult> {
        if (!cacheInitialized) {
            cachedRecents = getFromPrefs(context, KEY_RECENTS).toMutableList()
        }
        return cachedRecents
    }

    suspend fun addRecentSticker(context: Context, sticker: StickerResult) = withContext(Dispatchers.IO) {
        cachedRecents.removeAll { it.url == sticker.url }
        cachedRecents.add(0, sticker)
        if (cachedRecents.size > MAX_RECENTS) {
            cachedRecents.removeAt(cachedRecents.lastIndex)
        }
        saveToPrefs(context, KEY_RECENTS, cachedRecents)
        
        if (SupabaseClient.isConfigured) {
            try {
                val service = SupabaseClient.apiService ?: return@withContext
                val token = SupabaseClient.currentUser?.token ?: return@withContext
                val params = mapOf("sticker_url" to sticker.url, "preview_url" to sticker.preview)
                service.registerStickerUsage(SupabaseClient.supabaseAnonKey, "Bearer $token", params)
            } catch (e: Exception) {
                Log.e(TAG, "Error registering sticker usage", e)
            }
        }
    }

    suspend fun getFavoriteStickers(context: Context): List<StickerResult> {
        if (!cacheInitialized) {
            cachedFavorites = getFromPrefs(context, KEY_FAVORITES).toMutableList()
        }
        return cachedFavorites
    }

    suspend fun toggleFavoriteSticker(context: Context, sticker: StickerResult): Boolean = withContext(Dispatchers.IO) {
        val exists = cachedFavorites.find { it.url == sticker.url }
        val isFav: Boolean
        
        val service = SupabaseClient.apiService
        val token = SupabaseClient.currentUser?.token
        val apiKey = SupabaseClient.supabaseAnonKey
        
        if (exists != null) {
            cachedFavorites.remove(exists)
            isFav = false
            if (SupabaseClient.isConfigured && service != null && token != null) {
                try {
                    service.unfavoriteSticker(apiKey, "Bearer $token", mapOf("sticker_url" to sticker.url))
                } catch(e: Exception) {}
            }
        } else {
            cachedFavorites.add(0, sticker)
            isFav = true
            if (SupabaseClient.isConfigured && service != null && token != null) {
                try {
                    service.favoriteSticker(apiKey, "Bearer $token", mapOf("sticker_url" to sticker.url, "preview_url" to sticker.preview))
                } catch(e: Exception) {}
            }
        }
        saveToPrefs(context, KEY_FAVORITES, cachedFavorites)
        return@withContext isFav
    }

    fun isStickerFavorite(context: Context, stickerUrl: String): Boolean {
        if (!cacheInitialized) {
            cachedFavorites = getFromPrefs(context, KEY_FAVORITES).toMutableList()
        }
        return cachedFavorites.any { it.url == stickerUrl }
    }
    
    suspend fun saveSticker(context: Context, sticker: StickerResult) = withContext(Dispatchers.IO) {
        if (!cacheInitialized) {
            cachedSaved = getFromPrefs(context, "saved_stickers").toMutableList()
        }
        if (cachedSaved.none { it.url == sticker.url }) {
            cachedSaved.add(0, sticker)
            saveToPrefs(context, "saved_stickers", cachedSaved)
        }
        
        if (SupabaseClient.isConfigured) {
            try {
                val service = SupabaseClient.apiService ?: return@withContext
                val token = SupabaseClient.currentUser?.token ?: return@withContext
                val params = mapOf("sticker_url" to sticker.url, "preview_url" to sticker.preview)
                service.saveSticker(SupabaseClient.supabaseAnonKey, "Bearer $token", params)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving sticker", e)
            }
        }
    }
    
    suspend fun getSavedStickers(context: Context): List<StickerResult> {
        if (!cacheInitialized) {
            cachedSaved = getFromPrefs(context, "saved_stickers").toMutableList()
        }
        return cachedSaved
    }

    private fun saveToPrefs(context: Context, key: String, list: List<StickerResult>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(key, serializeStickerList(list)).apply()
    }
    
    private fun getFromPrefs(context: Context, key: String): List<StickerResult> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(key, null) ?: return emptyList()
        return deserializeStickerList(jsonStr)
    }

    private fun serializeStickerList(list: List<StickerResult>): String {
        return try {
            val array = org.json.JSONArray()
            for (sticker in list) {
                val obj = JSONObject()
                obj.put("url", sticker.url)
                obj.put("preview", sticker.preview)
                array.put(obj)
            }
            array.toString()
        } catch (e: Exception) {
            ""
        }
    }

    private fun deserializeStickerList(jsonStr: String): List<StickerResult> {
        val list = mutableListOf<StickerResult>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(StickerResult(url = obj.getString("url"), preview = obj.getString("preview")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize sticker list", e)
        }
        return list
    }
}
"""

with open('app/src/main/java/com/example/data/repository/StickerRepository.kt', 'w') as f:
    f.write(parts[0] + new_code)
