package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object CdnManager {
    private const val TAG = "CdnManager"
    private const val PREFS_NAME = "panalink_cdn_prefs"
    private const val KEY_CACHED_CDN_URL = "cached_cdn_url"

    @Volatile
    private var cachedCdnUrl: String? = null

    private var context: Context? = null
    private val cdnMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isListenerStarted = AtomicBoolean(false)

    fun init(appContext: Context) {
        if (context != null) return
        val ctx = appContext.applicationContext
        context = ctx
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val stored = prefs.getString(KEY_CACHED_CDN_URL, null)
            if (!stored.isNullOrEmpty()) {
                cachedCdnUrl = stored.trim().removeSuffix("/")
                Log.i(TAG, "Restored cached CDN URL from SharedPreferences: '$cachedCdnUrl'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring CDN URL from SharedPreferences", e)
        }
    }

    private fun startRealtimeListener() {
        if (isListenerStarted.compareAndSet(false, true)) {
            scope.launch {
                SupabaseClient.globalServerConfigUpdates.collect { newUrl ->
                    if (!newUrl.isNullOrBlank()) {
                        val clean = newUrl.trim().removeSuffix("/")
                        Log.i(TAG, "🟢 URL del CDN actualizada por Realtime: '$clean'")
                        cachedCdnUrl = clean
                        saveToPrefs(clean)
                    }
                }
            }
        }
    }

    private fun saveToPrefs(cdnUrl: String) {
        try {
            context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_CACHED_CDN_URL, cdnUrl)
                ?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving CDN URL to SharedPreferences", e)
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Checks if the target CDN URL is reachable via a lightweight HEAD or GET request.
     */
    private fun isCdnReachable(cdnUrl: String): Boolean {
        if (cdnUrl.isBlank() || !cdnUrl.startsWith("http")) return false
        val testUrl = "$cdnUrl/health"
        return try {
            val request = Request.Builder()
                .url(testUrl)
                .head()
                .build()
            client.newCall(request).execute().use { response ->
                val code = response.code
                val reachable = code in 200..404
                if (!reachable) {
                    Log.w(TAG, "CDN_UNREACHABLE: Health check returned HTTP $code for $testUrl")
                }
                reachable
            }
        } catch (e: Exception) {
            Log.w(TAG, "CDN_UNREACHABLE: Failed to reach $testUrl - ${e.message}")
            false
        }
    }

    /**
     * Obtains the active CDN URL by querying Supabase table 'global_server_config' for id=1.
     * Uses Mutex to guarantee single-flight execution across concurrent calls.
     */
    suspend fun getCDNUrl(forceRefresh: Boolean = false): String = cdnMutex.withLock {
        startRealtimeListener()
        if (!forceRefresh) {
            val cached = cachedCdnUrl
            if (!cached.isNullOrEmpty()) {
                Log.d(TAG, "Utilizando URL del CDN en caché: $cached")
                return@withLock cached
            }
        } else {
            Log.d(TAG, "Petición de refresco forzado del CDN. Ignorando caché.")
        }

        val supabaseUrl = SupabaseClient.supabaseUrl.trim().removeSuffix("/")
        val supabaseAnonKey = SupabaseClient.supabaseAnonKey
        val endpoint = "$supabaseUrl/rest/v1/global_server_config?id=eq.1&select=*"
        
        Log.d(TAG, "=== REGISTRO DE CONSULTA CDN DESDE SUPABASE ===")
        Log.d(TAG, "Consultando estado del CDN en Supabase: $endpoint")

        var attempts = 3
        while (attempts > 0) {
            try {
                val token = SupabaseClient.currentToken ?: supabaseAnonKey
                val bearer = "Bearer $token"

                val request = Request.Builder()
                    .url(endpoint)
                    .header("apikey", supabaseAnonKey)
                    .header("Authorization", bearer)
                    .header("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseCode = response.code
                    val bodyStr = response.body?.string()?.trim() ?: ""

                    if (response.isSuccessful && bodyStr.isNotEmpty()) {
                        if (bodyStr.startsWith("<")) {
                            Log.e(TAG, "🚨 Se recibió una respuesta HTML no-JSON de Supabase: ${bodyStr.take(200)}")
                        } else {
                            try {
                                val jsonObject = if (bodyStr.startsWith("[")) {
                                    val jsonArray = org.json.JSONArray(bodyStr)
                                    if (jsonArray.length() > 0) {
                                        jsonArray.getJSONObject(0)
                                    } else {
                                        null
                                    }
                                } else if (bodyStr.startsWith("{")) {
                                    JSONObject(bodyStr)
                                } else {
                                    null
                                }

                                if (jsonObject != null) {
                                    val active = jsonObject.optBoolean("active", false)
                                    val cdnUrl = jsonObject.optString("cdn_url", "").trim().removeSuffix("/")

                                    if (!active) {
                                        Log.e(TAG, "🚨 CDN_UNREACHABLE: El CDN en global_server_config no está activo ('active' es false)")
                                    } else if (cdnUrl.isEmpty() || !cdnUrl.startsWith("http")) {
                                        Log.e(TAG, "🚨 CDN_UNREACHABLE: URL inválida en Supabase: '$cdnUrl'")
                                    } else {
                                        val reachable = isCdnReachable(cdnUrl)
                                        if (reachable) {
                                            Log.i(TAG, "🟢 URL del CDN verificada exitosamente: '$cdnUrl'")
                                            cachedCdnUrl = cdnUrl
                                            saveToPrefs(cdnUrl)
                                            return@withLock cdnUrl
                                        } else {
                                            Log.w(TAG, "CDN_UNREACHABLE: URL '$cdnUrl' configurada en Supabase no respondió a health check")
                                            if (!cachedCdnUrl.isNullOrEmpty()) {
                                                Log.i(TAG, "🟢 Conservando URL anterior funcional: '$cachedCdnUrl'")
                                                return@withLock cachedCdnUrl!!
                                            } else {
                                                Log.w(TAG, "No hay URL anterior funcional. Devolviendo '$cdnUrl' como último recurso sin guardar.")
                                                return@withLock cdnUrl
                                            }
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "No se encontró ningún registro en global_server_config para id=1")
                                }
                            } catch (je: Exception) {
                                Log.e(TAG, "🚨 Error parseando respuesta JSON de Supabase: '$bodyStr'", je)
                            }
                        }
                    } else {
                        Log.w(TAG, "Respuesta no exitosa de Supabase en $endpoint: Código HTTP $responseCode")
                    }
                }
            } catch (ioe: java.io.IOException) {
                Log.e(TAG, "🚨 Error de red consultando CDN en Supabase: ${ioe.message}", ioe)
            } catch (t: Throwable) {
                Log.e(TAG, "🚨 Error fatal consultando CDN en Supabase: ${t.message}", t)
            }

            attempts--
            if (attempts > 0) {
                try {
                    kotlinx.coroutines.delay(1000)
                } catch (_: Exception) {}
            }
        }

        Log.w(TAG, "CDN_UNREACHABLE: No se pudo obtener el CDN desde Supabase después de varios intentos.")
        val fallback = cachedCdnUrl ?: ""
        fallback
    }

    /**
     * Force refresh the cached CDN URL on demand.
     */
    fun clearCache() {
        cachedCdnUrl = null
    }

    /**
     * Centralized function to resolve avatar URLs into valid absolute HTTPS/CDN/Local URLs.
     */
    fun resolveAvatarUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim()
        if (trimmed.isNullOrEmpty() || 
            trimmed.equals("null", ignoreCase = true) || 
            trimmed.equals("undefined", ignoreCase = true)
        ) {
            return null
        }

        if (trimmed.startsWith("content://") || 
            trimmed.startsWith("file://") || 
            trimmed.startsWith("android.resource://") ||
            trimmed.startsWith("preset:")
        ) {
            return trimmed
        }

        val baseSupabaseUrl = SupabaseClient.supabaseUrl.trim().removeSuffix("/")
        val baseSupabaseStorage = "$baseSupabaseUrl/storage/v1/object/public"

        val supabaseHost = try { URI(SupabaseClient.supabaseUrl).host ?: "supabase.co" } catch (e: Exception) { "supabase.co" }

        val absoluteUrl = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
                if (trimmed.contains(supabaseHost) && trimmed.startsWith("http://")) {
                    trimmed.replace("http://", "https://")
                } else {
                    trimmed
                }
            }
            trimmed.startsWith("/storage/v1/object/public/") -> {
                "$baseSupabaseUrl$trimmed"
            }
            trimmed.startsWith("storage/v1/object/public/") -> {
                "$baseSupabaseUrl/$trimmed"
            }
            trimmed.startsWith("avatars/") || trimmed.startsWith("/avatars/") -> {
                val cleanPath = trimmed.removePrefix("/")
                "$baseSupabaseStorage/$cleanPath"
            }
            else -> {
                val cleanPath = trimmed.removePrefix("/")
                "$baseSupabaseStorage/avatars/$cleanPath"
            }
        }

        val resolved = resolveMediaUrlSync(absoluteUrl)
        return resolved.ifEmpty { null }
    }

    fun isCdnRelated(originalUrl: String): Boolean {
        return (originalUrl.contains("bore.pub") || 
                originalUrl.contains("trycloudflare") || 
                originalUrl.contains("10.0.2.2") || 
                originalUrl.contains("localhost") || 
                originalUrl.contains("/video/") || 
                originalUrl.contains("/files/") ||
                originalUrl.contains("/documents/") ||
                originalUrl.contains("/uploads/") ||
                originalUrl.contains("/images/") ||
                originalUrl.contains("/avatars/") ||
                originalUrl.contains("/audios/")) &&
                !originalUrl.contains(try { URI(SupabaseClient.supabaseUrl).host ?: "supabase.co" } catch (e: Exception) { "supabase.co" })
    }

    /**
     * Synchronous version of resolveMediaUrl utilizing the cached CDN URL.
     * Prevents blockages or having to launch coroutines in UI rendering components.
     */
    fun resolveMediaUrlSync(originalUrl: String?): String {
        if (originalUrl.isNullOrEmpty()) return ""
        
        // Do not resolve local URIs or absolute file system paths
        if (originalUrl.startsWith("content://") || originalUrl.startsWith("file://") || originalUrl.startsWith("android.resource://") || originalUrl.startsWith("/")) {
            return originalUrl
        }

        var activeCdnBase = (cachedCdnUrl ?: "").trim().removeSuffix("/")
        if (activeCdnBase.isEmpty() && context != null) {
            try {
                val prefs = context!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                activeCdnBase = (prefs.getString(KEY_CACHED_CDN_URL, "") ?: "").trim().removeSuffix("/")
                if (activeCdnBase.isNotEmpty()) {
                    cachedCdnUrl = activeCdnBase
                }
            } catch (_: Exception) {}
        }

        if (isCdnRelated(originalUrl)) {
            if (activeCdnBase.isEmpty()) {
                Log.w(TAG, "Active CDN cache is empty, returning original URL: $originalUrl")
                return originalUrl
            }

            val path = when {
                originalUrl.contains("/video/") -> {
                    val index = originalUrl.indexOf("/video/")
                    originalUrl.substring(index)
                }
                originalUrl.contains("/files/") -> {
                    val index = originalUrl.indexOf("/files/")
                    originalUrl.substring(index)
                }
                originalUrl.contains("/documents/") -> {
                    val index = originalUrl.indexOf("/documents/")
                    originalUrl.substring(index)
                }
                originalUrl.contains("/uploads/") -> {
                    val index = originalUrl.indexOf("/uploads/")
                    originalUrl.substring(index)
                }
                originalUrl.contains("/images/") -> {
                    val index = originalUrl.indexOf("/images/")
                    originalUrl.substring(index)
                }
                originalUrl.contains("/avatars/") -> {
                    val index = originalUrl.indexOf("/avatars/")
                    originalUrl.substring(index)
                }
                originalUrl.contains("/audios/") -> {
                    val index = originalUrl.indexOf("/audios/")
                    originalUrl.substring(index)
                }
                else -> {
                    try {
                        val uri = URI(originalUrl)
                        val pathWithQuery = uri.path + (uri.query?.let { "?$it" } ?: "")
                        pathWithQuery
                    } catch (e: Exception) {
                        originalUrl
                    }
                }
            }

            val resolvedUrl = if (path.startsWith("/")) "$activeCdnBase$path" else "$activeCdnBase/$path"
            Log.d(TAG, "Resolved CDN URL (Sync): $originalUrl ➡️ $resolvedUrl")
            return resolvedUrl
        }

        return originalUrl
    }

    /**
     * Dynamically rewrites any old, hardcoded, or expired tunnel/localhost media URLs 
     * to use the currently active dynamic CDN URL retrieved from the backend.
     */
    suspend fun resolveMediaUrl(originalUrl: String?): String {
        if (originalUrl.isNullOrEmpty()) return ""
        
        // Do not resolve local URIs or absolute file system paths
        if (originalUrl.startsWith("content://") || originalUrl.startsWith("file://") || originalUrl.startsWith("android.resource://") || originalUrl.startsWith("/")) {
            return originalUrl
        }
        
        // Ensure cache is loaded if we have a suspend context
        val activeCdnBase = getCDNUrl().trim().removeSuffix("/")
        if (activeCdnBase.isEmpty()) {
            Log.w(TAG, "Active CDN base is empty, returning original URL: $originalUrl")
            return originalUrl
        }

        if (isCdnRelated(originalUrl)) {
            val path = when {
                originalUrl.contains("/video/") -> {
                    val index = originalUrl.indexOf("/video/")
                    originalUrl.substring(index)
                }
                originalUrl.contains("/files/") -> {
                    val index = originalUrl.indexOf("/files/")
                    originalUrl.substring(index)
                }
                originalUrl.contains("/documents/") -> {
                    val index = originalUrl.indexOf("/documents/")
                    originalUrl.substring(index)
                }
                originalUrl.contains("/uploads/") -> {
                    val index = originalUrl.indexOf("/uploads/")
                    originalUrl.substring(index)
                }
                originalUrl.contains("/images/") -> {
                    val index = originalUrl.indexOf("/images/")
                    originalUrl.substring(index)
                }
                originalUrl.contains("/avatars/") -> {
                    val index = originalUrl.indexOf("/avatars/")
                    originalUrl.substring(index)
                }
                originalUrl.contains("/audios/") -> {
                    val index = originalUrl.indexOf("/audios/")
                    originalUrl.substring(index)
                }
                else -> {
                    try {
                        val uri = URI(originalUrl)
                        val pathWithQuery = uri.path + (uri.query?.let { "?$it" } ?: "")
                        pathWithQuery
                    } catch (e: Exception) {
                        originalUrl
                    }
                }
            }

            val resolvedUrl = if (path.startsWith("/")) "$activeCdnBase$path" else "$activeCdnBase/$path"
            Log.d(TAG, "Resolved CDN URL (Suspend): $originalUrl ➡️ $resolvedUrl")
            return resolvedUrl
        }

        return originalUrl
    }
}
