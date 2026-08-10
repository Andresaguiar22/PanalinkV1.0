package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.StickerResult
import com.example.data.model.SearchStickersRequest
import com.example.data.supabase.SessionManager
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

object StickerRepository {
    private const val TAG = "StickerRepository"

    // Store recent and favorite stickers in SharedPreferences locally for clean performance
    private const val PREFS_NAME = "sticker_prefs"
    private const val KEY_RECENTS = "recent_stickers"
    private const val KEY_FAVORITES = "favorite_stickers"
    private const val MAX_RECENTS = 24

    // In-memory cache for search queries
    private val searchCache = java.util.Collections.synchronizedMap(HashMap<String, List<StickerResult>>())

    suspend fun getStickers(context: Context, query: String?, limit: Int = 24): List<StickerResult> = withContext(Dispatchers.IO) {
        val cacheKey = query?.trim()?.lowercase() ?: ""
        if (searchCache.containsKey(cacheKey)) {
            Log.d(TAG, "Returning cached stickers for query: '$cacheKey'")
            return@withContext searchCache[cacheKey]!!
        }

        // 1. Try calling the Supabase Edge Function (Production secure approach)
        if (SupabaseClient.isConfigured) {
            try {
                val service = SupabaseClient.apiService
                if (service != null) {
                    SessionManager.validateAndRefreshSessionIfNeeded()
                    val auth = SupabaseClient.currentToken?.let { "Bearer $it" } ?: ""
                    
                    val requestBody = SearchStickersRequest(query = query ?: "", limit = limit)
                    
                    // Directly call Giphy API
                    val response = com.example.service.GiphyClient.apiService.searchStickers(
                        apiKey = "t8xzuvPe8NkWYQgPzfDSLa4RMoUVj5ai",
                        query = query ?: "",
                        limit = limit
                    )
                    
                    val fullUrl = response.raw().request.url.toString()
                    val httpMethod = response.raw().request.method
                    val reqHeaders = response.raw().request.headers.toString()
                    val bodySent = "{\"query\":\"${query ?: ""}\",\"limit\":$limit}"
                    val httpCode = response.code()
                    val respHeaders = response.headers().toString()
                    
                    Log.d(TAG, "=== STICKERS GIPHY API REQ ===")
                    Log.d(TAG, "URL completa: $fullUrl")
                    Log.d(TAG, "Método HTTP: $httpMethod")
                    Log.d(TAG, "Headers del Request:\n$reqHeaders")
                    Log.d(TAG, "Body enviado: $bodySent")
                    Log.d(TAG, "Código HTTP recibido: $httpCode")
                    Log.d(TAG, "Headers de la Respuesta:\n$respHeaders")
                    
                    if (response.isSuccessful) {
                        val searchResponse = response.body()
                        val results = searchResponse?.data?.map { sticker ->
                            StickerResult(
                                id = sticker.id,
                                url = sticker.images.fixedWidth.url,
                                preview = sticker.images.fixedWidth.url,
                                width = sticker.images.fixedWidth.width.toIntOrNull(),
                                height = sticker.images.fixedWidth.height.toIntOrNull()
                            )
                        } ?: emptyList()
                        
                        Log.d(TAG, "Cantidad de stickers recibidos: ${results.size}")
                        if (results.isNotEmpty()) {
                            val first = results.first()
                            Log.d(TAG, "Primer sticker recibido -> ID: ${first.id}, URL: ${first.url}")
                        } else {
                            Log.d(TAG, "No se recibieron stickers en la lista 'data'")
                        }
                        
                        searchCache[cacheKey] = results
                        return@withContext results
                    } else {
                        val errorBody = response.errorBody()?.string() ?: ""
                        Log.w(TAG, "Giphy API returned error code $httpCode")
                        Log.w(TAG, "Body completo de la respuesta (error): $errorBody")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Supabase Edge Function call failed, resorting to backup stickers", e)
            }
        }

        // 2. Last Resort Fallback: Static high-quality trending stickers if offline or APIs fail
        Log.w(TAG, "Supabase Edge Function failed or is not configured. Loading static backup stickers.")
        val backups = getBackupStickers(query)
        searchCache[cacheKey] = backups
        return@withContext backups
    }

    private fun getBackupStickers(query: String?): List<StickerResult> {
        val backups = listOf(
            StickerResult(
                url = "https://media1.giphy.com/media/v1.Y2lkPTc5MGI3NjExM2ZicTlycWRtOGFhNDdhaTMyMzA4ZXhndW1hcGswYmF0cjB5NTA4ZiZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/Lp71UIpGgajCbaSg48/giphy.gif",
                preview = "https://media1.giphy.com/media/v1.Y2lkPTc5MGI3NjExM2ZicTlycWRtOGFhNDdhaTMyMzA4ZXhndW1hcGswYmF0cjB5NTA4ZiZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/Lp71UIpGgajCbaSg48/giphy-preview.gif"
            ),
            StickerResult(
                url = "https://media2.giphy.com/media/v1.Y2lkPTc5MGI3NjExdzBpaDBlN254MWp6YWN3bHhndHhzNDJmOWlxcThicjhsOTNsM2ptdSZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/mGMcg3OovpWvD7A6bI/giphy.gif",
                preview = "https://media2.giphy.com/media/v1.Y2lkPTc5MGI3NjExdzBpaDBlN254MWp6YWN3bHhndHhzNDJmOWlxcThicjhsOTNsM2ptdSZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/mGMcg3OovpWvD7A6bI/giphy-preview.gif"
            ),
            StickerResult(
                url = "https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExOW11cGJ2MXNoMDZpZjhuMjVnaXhhajQyeWlyYWtsaHBsdXFobHFkOCZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/xTk9ZY0C9ZADnqiSE8/giphy.gif",
                preview = "https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExOW11cGJ2MXNoMDZpZjhuMjVnaXhhajQyeWlyYWtsaHBsdXFobHFkOCZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/xTk9ZY0C9ZADnqiSE8/giphy-preview.gif"
            ),
            StickerResult(
                url = "https://media0.giphy.com/media/v1.Y2lkPTc5MGI3NjExNXdycnI2azVldXUzNW85MTFpYnMydWZkcXRma3ExZ3g5MHphNWhsOSZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/bF55Zon7jVPDqE1pCO/giphy.gif",
                preview = "https://media0.giphy.com/media/v1.Y2lkPTc5MGI3NjExNXdycnI2azVldXUzNW85MTFpYnMydWZkcXRma3ExZ3g5MHphNWhsOSZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/bF55Zon7jVPDqE1pCO/giphy-preview.gif"
            ),
            StickerResult(
                url = "https://media4.giphy.com/media/v1.Y2lkPTc5MGI3NjExaTJvdHRzNmE3ZnYzdHFubms0N2lrdWhqenBrOGVzbjNldTcxOHFxbSZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/v9N2Sct8Gst8eI2Kby/giphy.gif",
                preview = "https://media4.giphy.com/media/v1.Y2lkPTc5MGI3NjExaTJvdHRzNmE3ZnYzdHFubms0N2lrdWhqenBrOGVzbjNldTcxOHFxbSZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/v9N2Sct8Gst8eI2Kby/giphy-preview.gif"
            ),
            StickerResult(
                url = "https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExa2k2bjBzNmR2dnlhcHptZ2Y2Znd5eDNndjJ0dWQzYWZlbm8xeHlhOCZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/yFQ0ywscgobJK/giphy.gif",
                preview = "https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExa2k2bjBzNmR2dnlhcHptZ2Y2Znd5eDNndjJ0dWQzYWZlbm8xeHlhOCZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/yFQ0ywscgobJK/giphy-preview.gif"
            ),
            StickerResult(
                url = "https://media2.giphy.com/media/v1.Y2lkPTc5MGI3NjExdGs3aGxrdmRzdmJtdmMyczZmdWphcHhhbndrcDhrYzN2ZjdycHdyOCZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/P31RoFejRclZkODN9m/giphy.gif",
                preview = "https://media2.giphy.com/media/v1.Y2lkPTc5MGI3NjExdGs3aGxrdmRzdmJtdmMyczZmdWphcHhhbndrcDhrYzN2ZjdycHdyOCZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/P31RoFejRclZkODN9m/giphy-preview.gif"
            ),
            StickerResult(
                url = "https://media4.giphy.com/media/v1.Y2lkPTc5MGI3NjExZnpjdGsxbHh3NXR4MWhqMzZibms4MGpxOXVpODZ0cThtcnkxaTUzNCZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/L3Z6YyvcoVn0hO07b7/giphy.gif",
                preview = "https://media4.giphy.com/media/v1.Y2lkPTc5MGI3NjExZnpjdGsxbHh3NXR4MWhqMzZibms4MGpxOXVpODZ0cThtcnkxaTUzNCZlcD12MV9zdGlja2Vyc19zZWFyY2gmY3Q9cw/L3Z6YyvcoVn0hO07b7/giphy-preview.gif"
            )
        )
        if (query.isNullOrEmpty()) return backups
        return backups.filter { 
            it.url.contains(query, ignoreCase = true) || query.contains("cat", ignoreCase = true) || query.contains("love", ignoreCase = true)
        }.ifEmpty { backups }
    }

    // --- RPC Helpers for Recents, Favorites & Saved ---
    
    // In-memory cache to prevent constant fetching
    private var cachedSaved = mutableListOf<StickerResult>()
    private var cachedFavorites = mutableListOf<StickerResult>()
    private var cachedRecents = mutableListOf<StickerResult>()
    private var cacheInitialized = false

    suspend fun syncStickersFromRemote(context: Context) = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) return@withContext
        try {
            val service = SupabaseClient.apiService ?: return@withContext
            val token = SupabaseClient.currentToken ?: return@withContext
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
                list.add(StickerResult(url = url, preview = preview!!))
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
                val token = SupabaseClient.currentToken ?: return@withContext
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
        val token = SupabaseClient.currentToken
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
                val token = SupabaseClient.currentToken ?: return@withContext
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
