package com.example.data.repository

import android.content.Context
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

    private val MEDIA_PATH_MARKERS = listOf(
        "/video/", "/files/", "/documents/", "/uploads/",
        "/images/", "/avatars/", "/audios/"
    )

    @Volatile private var cachedCdnUrl: String? = null
    private var context: Context? = null
    private val cdnMutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isListenerStarted = AtomicBoolean(false)
    private val isWarmingCdn = AtomicBoolean(false)

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

    private fun isLocalHost(host: String): Boolean =
        host == "localhost" || host == "127.0.0.1" || host == "10.0.2.2"

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
                response.code in 200..299
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
     * CDN-related when the URL points to the active CDN host, a known local
     * tunnel host, or any host serving a known media folder (legacy/dead CDN
     * hosts included, so they get re-anchored to the active CDN). The Supabase
     * origin is never rewritten: its Storage URLs are always reachable, so
     * touching them would only risk loops and 404s on the CDN.
     */
    fun isCdnRelated(originalUrl: String): Boolean {
        if (originalUrl.isBlank()) return false
        if (originalUrl.startsWith("content://") || originalUrl.startsWith("file://") ||
            originalUrl.startsWith("android.resource://") || originalUrl.startsWith("/")) return false

        val uri = try { URI(originalUrl) } catch (_: Exception) { return false }
        val host = uri.host?.lowercase() ?: return false
        val path = uri.path.orEmpty().lowercase()
        val supabaseHost = try { URI(SupabaseClient.supabaseUrl).host?.lowercase() } catch (_: Exception) { null }

        if (!supabaseHost.isNullOrBlank() && host == supabaseHost) return false

        val activeHost = try { URI(cachedCdnUrl.orEmpty()).host?.lowercase() } catch (_: Exception) { null }
        if (!activeHost.isNullOrBlank() && host == activeHost) return true

        if (isLocalHost(host) ||
            host == "bore.pub" || host.endsWith(".bore.pub") ||
            host == "trycloudflare.com" || host.endsWith(".trycloudflare.com")) return true

        return MEDIA_PATH_MARKERS.any { path.contains(it) }
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

    /**
     * Re-anchors [originalUrl] onto the active CDN base preserving its full
     * relative path and query: http://dead-host/images/a.png becomes
     * <cdn-base>/images/a.png. URLs already on the CDN host are returned as-is.
     */
    private fun reconstructCdnUrl(originalUrl: String, activeCdnBase: String): String {
        val normalizedBase = cleanBase(activeCdnBase)
        if (normalizedBase.isBlank()) return originalUrl

        val uri = try { URI(originalUrl) } catch (_: Exception) { return originalUrl }
        val path = uri.rawPath.orEmpty()
        if (path.isBlank()) return originalUrl

        val baseHost = try { URI(normalizedBase).host?.lowercase() } catch (_: Exception) { null }
        if (!baseHost.isNullOrBlank() && uri.host?.lowercase() == baseHost) return originalUrl

        return buildString {
            append(normalizedBase)
            append(path)
            if (!uri.rawQuery.isNullOrBlank()) {
                append('?')
                append(uri.rawQuery)
            }
        }
    }

    fun resolveMediaUrlSync(originalUrl: String?): String {
        val raw = originalUrl?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        if (raw.startsWith("content://") || raw.startsWith("file://") ||
            raw.startsWith("android.resource://") || raw.startsWith("/")) return raw
        if (!isCdnRelated(raw)) return raw

        val base = currentCachedCdnBase()
        if (base.isBlank()) {
            // Cold start race: don't fail silently, warm the cache so the next
            // load resolves through the active CDN.
            if (context != null && isWarmingCdn.compareAndSet(false, true)) {
                scope.launch {
                    try {
                        getCDNUrl()
                    } catch (_: Exception) {
                    } finally {
                        isWarmingCdn.set(false)
                    }
                }
            }
            return raw
        }
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

        // Supabase Storage URLs are excluded from CDN rewriting, so avatars
        // load straight from Supabase; only tunnel/legacy hosts get re-anchored.
        return resolveMediaUrlSync(absolute).ifEmpty { null }
    }
}
