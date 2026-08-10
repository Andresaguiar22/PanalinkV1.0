package com.example.data.supabase

import android.util.Log
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response

class AuthManager {
    private val TAG = "AuthManager"

    var pendingEmail: String? = null
    var pendingPassword: String? = null

    // Sign up with display_name, email, password
    suspend fun signUp(displayName: String, email: String, pword: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        pendingEmail = email
        pendingPassword = pword
        if (!SupabaseClient.isConfigured) {
            // Mock delay and register in Demo Mode
            delay(1500)
            SupabaseClient.demoUserEmail = email
            SupabaseClient.demoUserVerified = false // Must verify email!
            
            val user = AuthUser("me_demo_id", email, null, mapOf("display_name" to displayName))
            SupabaseClient.currentUser = user
            SupabaseClient.currentToken = "demo_token_xyz"
            
            // Create profile
            val prof = Profile("me_demo_id", displayName, null, isProfileComplete = false)
            SupabaseClient.currentProfile = prof
            SupabaseClient.demoProfiles[prof.id] = prof

            return@withContext Result.success(user)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val metadata = mapOf(
                "display_name" to displayName,
                "first_name" to displayName,
                "last_name" to ""
            )
            val request = SignUpRequest(email, pword, metadata)
            
            val response: Response<ResponseBody> = service.signUp(apiKey = SupabaseClient.supabaseAnonKey, redirectTo = "panalink://verify", request = request)
            if (response.isSuccessful) {
                val responseBodyStr = response.body()?.string() ?: return@withContext Result.failure(Exception("Empty auth response"))
                val json = org.json.JSONObject(responseBodyStr)
                
                // When confirmation is ON: the response is a direct User object.
                // When confirmation is OFF: the response is a Session object containing "user" and "access_token".
                val userJson = if (json.has("user") && !json.isNull("user")) {
                    json.getJSONObject("user")
                } else {
                    json // The object itself is the user
                }
                
                val accessToken = if (json.has("access_token") && !json.isNull("access_token")) {
                    json.getString("access_token")
                } else {
                    null
                }
                
                val refreshToken = if (json.has("refresh_token") && !json.isNull("refresh_token")) {
                    json.getString("refresh_token")
                } else {
                    null
                }
                
                val userId = userJson.getString("id")
                val emailStr = if (userJson.has("email") && !userJson.isNull("email")) {
                    userJson.getString("email")
                } else {
                    email
                }
                
                val emailConfirmedAt = if (userJson.has("email_confirmed_at") && !userJson.isNull("email_confirmed_at")) {
                    userJson.getString("email_confirmed_at")
                } else {
                    null
                }
                
                val userMetadataMap = mutableMapOf<String, Any>()
                if (userJson.has("user_metadata") && !userJson.isNull("user_metadata")) {
                    val metaJson = userJson.getJSONObject("user_metadata")
                    val keys = metaJson.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        userMetadataMap[key] = metaJson.get(key)
                    }
                }
                
                val authUser = AuthUser(
                    id = userId,
                    email = emailStr,
                    emailConfirmedAt = emailConfirmedAt,
                    userMetadata = userMetadataMap
                )
                
                // If email confirmation is required (emailConfirmedAt is null or accessToken is null), do NOT log in automatically
                if (accessToken == null || emailConfirmedAt.isNullOrBlank()) {
                    Log.i(TAG, "Sign up successful for $emailStr, but email confirmation is required.")
                    SupabaseClient.currentUser = authUser
                    SupabaseClient.currentToken = null
                    SupabaseClient.currentRefreshToken = null
                    SupabaseClient.currentProfile = null
                    return@withContext Result.success(authUser)
                }

                SupabaseClient.currentUser = authUser
                SupabaseClient.currentToken = accessToken
                SupabaseClient.currentRefreshToken = refreshToken
                
                // Load real profile created by Supabase DB trigger with controlled retries
                if (accessToken != null) {
                    var realProfile: Profile? = null
                    for (attempt in 1..3) {
                        try {
                            val profileResponse = service.getProfile(SupabaseClient.supabaseAnonKey, "Bearer $accessToken", "eq.$userId")
                            if (profileResponse.isSuccessful && !profileResponse.body().isNullOrEmpty()) {
                                realProfile = profileResponse.body()!![0]
                                break
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Retry $attempt fetching public.profiles failed: ${e.message}")
                        }
                        delay(500)
                    }

                    if (realProfile != null) {
                        SupabaseClient.currentProfile = realProfile
                        SessionManager.saveSession(
                            SupabaseClient.currentToken,
                            SupabaseClient.currentRefreshToken,
                            SupabaseClient.currentUser,
                            SupabaseClient.currentProfile
                        )
                        return@withContext Result.success(authUser)
                    } else {
                        Log.i(TAG, "Profile not found in public.profiles for $userId. Setting initial incomplete profile for onboarding.")
                        val initialProfile = Profile(
                            id = userId,
                            displayName = displayName,
                            avatarUrl = null,
                            isProfileComplete = false
                        )
                        try {
                            val body = mapOf(
                                "id" to userId,
                                "display_name" to displayName,
                                "is_profile_complete" to false
                            )
                            service.insertProfile(SupabaseClient.supabaseAnonKey, "Bearer $accessToken", body)
                        } catch (e: Exception) {
                            Log.w(TAG, "Initial profile insert failed: ${e.message}")
                        }
                        SupabaseClient.currentProfile = initialProfile
                        SessionManager.saveSession(
                            SupabaseClient.currentToken,
                            SupabaseClient.currentRefreshToken,
                            SupabaseClient.currentUser,
                            SupabaseClient.currentProfile
                        )
                        return@withContext Result.success(authUser)
                    }
                } else {
                    // Email confirmation is required; no session or profile token yet
                    SupabaseClient.currentProfile = null
                    return@withContext Result.success(authUser)
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Sign up error: $errorBody")
                val cleanMsg = extractErrorMessage(errorBody)
                return@withContext Result.failure(Exception(cleanMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up exception", e)
            return@withContext Result.failure(e)
        }
    }

    // Sign in with email, password
    suspend fun signIn(email: String, pword: String): Result<AuthUser> = withContext(Dispatchers.IO) {
        pendingEmail = email
        pendingPassword = pword
        if (!SupabaseClient.isConfigured) {
            delay(1000)
            if (email.lowercase() == "demo@panalink.com" || email == SupabaseClient.demoUserEmail) {
                val isVerified = if (email == SupabaseClient.demoUserEmail) SupabaseClient.demoUserVerified else true
                val displayName = if (email == SupabaseClient.demoUserEmail) {
                    SupabaseClient.currentProfile?.displayName ?: "Mi Cuenta"
                } else "Pana Fundador"

                val user = AuthUser("me_demo_id", email, if (isVerified) "2026-06-25T12:00:00Z" else null, mapOf("display_name" to displayName))
                SupabaseClient.currentUser = user
                SupabaseClient.currentToken = "demo_token_xyz"
                
                val prof = Profile("me_demo_id", displayName, null)
                SupabaseClient.currentProfile = prof
                SupabaseClient.demoProfiles[prof.id] = prof

                return@withContext Result.success(user)
            } else {
                // Let register any, but if logging in a new demo email, let it pass
                val user = AuthUser("me_demo_id", email, "2026-06-25T12:00:00Z", mapOf("display_name" to "Mi Cuenta"))
                SupabaseClient.currentUser = user
                SupabaseClient.currentToken = "demo_token_xyz"
                val prof = Profile("me_demo_id", "Mi Cuenta", null)
                SupabaseClient.currentProfile = prof
                SupabaseClient.demoProfiles[prof.id] = prof
                return@withContext Result.success(user)
            }
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val request = SignInRequest(email, pword)
            
            val response: Response<AuthResponse> = service.signIn(SupabaseClient.supabaseAnonKey, request)
            if (response.isSuccessful) {
                val authBody = response.body() ?: return@withContext Result.failure(Exception("Response empty"))
                
                if (authBody.user.emailConfirmedAt.isNullOrBlank()) {
                    Log.w(TAG, "Sign in blocked: User ${authBody.user.email} email is not confirmed.")
                    SupabaseClient.currentUser = null
                    SupabaseClient.currentToken = null
                    SupabaseClient.currentRefreshToken = null
                    SupabaseClient.currentProfile = null
                    SessionManager.clearSession()
                    return@withContext Result.failure(Exception("Debes confirmar tu correo electrónico antes de entrar a Panalink."))
                }

                SupabaseClient.currentUser = authBody.user
                SupabaseClient.currentToken = authBody.accessToken
                SupabaseClient.currentRefreshToken = authBody.refreshToken
                
                // Fetch profile with retries
                val profileId = authBody.user.id
                var realProfile: Profile? = null
                for (attempt in 1..3) {
                    try {
                        val profileResponse = service.getProfile(SupabaseClient.supabaseAnonKey, "Bearer ${authBody.accessToken}", "eq.$profileId")
                        if (profileResponse.isSuccessful && !profileResponse.body().isNullOrEmpty()) {
                            realProfile = profileResponse.body()!![0]
                            break
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Retry $attempt fetching public.profiles on signIn failed: ${e.message}")
                    }
                    delay(500)
                }

                if (realProfile != null) {
                    SupabaseClient.currentProfile = realProfile
                    if (realProfile.isProfileComplete) {
                        SupabaseClient.connectRealtime()
                    }
                    SessionManager.saveSession(
                        SupabaseClient.currentToken,
                        SupabaseClient.currentRefreshToken,
                        SupabaseClient.currentUser,
                        SupabaseClient.currentProfile
                    )
                    return@withContext Result.success(authBody.user)
                } else {
                    val cached = SessionManager.getCachedProfile() ?: SupabaseClient.currentProfile
                    if (cached != null && cached.isProfileComplete) {
                        Log.i(TAG, "Profile fetch on signIn returned null, but valid complete cached profile found. Retaining cached profile.")
                        SupabaseClient.currentProfile = cached
                        if (cached.isProfileComplete) {
                            SupabaseClient.connectRealtime()
                        }
                        SessionManager.saveSession(
                            SupabaseClient.currentToken,
                            SupabaseClient.currentRefreshToken,
                            SupabaseClient.currentUser,
                            cached
                        )
                        return@withContext Result.success(authBody.user)
                    }

                    Log.i(TAG, "Profile not found for $profileId on sign in. Creating initial incomplete profile for onboarding.")
                    val fallbackDisplayName = authBody.user.email?.substringBefore("@") ?: "Pana"
                    val initialProfile = Profile(
                        id = profileId,
                        displayName = fallbackDisplayName,
                        avatarUrl = null,
                        isProfileComplete = false
                    )
                    try {
                        val body = mapOf(
                            "id" to profileId,
                            "display_name" to fallbackDisplayName,
                            "is_profile_complete" to false
                        )
                        service.insertProfile(SupabaseClient.supabaseAnonKey, "Bearer ${authBody.accessToken}", body)
                    } catch (e: Exception) {
                        Log.w(TAG, "Initial profile insert on signIn failed: ${e.message}")
                    }
                    SupabaseClient.currentProfile = initialProfile
                    SessionManager.saveSession(
                        SupabaseClient.currentToken,
                        SupabaseClient.currentRefreshToken,
                        SupabaseClient.currentUser,
                        SupabaseClient.currentProfile
                    )
                    return@withContext Result.success(authBody.user)
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: "Credenciales inválidas"
                Log.e(TAG, "Sign in error: $errorBody")
                val cleanMsg = extractErrorMessage(errorBody)
                return@withContext Result.failure(Exception(cleanMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in exception", e)
            return@withContext Result.failure(e)
        }
    }

    private fun extractErrorMessage(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Error desconocido"
        return try {
            val json = org.json.JSONObject(errorBody)
            when {
                json.has("msg") && !json.isNull("msg") -> json.getString("msg")
                json.has("error_description") && !json.isNull("error_description") -> json.getString("error_description")
                json.has("message") && !json.isNull("message") -> json.getString("message")
                json.has("error") && !json.isNull("error") -> json.getString("error")
                else -> errorBody
            }
        } catch (e: Exception) {
            errorBody
        }
    }

    // Verify OTP using token_hash from email deep link
    suspend fun verifyOtpByHash(tokenHash: String, type: String = "signup"): Result<AuthUser> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) return@withContext Result.failure(Exception("Supabase not configured"))
        val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Service unavailable"))
        try {
            val request = VerifyOtpRequest(tokenHash = tokenHash, type = type)
            val response = service.verifyOtp(SupabaseClient.supabaseAnonKey, request)
            if (response.isSuccessful) {
                val authBody = response.body() ?: return@withContext Result.failure(Exception("Response empty"))
                return@withContext setTokensAndFetchProfile(authBody.accessToken, authBody.refreshToken)
            } else {
                val err = response.errorBody()?.string() ?: "Error al verificar correo"
                Log.e(TAG, "verifyOtp error: $err")
                return@withContext Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Log.e(TAG, "verifyOtp exception", e)
            return@withContext Result.failure(e)
        }
    }

    // Set tokens from deep link and fetch user profile
    suspend fun setTokensAndFetchProfile(accessToken: String, refreshToken: String?): Result<AuthUser> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            return@withContext Result.failure(Exception("Supabase not configured"))
        }
        val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Service unavailable"))
        try {
            SupabaseClient.currentToken = accessToken
            if (refreshToken != null) SupabaseClient.currentRefreshToken = refreshToken
            val userResponse = service.getCurrentUser(SupabaseClient.supabaseAnonKey, "Bearer $accessToken")
            if (!userResponse.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to get user profile"))
            }
            val userRes = userResponse.body() ?: return@withContext Result.failure(Exception("User response empty"))
            val authUser = AuthUser(
                id = userRes.id,
                email = userRes.email,
                emailConfirmedAt = userRes.emailConfirmedAt,
                userMetadata = null
            )
            SupabaseClient.currentUser = authUser

            // Fetch public.profiles
            val userId = authUser.id
            var realProfile: Profile? = null
            for (attempt in 1..3) {
                try {
                    val profileResponse = service.getProfile(SupabaseClient.supabaseAnonKey, "Bearer $accessToken", "eq.$userId")
                    if (profileResponse.isSuccessful && !profileResponse.body().isNullOrEmpty()) {
                        realProfile = profileResponse.body()!![0]
                        break
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Retry $attempt fetching public.profiles in setTokensAndFetchProfile failed: ${e.message}")
                }
                delay(500)
            }

            if (realProfile != null) {
                SupabaseClient.currentProfile = realProfile
                if (realProfile.isProfileComplete) {
                    SupabaseClient.connectRealtime()
                }
                SessionManager.saveSession(
                    SupabaseClient.currentToken,
                    SupabaseClient.currentRefreshToken,
                    SupabaseClient.currentUser,
                    SupabaseClient.currentProfile
                )
                return@withContext Result.success(authUser)
            } else {
                val cached = SessionManager.getCachedProfile() ?: SupabaseClient.currentProfile
                if (cached != null && cached.isProfileComplete) {
                    Log.i(TAG, "Profile fetch in setTokensAndFetchProfile returned null, but valid complete cached profile found. Retaining cached profile.")
                    SupabaseClient.currentProfile = cached
                    if (cached.isProfileComplete) {
                        SupabaseClient.connectRealtime()
                    }
                    SessionManager.saveSession(
                        SupabaseClient.currentToken,
                        SupabaseClient.currentRefreshToken,
                        SupabaseClient.currentUser,
                        cached
                    )
                    return@withContext Result.success(authUser)
                }

                Log.i(TAG, "Profile not found for $userId in setTokensAndFetchProfile. Creating initial incomplete profile for onboarding.")
                val fallbackDisplayName = authUser.email?.substringBefore("@") ?: "Pana"
                val initialProfile = Profile(
                    id = userId,
                    displayName = fallbackDisplayName,
                    avatarUrl = null,
                    isProfileComplete = false
                )
                try {
                    val body = mapOf(
                        "id" to userId,
                        "display_name" to fallbackDisplayName,
                        "is_profile_complete" to false
                    )
                    service.insertProfile(SupabaseClient.supabaseAnonKey, "Bearer $accessToken", body)
                } catch (e: Exception) {
                    Log.w(TAG, "Initial profile insert in setTokensAndFetchProfile failed: ${e.message}")
                }
                SupabaseClient.currentProfile = initialProfile
                SessionManager.saveSession(
                    SupabaseClient.currentToken,
                    SupabaseClient.currentRefreshToken,
                    SupabaseClient.currentUser,
                    SupabaseClient.currentProfile
                )
                return@withContext Result.success(authUser)
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    // Check if current user is confirmed / verified
    suspend fun checkEmailVerification(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            delay(500)
            return@withContext Result.success(SupabaseClient.demoUserVerified)
        }

        val token = SupabaseClient.currentToken
        val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))

        if (token != null) {
            try {
                val response: Response<UserResponse> = service.getCurrentUser(SupabaseClient.supabaseAnonKey, "Bearer $token")
                if (response.isSuccessful) {
                    val user = response.body()
                    val isConfirmed = user?.emailConfirmedAt != null
                    if (isConfirmed && SupabaseClient.currentUser != null) {
                        // Update verification in our local state
                        SupabaseClient.currentUser = AuthUser(
                            id = SupabaseClient.currentUser!!.id,
                            email = user?.email,
                            emailConfirmedAt = user?.emailConfirmedAt,
                            userMetadata = SupabaseClient.currentUser!!.userMetadata
                        )
                        return@withContext Result.success(true)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "checkEmailVerification token check exception: ${e.message}")
            }
        }

        // If currentToken is null or unconfirmed, attempt signIn using pending credentials if available!
        val pEmail = pendingEmail
        val pPassword = pendingPassword
        if (!pEmail.isNullOrBlank() && !pPassword.isNullOrBlank()) {
            Log.i(TAG, "checkEmailVerification attempting signIn with pending credentials for $pEmail")
            val signInResult = signIn(pEmail, pPassword)
            if (signInResult.isSuccess) {
                val user = signInResult.getOrNull()
                if (user != null) {
                    return@withContext Result.success(true)
                }
            }
        }

        return@withContext Result.success(false)
    }

    // Set verified in Demo Mode
    fun verifyDemoUser() {
        SupabaseClient.demoUserVerified = true
        if (SupabaseClient.currentUser != null) {
            SupabaseClient.currentUser = AuthUser(
                id = SupabaseClient.currentUser!!.id,
                email = SupabaseClient.currentUser!!.email,
                emailConfirmedAt = "2026-06-25T12:00:00Z",
                userMetadata = SupabaseClient.currentUser!!.userMetadata
            )
        }
    }

    // Resend confirmation email
    suspend fun resendVerification(email: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            delay(1000)
            return@withContext Result.success(true)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val request = ResendRequest(email, "signup")
            val response = service.resendEmail(SupabaseClient.supabaseAnonKey, request)
            return@withContext if (response.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Error al reenviar"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    // Sign out
    fun signOut() {
        pendingEmail = null
        pendingPassword = null
        SessionManager.clearSession()
        SupabaseClient.disconnectRealtime()
    }

    // Delete account
    suspend fun deleteAccount(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService
            val token = SupabaseClient.currentToken
            val userId = SupabaseClient.currentUser?.id
            if (service != null && token != null && userId != null) {
                val bearer = "Bearer $token"
                val apiKey = SupabaseClient.supabaseAnonKey
                service.deleteProfile(apiKey, bearer, "eq.$userId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteAccount error: ${e.message}")
        }
        signOut()
        Result.success(true)
    }

    private fun splitEmail(email: String): String {
        return email.split("@").firstOrNull() ?: "Pana"
    }
}
