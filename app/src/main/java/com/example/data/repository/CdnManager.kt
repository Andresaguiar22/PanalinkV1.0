package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
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
 */
object CdnManager {
    private const val TAG = "CdnManager"
    private const val PREFS_NAME = "panalink_cdn_prefs"
    private const val KEY_CACHED_CDN_URL = "cached_cdn_url"

    @Volatile
    private var cachedCdnUrl: String? = null

    @Volatile
    private var lastSupabaseCandidate: String? = null

    @Volatile
    private var lastRealtimeCandidate: String? = null

    @Volatile
    private var lastHealthUrl: String? = null

    @Volatile
    private var lastHealthCode: Int? = null

    @Volatile
    private var lastHealthOk: Boolean? = null

    @Volatile
    private var lastRefreshSource: String? = null

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
                Log.i(TAG, "CDN_DIAGNOSTIC cached=$stored")
            }
        } catch (e: Exception) {
            Log.e(TAG, "CDN_DIAGNOSTIC failed to restore cache", e)
        }

        Log.i(TAG, "CDN_DIAGNOSTIC init supabase=${SupabaseClient.supabaseUrl.trim().removeSuffix("/")}")

        if (isStartupRefreshStarted.compareAndSet(false, true)) {
            scope.launch {
                delay(500)
                runCatching { getCDNUrl(forceRefresh = true) }
                    .onFailure { Log.w(TAG, "CDN_DIAGNOSTIC startup refresh failed: ${it.message}") }
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
            Log.e(TAG, "CDN_DIAGNOSTIC failed to persist CDN", e)
        }
    }

    /** /health must return an actual successful 2xx response. */
    private fun isCdnReachable(cdnUrl: String): Boolean {
        val base = normalizeBase(cdnUrl)
        if (!isValidCdnBase(base)) {
            Log.w(TAG, "CDN_DIAGNOSTIC health skipped invalid_base=$base")
            return false
        }

        val healthUrl = "$base/health"
        lastHealthUrl = healthUrl
        lastHealthCode = null
        lastHealthOk = false

        return try {
            val request = Request.Builder()
                .url(healthUrl)
                .get()
                .header("Cache-Control", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                val ok = response.code in 200..299
                lastHealthCode = response.code
                lastHealthOk = ok
                if (ok) {
                    Log.i(TAG, "CDN_DIAGNOSTIC health=OK url=$healthUrl code=${response.code}")
                } else {
                    Log.w(TAG, "CDN_DIAGNOSTIC health=FAILED url=$healthUrl code=${response.code}")
                }
                ok
            }
        } catch (e: Exception) {
            lastHealthCode = null
            lastHealthOk = false
            Log.w(TAG, "CDN_DIAGNOSTIC health=EXCEPTION url=$healthUrl error=${e.javaClass.simpleName}:${e.message}")
            false
        }
    }

    /** Realtime is advisory; never promote an unvalidated URL. */
    private fun startRealtimeListener() {
        if (!isListenerStarted.compareAndSet(false, true)) return

        Log.i(TAG, "CDN_DIAGNOSTIC realtime=STARTING table=global_server_config")
        scope.launch {
            try {
                SupabaseClient.globalServerConfigUpdates.collect { newUrl ->
                    val candidate = newUrl?.trim()?.removeSuffix("/")
                    lastRealtimeCandidate = candidate
                    Log.i(TAG, "CDN_DIAGNOSTIC realtime=UPDATE candidate=$candidate")

                    if (candidate.isNullOrBlank() || !isValidCdnBase(candidate)) {
                        Log.w(TAG, "CDN_DIAGNOSTIC realtime=REJECT invalid_candidate=$newUrl")
                        return@collect
                    }

                    if (isCdnReachable(candidate)) {
                        promoteCdn(candidate, "Realtime")
                    } else {
                        Log.w(TAG, "CDN_DIAGNOSTIC realtime=REJECT health_failed candidate=$candidate")
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "CDN_DIAGNOSTIC realtime=STOPPED error=${t.message}", t)
                isListenerStarted.set(false)
            }
        }
    }

    private fun promoteCdn(candidate: String, source: String) {
        val clean = normalizeBase(candidate)
        val previous = cachedCdnUrl
        lastRefreshSource = source

        if (previous == clean) {
            Log.i(TAG, "CDN_DIAGNOSTIC active=UNCHANGED source=$source url=$clean")
            return
        }

        cachedCdnUrl = clean
        saveToPrefs(clean)
        Log.i(TAG, "CDN_DIAGNOSTIC active=PROMOTED source=$source previous=$previous new=$clean")
    }

    suspend fun getCDNUrl(forceRefresh: Boolean = false): String = cdnMutex.withLock {
        startRealtimeListener()

        Log.i(TAG, "CDN_DIAGNOSTIC refresh=start force=$forceRefresh cached=${cachedCdnUrl.orEmpty()}")

        if (!forceRefresh) {
            cachedCdnUrl?.takeIf { isValidCdnBase(it) }?.let {
                Log.i(TAG, "CDN_DIAGNOSTIC refresh=cache_hit active=$it")
                return@withLock it
            }
        }

        val supabaseUrl = SupabaseClient.supabaseUrl.trim().removeSuffix("/")
        val anonKey = SupabaseClient.supabaseAnonKey
        if (supabaseUrl.isBlank() || anonKey.isBlank()) {
            Log.e(TAG, "CDN_DIAGNOSTIC supabase_config=INCOMPLETE preserving=${cachedCdnUrl.orEmpty()}")
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

                Log.i(TAG, "CDN_DIAGNOSTIC supabase=REQUEST endpoint=$endpoint attempt=${3 - attempts}")
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string()?.trim().orEmpty()
                    Log.i(TAG, "CDN_DIAGNOSTIC supabase=RESPONSE http=${response.code} bodyPresent=${body.isNotEmpty()}")

                    if (!response.isSuccessful || body.isEmpty()) {
                        Log.w(TAG, "CDN_DIAGNOSTIC supabase=FAILED http=${response.code}")
                    } else {
                        val json = when {
                            body.startsWith("[") -> JSONArray(body).let { if (it.length() > 0) it.getJSONObject(0) else null }
                            body.startsWith("{") -> JSONObject(body)
                            else -> null
                        }

                        if (json == null) {
                            Log.w(TAG, "CDN_DIAGNOSTIC supabase=INVALID_JSON")
                        } else {
                            val active = json.optBoolean("active", false)
                            val candidate = normalizeBase(json.optString("cdn_url", ""))
                            lastSupabaseCandidate = candidate.takeIf { it.isNotBlank() }
                            Log.i(TAG, "CDN_DIAGNOSTIC supabase=CONFIG active=$active candidate=$candidate cached=${cachedCdnUrl.orEmpty()}")

                            if (!active) {
                                Log.w(TAG, "CDN_DIAGNOSTIC supabase=INACTIVE preserving=${cachedCdnUrl.orEmpty()}")
                            } else if (!isValidCdnBase(candidate)) {
                                Log.w(TAG, "CDN_DIAGNOSTIC supabase=INVALID_URL candidate=$candidate")
                            } else if (isCdnReachable(candidate)) {
                                promoteCdn(candidate, "Supabase")
                                Log.i(TAG, "CDN_DIAGNOSTIC flow=SUPABASE->HEALTH->ACTIVE success=true")
                                return@withLock candidate
                            } else {
                                Log.w(TAG, "CDN_DIAGNOSTIC flow=SUPABASE->HEALTH->ACTIVE success=false preserving=${cachedCdnUrl.orEmpty()}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "CDN_DIAGNOSTIC supabase=EXCEPTION error=${e.javaClass.simpleName}:${e.message}")
            }

            if (attempts > 0) delay(1000)
        }

        Log.w(TAG, "CDN_DIAGNOSTIC refresh=END active=${cachedCdnUrl.orEmpty()} supabaseCandidate=${lastSupabaseCandidate.orEmpty()}")
        return@withLock cachedCdnUrl.orEmpty()
    }

    /**
     * Snapshot for diagnostics/UI tests. It performs no network operation.
     */
    fun diagnosticSnapshot(): String {
        return buildString {
            append("supabase=")
            append(SupabaseClient.supabaseUrl.trim().removeSuffix("/"))
            append("\ncached=")
            append(cachedCdnUrl.orEmpty())
            append("\nsupabaseCandidate=")
            append(lastSupabaseCandidate.orEmpty())
            append("\nrealtimeCandidate=")
            append(lastRealtimeCandidate.orEmpty())
            append("\nhealthUrl=")
            append(lastHealthUrl.orEmpty())
            append("\nhealthCode=")
            append(lastHealthCode?.toString().orEmpty())
            append("\nhealthOk=")
            append(lastHealthOk?.toString().orEmpty())
            append("\nlastSource=")
            append(lastRefreshSource.orEmpty())
        }
    }

    fun clearCache() {
        cachedCdnUrl = null
        try {
            context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.edit()?.remove(KEY_CACHED_CDN_URL)?.apply()
        } catch (_: Exception) { }
        Log.i(TAG, "CDN_DIAGNOSTIC cache=CLEARED")
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
