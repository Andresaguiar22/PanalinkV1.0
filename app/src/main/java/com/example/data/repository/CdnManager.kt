package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object CdnManager {
    private const val TAG = "CdnManager"
    private const val PREFS_NAME = "panalink_cdn_prefs"
    private const val KEY_CACHED_CDN_URL = "cached_cdn_url"

    @Volatile private var cachedCdnUrl: String? = null
    private var context: Context? = null
    private val cdnMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isListenerStarted = AtomicBoolean(false)

    fun init(appContext: Context) {
        if (context != null) return
        val ctx = appContext.applicationContext
        context = ctx
        try {
            val stored = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CACHED_CDN_URL, null)
            if (!stored.isNullOrBlank()) cachedCdnUrl = cleanBase(stored)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring CDN URL", e)
        }
    }

    private fun cleanBase(url: String): String = url.trim().removeSuffix("/")

    private fun startRealtimeListener() {
        if (isListenerStarted.compareAndSet(false, true)) {
            scope.launch {
                SupabaseClient.globalServerConfigUpdates.collect { newUrl ->
                    if (!newUrl.isNullOrBlank()) {
                        val clean = cleanBase(newUrl)
                        if (isValidHttpUrl(clean)) {
                            cachedCdnUrl = clean
                            saveToPrefs(clean)
                            Log.i(TAG, "CDN URL actualizada por Realtime: '$clean'")
                        }
                    }
                }
            }
        }
    }

    private fun saveToPrefs(cdnUrl: String) {
        try {
            context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()?.putString(KEY_CACHED_CDN_URL, cdnUrl)?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving CDN URL", e)
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    private fun isValidHttpUrl(url: String): Boolean =
        try { URI(url).let { it.scheme == "http" || it.scheme == "https" } } catch (_: Exception) { false }

    private fun isCdnReachable(cdnUrl: String): Boolean {
        if (!isValidHttpUrl(cdnUrl)) return false
        return try {
            val request = Request.Builder().url("$cdnUrl/health").head().build()
            client.newCall(request).execute().use { response ->
                response.code in 200..404
            }
        } catch (e: Exception) {
            Log.w(TAG, "CDN health check failed: ${e.message}")
            false
        }
    }

    suspend fun getCDNUrl(forceRefresh: Boolean = false): String = cdnMutex.withLock {
        startRealtimeListener()
        if (!forceRefresh) cachedCdnUrl?.takeIf { it.isNotBlank() }?.let { return@withLock it }

        val supabaseUrl = cleanBase(SupabaseClient.supabaseUrl)
        val anonKey = SupabaseClient.supabaseAnonKey
        val endpoint = "$supabaseUrl/rest/v1/global_server_config?id=eq.1&select=*"

        var attempts = 3
        while (attempts-- > 0) {
            try {
                val token = SupabaseClient.currentToken ?: anonKey
                val request = Request.Builder()
                    .url(endpoint)
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()?.trim().orEmpty()
                    if (response.isSuccessful && body.isNotEmpty() && !body.startsWith("<")) {
                        val json = try {
                            if (body.startsWith("[")) {
                                JSONArray(body).takeIf { it.length() > 0 }?.getJSONObject(0)
                            } else if (body.startsWith("{")) JSONObject(body) else null
                        } catch (e: Exception) {
                            Log.e(TAG, "Invalid CDN config JSON", e)
                            null
                        }
                        val active = json?.optBoolean("active", false) ?: false
                        val cdn = cleanBase(json?.optString("cdn_url", "").orEmpty())
                        if (active && isValidHttpUrl(cdn)) {
                            if (!isCdnReachable(cdn)) {
                                Log.w(TAG, "CDN health check failed; keeping configured URL: $cdn")
                            }
                            cachedCdnUrl = cdn
                            saveToPrefs(cdn)
                            return@withLock cdn
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error obtaining CDN URL: ${e.message}")
            }
            if (attempts > 0) delay(1000)
        }

        return@withLock cachedCdnUrl.orEmpty()
    }

    fun clearCache() {
        cachedCdnUrl = null
    }

    /**
     * True only for known CDN hosts or the currently active CDN host.
     * URL path names alone never make an URL CDN-related, preventing
     * accidental rewriting of Supabase Storage and third-party URLs.
     */
    fun isCdnRelated(originalUrl: String): Boolean {
        if (originalUrl.isBlank()) return false
        if (originalUrl.startsWith("content://") || originalUrl.startsWith("file://") ||
            originalUrl.startsWith("android.resource://") || originalUrl.startsWith("/")) return false

        val host = try { URI(originalUrl).host?.lowercase() } catch (_: Exception) { null } ?: return false
        val supabaseHost = try { URI(SupabaseClient.supabaseUrl).host?.lowercase() } catch (_: Exception) { null }
        if (!supabaseHost.isNullOrBlank() && host == supabaseHost) return false

        val activeHost = try { URI(cachedCdnUrl.orEmpty()).host?.lowercase() } catch (_: Exception) { null }
        if (!activeHost.isNullOrBlank() && host == activeHost) return true

        return host == "localhost" || host == "10.0.2.2" ||
            host == "bore.pub" || host.endsWith(".bore.pub") ||
            host == "trycloudflare.com" || host.endsWith(".trycloudflare.com")
    }

    private fun currentCachedCdnBase(): String {
        var base = cleanBase(cachedCdnUrl.orEmpty())
        if (base.isBlank()) {
            try {
                base = cleanBase(context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    ?.getString(KEY_CACHED_CDN_URL, "").orEmpty())
                if (base.isNotBlank()) cachedCdnUrl = base
            } catch (_: Exception) {}
        }
        return base
    }

    private fun reconstructCdnUrl(originalUrl: String, activeCdnBase: String): String {
        val normalizedBase = cleanBase(activeCdnBase)
        val currentPrefix = "$normalizedBase/video/"
        if (originalUrl.startsWith(currentPrefix)) return originalUrl

        val filename = try {
            val path = URI(originalUrl).path.orEmpty()
            path.substringAfterLast('/').takeIf { it.isNotBlank() }.orEmpty()
        } catch (_: Exception) {
            originalUrl.substringAfterLast('/').substringBefore('?')
        }

        if (filename.isBlank()) return originalUrl
        return "$normalizedBase/video/${Uri.encode(filename)}"
    }

    fun resolveMediaUrlSync(originalUrl: String?): String {
        val raw = originalUrl?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        if (raw.startsWith("content://") || raw.startsWith("file://") ||
            raw.startsWith("android.resource://") || raw.startsWith("/")) return raw
        if (!isCdnRelated(raw)) return raw

        val base = currentCachedCdnBase()
        if (base.isBlank()) return raw
        return reconstructCdnUrl(raw, base)
    }

    suspend fun resolveMediaUrl(originalUrl: String?): String {
        val raw = originalUrl?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        if (raw.startsWith("content://") || raw.startsWith("file://") ||
            raw.startsWith("android.resource://") || raw.startsWith("/")) return raw

        val base = getCDNUrl()
        if (base.isBlank() || !isCdnRelated(raw)) return raw
        return reconstructCdnUrl(raw, base)
    }

    fun resolveAvatarUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim()
        if (trimmed.isNullOrEmpty() || trimmed.equals("null", true) || trimmed.equals("undefined", true)) return null
        if (trimmed.startsWith("content://") || trimmed.startsWith("file://") ||
            trimmed.startsWith("android.resource://") || trimmed.startsWith("preset:")) return trimmed

        val supabaseBase = cleanBase(SupabaseClient.supabaseUrl)
        val storageBase = "$supabaseBase/storage/v1/object/public"
        val absolute = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
                if (trimmed.startsWith("http://") && trimmed.contains(try { URI(supabaseBase).host.orEmpty() } catch (_: Exception) { "" })) {
                    trimmed.replaceFirst("http://", "https://")
                } else trimmed
            }
            trimmed.startsWith("/storage/v1/object/public/") -> "$supabaseBase$trimmed"
            trimmed.startsWith("storage/v1/object/public/") -> "$supabaseBase/$trimmed"
            trimmed.startsWith("avatars/") || trimmed.startsWith("/avatars/") -> "$storageBase/${trimmed.removePrefix("/")}"
            else -> "$storageBase/avatars/${trimmed.removePrefix("/")}"
        }
        return resolveMediaUrlSync(absolute).ifEmpty { null }
    }
}
