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

/**
 * Single source of truth for Panalink's dynamic media CDN.
 *
 * Supabase is the source of truth, but a candidate is promoted only after a
 * successful health check. A failed refresh never destroys the last known-good
 * CDN, which is essential because the CDN endpoint may change dynamically.
 *
 * URL classification is based on origin/host, never on media directory names.
 * This prevents a new server path such as /media/reels/ from bypassing the
 * resolver simply because it does not contain /video/, /images/, etc.
 */
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
    private val isStartupRefreshStarted = AtomicBoolean(false)

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(7, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun init(appContext: Context) {
        if (context != null) return
        context = appContext.applicationContext
        try {
            val stored = context!!.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CACHED_CDN_URL, null)
                ?.trim()
                ?.removeSuffix("/")

            if (!stored.isNullOrEmpty() && isValidCdnBase(stored)) {
                cachedCdnUrl = stored
                Log.i(TAG, "Restored cached CDN URL: $stored")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring CDN URL", e)
        }

        if (isStartupRefreshStarted.compareAndSet(false, true)) {
            scope.launch {
                delay(500)
                runCatching { getCDNUrl(forceRefresh = true) }
                    .onFailure { Log.w(TAG, "Startup CDN refresh failed: ${it.message}") }
            }
        }
    }

    private fun normalizeBase(url: String): String = url.trim().removeSuffix("/")

    private fun isValidCdnBase(url: String): Boolean {
        return try {
            val uri = URI(normalizeBase(url))
            (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
        } catch (_: Exception) {
            false
        }
    }

    private fun saveToPrefs(cdnUrl: String) {
        try {
            context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()
                ?.putString(KEY_CACHED_CDN_URL, cdnUrl)
                ?.apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving CDN URL", e)
        }
    }

    /** /health must return an actual successful 2xx response. */
    private fun isCdnReachable(cdnUrl: String): Boolean {
        val base = normalizeBase(cdnUrl)
        if (!isValidCdnBase(base)) return false

        val healthUrl = "$base/health"
        return try {
            val request = Request.Builder()
                .url(healthUrl)
                .get()
                .header("Cache-Control", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                val ok = response.code in 200..299
                if (ok) {
                    Log.i(TAG, "CDN health OK: $healthUrl (${response.code})")
                } else {
                    Log.w(TAG, "CDN health FAILED: $healthUrl (${response.code})")
                }
                ok
            }
        } catch (e: Exception) {
            Log.w(TAG, "CDN health FAILED: $healthUrl - ${e.message}")
            false
        }
    }

    /** Realtime is advisory; never promote an unvalidated URL. */
    private fun startRealtimeListener() {
        if (!isListenerStarted.compareAndSet(false, true)) return

        scope.launch {
            try {
                SupabaseClient.globalServerConfigUpdates.collect { newUrl ->
                    val candidate = newUrl?.trim()?.removeSuffix("/")
                    if (candidate.isNullOrBlank() || !isValidCdnBase(candidate)) {
                        Log.w(TAG, "Ignoring invalid CDN URL from Realtime: '$newUrl'")
                        return@collect
                    }

                    if (isCdnReachable(candidate)) {
                        promoteCdn(candidate, "Realtime")
                    } else {
                        Log.w(TAG, "Ignoring unreachable CDN from Realtime; preserving current CDN")
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Dynamic CDN Realtime listener stopped unexpectedly", t)
                isListenerStarted.set(false)
            }
        }
    }

    private fun promoteCdn(candidate: String, source: String) {
        val clean = normalizeBase(candidate)
        val previous = cachedCdnUrl
        if (previous == clean) return

        cachedCdnUrl = clean
        saveToPrefs(clean)
        Log.i(TAG, "CDN promoted from $source: $clean")
    }

    suspend fun getCDNUrl(forceRefresh: Boolean = false): String = cdnMutex.withLock {
        startRealtimeListener()

        if (!forceRefresh) {
            cachedCdnUrl?.takeIf { isValidCdnBase(it) }?.let {
                return@withLock it
            }
        }

        val supabaseUrl = SupabaseClient.supabaseUrl.trim().removeSuffix("/")
        val anonKey = SupabaseClient.supabaseAnonKey
        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            Log.e(TAG, "Supabase configuration is incomplete; preserving cached CDN")
            return@withLock cachedCdnUrl.orEmpty()
        }

        val endpoint = "$supabaseUrl/rest/v1/global_server_config?id=eq.1&select=active,cdn_url"
        var attempts = 3

        while (attempts-- > 0) {
            try {
                val token = SupabaseClient.currentToken?.takeIf { it.isNotBlank() } ?: anonKey
                val request = Request.Builder()
                    .url(endpoint)
                    .header("apikey", anonKey)
                    .header("Authorization", "Bearer $token")
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()?.trim().orEmpty()
                    if (!response.isSuccessful || body.isEmpty()) {
                        Log.w(TAG, "Supabase CDN config failed: HTTP ${response.code}")
                    } else {
                        val json = when {
                            body.startsWith("[") -> JSONArray(body).let { if (it.length() > 0) it.getJSONObject(0) else null }
                            body.startsWith("{") -> JSONObject(body)
                            else -> null
                        }

                        if (json == null) {
                            Log.w(TAG, "Supabase CDN config returned invalid JSON")
                        } else {
                            val active = json.optBoolean("active", false)
                            val candidate = normalizeBase(json.optString("cdn_url", ""))

                            if (!active) {
                                Log.w(TAG, "Supabase reports CDN inactive; preserving known-good CDN")
                            } else if (!isValidCdnBase(candidate)) {
                                Log.w(TAG, "Supabase returned invalid CDN URL: '$candidate'")
                            } else if (isCdnReachable(candidate)) {
                                promoteCdn(candidate, "Supabase")
                                return@withLock candidate
                            } else {
                                Log.w(TAG, "Supabase CDN candidate is unreachable; preserving known-good CDN")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error refreshing CDN from Supabase: ${e.message}")
            }

            if (attempts > 0) delay(1000)
        }

        return@withLock cachedCdnUrl.orEmpty()
    }

    fun clearCache() {
        cachedCdnUrl = null
        try {
            context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()?.remove(KEY_CACHED_CDN_URL)?.apply()
        } catch (_: Exception) { }
    }

    fun resolveAvatarUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim()
        if (trimmed.isNullOrEmpty() || trimmed.equals("null", true) || trimmed.equals("undefined", true)) return null

        if (trimmed.startsWith("content://") || trimmed.startsWith("file://") ||
            trimmed.startsWith("android.resource://") || trimmed.startsWith("preset:")) return trimmed

        val supabase = SupabaseClient.supabaseUrl.trim().removeSuffix("/")
        val supabaseHost = try { URI(supabase).host.orEmpty() } catch (_: Exception) { "" }
        val storage = "$supabase/storage/v1/object/public"
        val absolute = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
                try {
                    val host = URI(trimmed).host.orEmpty()
                    if (trimmed.startsWith("http://") && supabaseHost.isNotBlank() && host.equals(supabaseHost, true)) {
                        trimmed.replaceFirst("http://", "https://")
                    } else {
                        trimmed
                    }
                } catch (_: Exception) {
                    trimmed
                }
            }
            trimmed.startsWith("/storage/v1/object/public/") -> "$supabase$trimmed"
            trimmed.startsWith("storage/v1/object/public/") -> "$supabase/$trimmed"
            trimmed.startsWith("avatars/") || trimmed.startsWith("/avatars/") -> "$storage/${trimmed.removePrefix("/")}"
            else -> "$storage/avatars/${trimmed.removePrefix("/")}"
        }
        return resolveMediaUrlSync(absolute).ifEmpty { null }
    }

    private fun hostOf(rawUrl: String): String {
        return runCatching { URI(rawUrl).host.orEmpty().lowercase() }.getOrDefault("")
    }

    private fun supabaseHost(): String {
        return hostOf(SupabaseClient.supabaseUrl)
    }

    /**
     * Explicit origin classification. Directory names are deliberately ignored.
     * The only dynamic legacy patterns are infrastructure domains, not media paths.
     */
    private fun isKnownCdnHost(host: String): Boolean {
        if (host.isBlank()) return false

        val activeHost = cachedCdnUrl?.let(::hostOf).orEmpty()
        if (activeHost.isNotBlank() && host == activeHost) return true

        return host == "localhost" ||
            host == "10.0.2.2" ||
            host == "127.0.0.1" ||
            host == "::1" ||
            host.endsWith(".bore.pub") ||
            host == "bore.pub" ||
            host.endsWith(".trycloudflare.com")
    }

    /**
     * True only when the URL originates from the configured CDN/infrastructure.
     * Supabase Storage and arbitrary external hosts are never rewritten here.
     */
    fun isCdnRelated(originalUrl: String): Boolean {
        if (originalUrl.isBlank()) return false

        val host = hostOf(originalUrl)
        if (host.isBlank()) return false
        if (host == supabaseHost()) return false

        return isKnownCdnHost(host)
    }

    private fun extractMediaPath(originalUrl: String): String {
        return try {
            val uri = URI(originalUrl)
            val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
            path + (uri.rawQuery?.let { "?$it" } ?: "")
        } catch (_: Exception) {
            val withoutScheme = originalUrl.substringAfter("://", originalUrl)
            val slash = withoutScheme.indexOf('/')
            if (slash >= 0) withoutScheme.substring(slash) else "/"
        }
    }

    /** Non-blocking resolver for UI; uses only the last known-good CDN. */
    fun resolveMediaUrlSync(originalUrl: String?): String {
        if (originalUrl.isNullOrBlank()) return ""
        if (originalUrl.startsWith("content://") || originalUrl.startsWith("file://") ||
            originalUrl.startsWith("android.resource://") || originalUrl.startsWith("/")) return originalUrl
        if (!isCdnRelated(originalUrl)) return originalUrl

        val base = cachedCdnUrl?.takeIf { isValidCdnBase(it) } ?: return originalUrl
        val path = extractMediaPath(originalUrl)
        return "$base/${path.removePrefix("/")}"
    }

    suspend fun resolveMediaUrl(originalUrl: String?): String {
        if (originalUrl.isNullOrBlank()) return ""
        if (originalUrl.startsWith("content://") || originalUrl.startsWith("file://") ||
            originalUrl.startsWith("android.resource://") || originalUrl.startsWith("/")) return originalUrl

        val active = getCDNUrl()
        if (active.isBlank() || !isCdnRelated(originalUrl)) return originalUrl

        val path = extractMediaPath(originalUrl)
        return "$active/${path.removePrefix("/")}"
    }
}
