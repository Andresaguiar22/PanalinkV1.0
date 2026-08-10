package com.example.data.supabase

import android.content.Context
import android.util.Log
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
import retrofit2.Response

object SessionManager {
    private const val TAG = "SessionManager"
    private const val PREFS_NAME = "panalink_session_prefs"
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

    fun init(context: Context) {
        if (isInitialized) return
        this.context = context.applicationContext
        isInitialized = true
        Log.i(TAG, "SessionManager initialized")
        restoreSession()
        
        // Start periodic token validation / refresh loop
        scope.launch {
            while (isActive) {
                delay(120_000) // Every 2 minutes
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
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_PROFILE_JSON, null)
            if (json != null) {
                val parsed = profileAdapter.fromJson(json)
                if (parsed != null) parsed else SupabaseClient.currentProfile
            } else {
                SupabaseClient.currentProfile
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached profile", e)
            SupabaseClient.currentProfile
        }
    }

    // Save session automatically
    fun saveSession(accessToken: String?, refreshToken: String?, user: AuthUser?, profile: Profile?) {
        if (!isInitialized) return
        if (accessToken.isNullOrEmpty() || user == null) {
            Log.w(TAG, "Attempted to save session without valid token or user. Clearing session.")
            clearSession()
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingProfileJson = prefs.getString(KEY_PROFILE_JSON, null)

        var profileToSave: Profile? = profile
        if (existingProfileJson != null) {
            try {
                val existing = profileAdapter.fromJson(existingProfileJson)
                if (existing != null && existing.isProfileComplete) {
                    if (profile == null || !profile.isProfileComplete) {
                        Log.i(TAG, "Retaining existing complete profile from SharedPreferences.")
                        profileToSave = existing
                        if (SupabaseClient.currentProfile == null || !SupabaseClient.currentProfile!!.isProfileComplete) {
                            SupabaseClient.currentProfile = existing
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing existing profile in saveSession", e)
            }
        }

        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            try {
                putString(KEY_USER_JSON, userAdapter.toJson(user))
            } catch (e: Exception) {
                Log.e(TAG, "Error serializing user", e)
            }
            if (profileToSave != null) {
                try {
                    putString(KEY_PROFILE_JSON, profileAdapter.toJson(profileToSave))
                } catch (e: Exception) {
                    Log.e(TAG, "Error serializing profile", e)
                }
            }
            apply()
        }
        Log.d(TAG, "Session saved. AccessToken length: ${accessToken.length}, User: ${user.email}, Profile complete: ${profileToSave?.isProfileComplete}")
    }

    // Restore session automatically
    fun restoreSession() {
        if (!isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val userJson = prefs.getString(KEY_USER_JSON, null)
        val profileJson = prefs.getString(KEY_PROFILE_JSON, null)

        if (accessToken != null && userJson != null) {
            SupabaseClient.currentToken = accessToken
            SupabaseClient.currentRefreshToken = refreshToken
            try {
                SupabaseClient.currentUser = userAdapter.fromJson(userJson)
                if (profileJson != null) {
                    SupabaseClient.currentProfile = profileAdapter.fromJson(profileJson)
                } else {
                    SupabaseClient.currentProfile = null
                }
                Log.i(TAG, "Session restored successfully for user: ${SupabaseClient.currentUser?.email}, profile complete: ${SupabaseClient.currentProfile?.isProfileComplete}")
                
                // Validate restored session
                scope.launch {
                    val isValid = validateAndRefreshSessionIfNeeded()
                    if (isValid && SupabaseClient.currentToken != null) {
                        val userId = SupabaseClient.currentUser?.id
                        if (userId != null) {
                            val profilesRepo = com.example.data.repository.ProfilesRepository()
                            val profResult = profilesRepo.getProfile(userId)
                            val freshProfile = profResult.getOrNull()
                            if (freshProfile != null && (freshProfile.isProfileComplete || SupabaseClient.currentProfile == null)) {
                                SupabaseClient.currentProfile = freshProfile
                                saveSession(
                                    SupabaseClient.currentToken,
                                    SupabaseClient.currentRefreshToken,
                                    SupabaseClient.currentUser,
                                    freshProfile
                                )
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
    }

    fun clearSession() {
        if (!isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        SupabaseClient.currentToken = null
        SupabaseClient.currentRefreshToken = null
        SupabaseClient.currentUser = null
        SupabaseClient.currentProfile = null
        SupabaseClient.disconnectRealtime()
        Log.i(TAG, "Session cleared")
    }

    fun isJwtExpired(token: String?): Boolean {
        if (token == null) return true
        try {
            val parts = token.split(".")
            if (parts.size < 2) return true
            val payloadBase64 = parts[1]
            val decodedBytes = android.util.Base64.decode(payloadBase64, android.util.Base64.DEFAULT or android.util.Base64.NO_WRAP)
            val decodedString = String(decodedBytes, Charsets.UTF_8)
            val json = org.json.JSONObject(decodedString)
            if (json.has("exp")) {
                val exp = json.getLong("exp")
                val nowSeconds = System.currentTimeMillis() / 1000
                // Expired if current time is within 90 seconds of expiration
                return nowSeconds >= (exp - 90)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JWT", e)
        }
        return true
    }

    // Refresh token synchronously or via mutex to avoid concurrent refreshes (single-flight)
    suspend fun refreshSession(): Boolean = mutex.withLock {
        if (!SupabaseClient.isConfigured) return@withLock true

        // Double-check if token was already refreshed by a concurrent caller
        val currentToken = SupabaseClient.currentToken
        if (currentToken != null && !isJwtExpired(currentToken)) {
            Log.i(TAG, "Session was already refreshed by a concurrent task.")
            return@withLock true
        }
        
        val rToken = SupabaseClient.currentRefreshToken
        if (rToken.isNullOrEmpty()) {
            Log.w(TAG, "No refresh token available to refresh session")
            return@withLock false
        }

        Log.i(TAG, "Attempting to refresh session via API...")
        try {
            val service = SupabaseClient.apiService
            if (service == null) {
                Log.e(TAG, "apiService is null")
                return@withLock false
            }

            val request = RefreshTokenRequest(rToken)
            val response = service.refreshToken(SupabaseClient.supabaseAnonKey, request)
            if (response.isSuccessful) {
                val authBody = response.body()
                if (authBody != null) {
                    // Update tokens FIRST so subsequent queries use valid token
                    SupabaseClient.currentToken = authBody.accessToken
                    SupabaseClient.currentRefreshToken = authBody.refreshToken ?: rToken
                    SupabaseClient.currentUser = authBody.user

                    val userId = authBody.user.id
                    val profilesRepo = com.example.data.repository.ProfilesRepository()
                    val profResult = profilesRepo.getProfile(userId)
                    val realProfile = profResult.getOrNull()

                    if (realProfile != null && realProfile.isProfileComplete) {
                        SupabaseClient.currentProfile = realProfile
                    } else if (SupabaseClient.currentProfile == null) {
                        SupabaseClient.currentProfile = getCachedProfile()
                    }

                    saveSession(
                        SupabaseClient.currentToken,
                        SupabaseClient.currentRefreshToken,
                        SupabaseClient.currentUser,
                        SupabaseClient.currentProfile
                    )
                    _isOffline.value = false
                    Log.i(TAG, "Session refreshed successfully. New token updated.")
                    _sessionEvent.emit(SessionEvent.REFRESHED)
                    return@withLock true
                }
            } else {
                val code = response.code()
                val errBody = response.errorBody()?.string() ?: ""
                Log.e(TAG, "Session refresh failed (HTTP $code): $errBody")
                if ((code == 400 || code == 401) && (errBody.contains("invalid_grant") || errBody.contains("invalid_refresh_token"))) {
                    Log.w(TAG, "Refresh token permanently rejected by server ($code). Clearing session.")
                    clearSession()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network or server exception during session refresh", e)
            _isOffline.value = true // Mark offline status on network errors, retain session credentials
        }
        return@withLock false
    }

    // Checks current session, refreshes if expired
    suspend fun validateAndRefreshSessionIfNeeded(): Boolean {
        if (SupabaseClient.currentToken == null && isInitialized) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
            val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
            val userJson = prefs.getString(KEY_USER_JSON, null)
            val profileJson = prefs.getString(KEY_PROFILE_JSON, null)
            if (accessToken != null && userJson != null) {
                SupabaseClient.currentToken = accessToken
                SupabaseClient.currentRefreshToken = refreshToken
                try {
                    SupabaseClient.currentUser = userAdapter.fromJson(userJson)
                    if (profileJson != null) {
                        SupabaseClient.currentProfile = profileAdapter.fromJson(profileJson)
                    }
                    Log.i(TAG, "validateAndRefreshSessionIfNeeded: Restored session synchronously during pre-flight check")
                } catch (e: Exception) {
                    Log.e(TAG, "validateAndRefreshSessionIfNeeded: Failed to parse restored session synchronously", e)
                }
            }
        }

        val token = SupabaseClient.currentToken
        if (token != null && isJwtExpired(token)) {
            Log.i(TAG, "Current JWT is expired. Refreshing token...")
            return refreshSession()
        }
        return token != null
    }

    fun <T> saveCache(key: String, obj: T, elementClass: Class<T>) {
        if (!isInitialized) return
        try {
            val adapter = moshi.adapter(elementClass)
            val json = adapter.toJson(obj)
            val prefs = context.getSharedPreferences("panalink_cache_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString(key, json).apply()
            Log.d(TAG, "Saved cache for key: $key")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cache for key: $key", e)
        }
    }

    fun <T> getCache(key: String, elementClass: Class<T>): T? {
        if (!isInitialized) return null
        try {
            val prefs = context.getSharedPreferences("panalink_cache_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString(key, null) ?: return null
            val adapter = moshi.adapter(elementClass)
            return adapter.fromJson(json)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache for key: $key", e)
            return null
        }
    }

    // Generic Moshi list caching helper
    fun <T> saveCacheList(key: String, list: List<T>, elementClass: Class<T>) {
        if (!isInitialized) return
        try {
            val type = Types.newParameterizedType(List::class.java, elementClass)
            val adapter = moshi.adapter<List<T>>(type)
            val json = adapter.toJson(list)
            val prefs = context.getSharedPreferences("panalink_cache_prefs", Context.MODE_PRIVATE)
            prefs.edit().putString(key, json).apply()
            Log.d(TAG, "Saved cache list for key: $key, size: ${list.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cache list for key: $key", e)
        }
    }

    fun <T> getCacheList(key: String, elementClass: Class<T>): List<T> {
        if (!isInitialized) return emptyList()
        try {
            val prefs = context.getSharedPreferences("panalink_cache_prefs", Context.MODE_PRIVATE)
            val json = prefs.getString(key, null) ?: return emptyList()
            val type = Types.newParameterizedType(List::class.java, elementClass)
            val adapter = moshi.adapter<List<T>>(type)
            return adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache list for key: $key", e)
            return emptyList()
        }
    }

    // Trigger explicit sync
    fun triggerSync() {
        scope.launch {
            _sessionEvent.emit(SessionEvent.SYNC_NEEDED)
        }
    }

    fun setOffline(offline: Boolean) {
        _isOffline.value = offline
    }

    fun getUserAuthToken(): String? = SupabaseClient.currentToken

    fun getCurrentUserId(): String? = SupabaseClient.currentUser?.id
}
