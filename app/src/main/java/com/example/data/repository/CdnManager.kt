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
 * Rules:
 *  - Supabase remains the source of truth for the current CDN candidate.
 *  - A candidate is promoted only after a successful health check.
 *  - A failed refresh NEVER replaces a known-good CDN.
 *  - Realtime updates are validated before being cached.
 *  - UI/media consumers can safely use resolveMediaUrlSync() without doing network I/O.
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

    /**
     * Validates the CDN itself, not merely DNS/connectivity.
     * /health must return a successful 2xx response.
     */
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

    /**
     * Realtime is advisory. Never promote a URL received from Realtime without validation.
     */
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

    /**
     * Gets the active CDN from local cache or Supabase.
     * A failed Supabase refresh keeps the last known-good CDN.
     */
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
        val storage = "$supabase/storage/v1/object/public"
        val absolute = when {
            trimmed.startsWith("http://") || trimmed.startsWith("https://") ->
                if (trimmed.startsWith("http://") && trimmed.contains(URI(supabase).host ?: "supabase.co")) trimmed.replaceFirst("http://", "https://") else trimmed
            trimmed.startsWith("/storage/v1/object/public/") -> "$supabase$trimmed"
            trimmed.startsWith("storage/v1/object/public/") -> "$supabase/$trimmed"
            trimmed.startsWith("avatars/") || trimmed.startsWith("/avatars/") -> "$storage/${trimmed.removePrefix("/")}"
            else -> "$storage/avatars/${trimmed.removePrefix("/")}"
        }
        return resolveMediaUrlSync(absolute).ifEmpty { null }
    }

    fun isCdnRelated(originalUrl: String): Boolean {
        if (originalUrl.isBlank()) return false
        val supabaseHost = try { URI(SupabaseClient.supabaseUrl).host.orEmpty() } catch (_: Exception) { "" }
        if (supabaseHost.isNotBlank() && originalUrl.contains(supabaseHost, ignoreCase = true)) return false

        val lower = originalUrl.lowercase()
        return lower.contains("bore.pub") ||
            lower.contains("trycloudflare") ||
            lower.contains("10.0.2.2") ||
            lower.contains("localhost") ||
            lower.contains("/video/") ||
            lower.contains("/files/") ||
            lower.contains("/documents/") ||
            lower.contains("/uploads/") ||
            lower.contains("/images/") ||
            lower.contains("/avatars/") ||
            lower.contains("/audios/")
    }

    private fun extractMediaPath(originalUrl: String): String {
        return try {
            val uri = URI(originalUrl)
            val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
            path + (uri.rawQuery?.let { "?$it" } ?: "")
        } catch (_: Exception) {
            originalUrl.substringAfter("://", originalUrl).let { "/${it.substringAfter('/', "")}" }
        }
    }

    /**
     * Non-blocking resolver for UI. It only uses the last known-good CDN.
     * Network refresh must be done by getCDNUrl()/resolveMediaUrl().
     */
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