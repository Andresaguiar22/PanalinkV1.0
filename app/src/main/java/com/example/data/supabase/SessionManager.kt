package com.example.data.supabase

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SessionManager {
    private const val TAG = "SessionManager"
    private const val PREFS_NAME = "panalink_session_secure_prefs"
    private const val LEGACY_PREFS_NAME = "panalink_session_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_USER_JSON = "user_json"
    private const val KEY_PROFILE_JSON = "profile_json"

    private lateinit var context: Context
    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val moshi = Moshi.Builder()
        .add(com.example.data.model.ProfileSurrogateAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val userAdapter = moshi.adapter(AuthUser::class.java)
    private val profileAdapter = moshi.adapter(Profile::class.java)

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    private val _sessionEvent = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 16)
    val sessionEvent: SharedFlow<SessionEvent> = _sessionEvent

    var isInitialized = false
        private set

    enum class SessionEvent {
        REFRESHED,
        SYNC_NEEDED
    }

    private fun securePrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun migrateLegacySessionIfNeeded() {
        try {
            val secure = securePrefs()
            if (secure.contains(KEY_ACCESS_TOKEN)) return
            val legacy = context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
            val access = legacy.getString(KEY_ACCESS_TOKEN, null)
            val user = legacy.getString(KEY_USER_JSON, null)
            if (access.isNullOrEmpty() || user.isNullOrEmpty()) return

            secure.edit().apply {
                putString(KEY_ACCESS_TOKEN, access)
                legacy.getString(KEY_REFRESH_TOKEN, null)?.let { putString(KEY_REFRESH_TOKEN, it) }
                putString(KEY_USER_JSON, user)
                legacy.getString(KEY_PROFILE_JSON, null)?.let { putString(KEY_PROFILE_JSON, it) }
                commit()
            }
            legacy.edit().clear().commit()
            Log.i(TAG, "Migrated legacy session credentials to encrypted storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to migrate legacy session storage", e)
        }
    }

    fun init(context: Context) {
        if (isInitialized) return
        this.context = context.applicationContext
        isInitialized = true
        Log.i(TAG, "SessionManager initialized")
        migrateLegacySessionIfNeeded()
        restoreSession()

        scope.launch {
            while (isActive) {
                delay(120_000)
                if (SupabaseClient.currentToken != null) {
                    validateAndRefreshSessionIfNeeded()
                }
            }
        }
    }

    fun getCachedProfile(): Profile? {
        if (SupabaseClient.currentProfile != null && SupabaseClient.currentProfile!!.isProfileComplete) {
            return SupabaseClient.currentProfile
        }
        if (!isInitialized) return SupabaseClient.currentProfile
        return try {
            val json = securePrefs().getString(KEY_PROFILE_JSON, null)
            if (json != null) profileAdapter.fromJson(json) ?: SupabaseClient.currentProfile
            else SupabaseClient.currentProfile
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached profile", e)
            SupabaseClient.currentProfile
        }
    }

    fun saveSession(accessToken: String?, refreshToken: String?, user: AuthUser?, profile: Profile?) {
        if (!isInitialized) return
        if (accessToken.isNullOrEmpty() || user == null) {
            Log.w(TAG, "Attempted to save session without valid token or user. Clearing session.")
            clearSession()
            return
        }

        val prefs = securePrefs()
        val existingProfileJson = prefs.getString(KEY_PROFILE_JSON, null)
        var profileToSave: Profile? = profile
        if (existingProfileJson != null) {
            try {
                val existing = profileAdapter.fromJson(existingProfileJson)
                if (existing != null && existing.isProfileComplete && (profile == null || !profile.isProfileComplete)) {
                    profileToSave = existing
                    if (SupabaseClient.currentProfile == null || !SupabaseClient.currentProfile!!.isProfileComplete) {
                        SupabaseClient.currentProfile = existing
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing existing profile in saveSession", e)
            }
        }

        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            if (!refreshToken.isNullOrEmpty()) putString(KEY_REFRESH_TOKEN, refreshToken)
            try { putString(KEY_USER_JSON, userAdapter.toJson(user)) }
            catch (e: Exception) { Log.e(TAG, "Error serializing user", e) }
            if (profileToSave != null) {
                try { putString(KEY_PROFILE_JSON, profileAdapter.toJson(profileToSave)) }
                catch (e: Exception) { Log.e(TAG, "Error serializing profile", e) }
            }
            apply()
        }
        Log.d(TAG, "Session saved. Access token length=${accessToken.length}")
    }

    fun restoreSession() {
        if (!isInitialized) return
        try {
            val prefs = securePrefs()
            val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
            val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
            val userJson = prefs.getString(KEY_USER_JSON, null)
            val profileJson = prefs.getString(KEY_PROFILE_JSON, null)

            if (accessToken != null && userJson != null) {
                SupabaseClient.currentToken = accessToken
                SupabaseClient.currentRefreshToken = refreshToken
                try {
                    SupabaseClient.currentUser = userAdapter.fromJson(userJson)
                    SupabaseClient.currentProfile = profileJson?.let { profileAdapter.fromJson(it) }
                    Log.i(TAG, "Session restored successfully")
                    scope.launch {
                        val isValid = validateAndRefreshSessionIfNeeded()
                        if (isValid && SupabaseClient.currentToken != null) {
                            val userId = SupabaseClient.currentUser?.id
                            if (userId != null) {
                                val profilesRepo = com.example.data.repository.ProfilesRepository()
                                val freshProfile = profilesRepo.getProfile(userId).getOrNull()
                                if (freshProfile != null && (freshProfile.isProfileComplete || SupabaseClient.currentProfile == null)) {
                                    SupabaseClient.currentProfile = freshProfile
                                    saveSession(SupabaseClient.currentToken, SupabaseClient.currentRefreshToken, SupabaseClient.currentUser, freshProfile)
                                }
                            }
                            if (SupabaseClient.currentProfile?.isProfileComplete == true) {
                                SupabaseClient.connectRealtime()
                                _sessionEvent.emit(SessionEvent.SYNC_NEEDED)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse restored session", e)
                }
            } else {
                Log.i(TAG, "No saved session found to restore")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unable to access encrypted session storage", e)
            clearSession()
        }
    }

    fun clearSession() {
        if (!isInitialized) return
        try { securePrefs().edit().clear().apply() } catch (e: Exception) { Log.e(TAG, "Error clearing secure session", e) }
        try { context.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply() } catch (_: Exception) { }
        SupabaseClient.currentToken = null
        SupabaseClient.currentRefreshToken = null
        SupabaseClient.currentUser = null
        SupabaseClient.currentProfile = null
        SupabaseClient.disconnectRealtime()
        Log.i(TAG, "Session cleared")
    }

    fun getJwtUserId(token: String?): String? {
        if (token.isNullOrBlank()) return null
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return null
            val decodedBytes = android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT or android.util.Base64.NO_WRAP)
            val json = org.json.JSONObject(String(decodedBytes, Charsets.UTF_8))
            if (json.has("sub")) json.optString("sub") else null
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JWT sub claim", e)
            null
        }
    }

    fun isJwtExpired(token: String?): Boolean {
        if (token.isNullOrBlank()) return true
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true
            val decodedBytes = android.util.Base64.decode(parts[1], android.util.Base64.DEFAULT or android.util.Base64.NO_WRAP)
            val json = org.json.JSONObject(String(decodedBytes, Charsets.UTF_8))
            if (json.has("exp")) {
                val exp = json.getLong("exp")
                val nowSeconds = System.currentTimeMillis() / 1000
                nowSeconds >= (exp - 90)
            } else true
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JWT", e)
            true
        }
    }

    suspend fun refreshSession(): Boolean = mutex.withLock {
        if (!SupabaseClient.isConfigured) return@withLock true
        val activeToken = SupabaseClient.currentToken
        if (activeToken != null && !isJwtExpired(activeToken)) return@withLock true

        var rToken = SupabaseClient.currentRefreshToken
        if (rToken.isNullOrEmpty() && isInitialized) {
            rToken = try { securePrefs().getString(KEY_REFRESH_TOKEN, null) } catch (_: Exception) { null }
            if (!rToken.isNullOrEmpty()) SupabaseClient.currentRefreshToken = rToken
        }
        if (rToken.isNullOrEmpty()) {
            Log.w(TAG, "No refresh token available to refresh session")
            return@withLock false
        }

        try {
            val service = SupabaseClient.apiService ?: return@withLock false
            val response = service.refreshToken(SupabaseClient.supabaseAnonKey, RefreshTokenRequest(rToken))
            if (response.isSuccessful) {
                val authBody = response.body()
                if (authBody != null) {
                    SupabaseClient.currentToken = authBody.accessToken
                    SupabaseClient.currentRefreshToken = authBody.refreshToken ?: rToken
                    SupabaseClient.currentUser = authBody.user
                    if (SupabaseClient.currentProfile == null) SupabaseClient.currentProfile = getCachedProfile()
                    saveSession(SupabaseClient.currentToken, SupabaseClient.currentRefreshToken, SupabaseClient.currentUser, SupabaseClient.currentProfile)
                    _isOffline.value = false
                    try { if (SupabaseClient.currentProfile?.isProfileComplete == true) SupabaseClient.connectRealtime() } catch (e: Exception) { Log.w(TAG, "Error reconnecting realtime", e) }
                    _sessionEvent.emit(SessionEvent.REFRESHED)
                    return@withLock true
                }
            } else {
                val code = response.code()
                val errBody = response.errorBody()?.string() ?: ""
                Log.e(TAG, "Session refresh failed (HTTP $code)")
                if ((code == 400 || code == 401) && (errBody.contains("invalid_grant") || errBody.contains("invalid_refresh_token"))) clearSession()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network or server exception during session refresh", e)
            _isOffline.value = true
        }
        false
    }

    suspend fun validateAndRefreshSessionIfNeeded(): Boolean {
        if (SupabaseClient.currentToken == null && isInitialized) {
            try {
                val prefs = securePrefs()
                val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
                val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
                val userJson = prefs.getString(KEY_USER_JSON, null)
                val profileJson = prefs.getString(KEY_PROFILE_JSON, null)
                if (accessToken != null && userJson != null) {
                    SupabaseClient.currentToken = accessToken
                    SupabaseClient.currentRefreshToken = refreshToken
                    SupabaseClient.currentUser = userAdapter.fromJson(userJson)
                    SupabaseClient.currentProfile = profileJson?.let { profileAdapter.fromJson(it) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restore session during pre-flight", e)
            }
        }
        val token = SupabaseClient.currentToken
        return if (token != null && isJwtExpired(token)) refreshSession() else token != null
    }

    fun <T> saveCache(key: String, obj: T, elementClass: Class<T>) {
        if (!isInitialized) return
        try {
            val adapter = moshi.adapter(elementClass)
            val json = adapter.toJson(obj)
            context.getSharedPreferences("panalink_cache_prefs", Context.MODE_PRIVATE).edit().putString(key, json).apply()
        } catch (e: Exception) { Log.e(TAG, "Error saving cache for key: $key", e) }
    }

    fun <T> getCache(key: String, elementClass: Class<T>): T? {
        if (!isInitialized) return null
        return try {
            val json = context.getSharedPreferences("panalink_cache_prefs", Context.MODE_PRIVATE).getString(key, null) ?: return null
            moshi.adapter(elementClass).fromJson(json)
        } catch (e: Exception) { Log.e(TAG, "Error getting cache for key: $key", e); null }
    }

    fun <T> saveCacheList(key: String, list: List<T>, elementClass: Class<T>) {
        if (!isInitialized) return
        try {
            val type = Types.newParameterizedType(List::class.java, elementClass)
            val adapter = moshi.adapter<List<T>>(type)
            context.getSharedPreferences("panalink_cache_prefs", Context.MODE_PRIVATE).edit().putString(key, adapter.toJson(list)).apply()
        } catch (e: Exception) { Log.e(TAG, "Error saving cache list for key: $key", e) }
    }

    fun <T> getCacheList(key: String, elementClass: Class<T>): List<T> {
        if (!isInitialized) return emptyList()
        return try {
            val json = context.getSharedPreferences("panalink_cache_prefs", Context.MODE_PRIVATE).getString(key, null) ?: return emptyList()
            val type = Types.newParameterizedType(List::class.java, elementClass)
            moshi.adapter<List<T>>(type).fromJson(json) ?: emptyList()
        } catch (e: Exception) { Log.e(TAG, "Error getting cache list for key: $key", e); emptyList() }
    }

    fun triggerSync() { scope.launch { _sessionEvent.emit(SessionEvent.SYNC_NEEDED) } }
    fun setOffline(offline: Boolean) { _isOffline.value = offline }
    fun getUserAuthToken(): String? = SupabaseClient.currentToken
    fun getCurrentUserId(): String? = SupabaseClient.currentUser?.id
}
