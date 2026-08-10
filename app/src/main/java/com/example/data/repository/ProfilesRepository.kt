package com.example.data.repository

import android.util.Log
import com.example.data.model.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
class ProfilesRepository {
    private val TAG = "ProfilesRepository"

    private suspend fun <R> runCall(call: suspend (String) -> retrofit2.Response<R>): retrofit2.Response<R>? {
        return com.example.util.Resilience.retry(
            times = 3,
            initialDelay = 500L,
            retryCondition = { it is java.io.IOException || (it is retrofit2.HttpException && it.code() in 500..599) }
        ) {
            SessionManager.validateAndRefreshSessionIfNeeded()
            var token = SupabaseClient.currentToken ?: return@retry null
            var bearer = "Bearer $token"
            
            var response = try {
                call(bearer)
            } catch (e: Exception) {
                Log.e(TAG, "Network call failed", e)
                throw e
            }
            
            if (response != null && response.code() == 401) {
                Log.i(TAG, "401/JWT expired detected. Triggering refresh session...")
                val refreshed = SessionManager.refreshSession()
                if (refreshed) {
                    val newToken = SupabaseClient.currentToken ?: ""
                    bearer = "Bearer $newToken"
                    response = try {
                        call(bearer)
                    } catch (e: Exception) {
                        Log.e(TAG, "Retry call failed", e)
                        throw e
                    }
                }
            }
            response
        }
    }

    suspend fun getProfile(userId: String): Result<Profile> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            val prof = SupabaseClient.demoProfiles[userId]
            return@withContext if (prof != null) {
                Result.success(prof)
            } else {
                Result.failure(Exception("Usuario no encontrado"))
            }
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            
            val response = runCall { b -> service.getProfile(SupabaseClient.supabaseAnonKey, b, "eq.$userId") }
            if (response != null && response.isSuccessful && !response.body().isNullOrEmpty()) {
                val profile = response.body()!![0]
                if (userId == SupabaseClient.currentUser?.id) {
                    SupabaseClient.currentProfile = profile
                    SessionManager.saveSession(
                        SupabaseClient.currentToken,
                        SupabaseClient.currentRefreshToken,
                        SupabaseClient.currentUser,
                        profile
                    )
                }
                Result.success(profile)
            } else {
                if (userId == SupabaseClient.currentUser?.id) {
                    Log.w(TAG, "getProfile returned empty or failed for current user.")
                    val cachedProfile = SessionManager.getCachedProfile() ?: SupabaseClient.currentProfile
                    if (cachedProfile != null && cachedProfile.isProfileComplete) {
                        Log.i(TAG, "Recovered profile from local cache/memory.")
                        SupabaseClient.currentProfile = cachedProfile
                        Result.success(cachedProfile)
                    } else {
                        Result.failure(Exception("Perfil no encontrado en public.profiles"))
                    }
                } else {
                    Result.failure(Exception("Perfil no encontrado en public.profiles"))
                }
            }
        } catch (e: Exception) {
            if (userId == SupabaseClient.currentUser?.id) {
                Log.w(TAG, "getProfile exception for current user: ${e.message}")
                val cachedProfile = SessionManager.getCachedProfile() ?: SupabaseClient.currentProfile
                if (cachedProfile != null) {
                    SupabaseClient.currentProfile = cachedProfile
                    Result.success(cachedProfile)
                } else {
                    Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun getCachedProfile(userId: String): Profile? = withContext(Dispatchers.IO) {
        try {
            val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            db.profileDao().getProfileById(userId)?.toProfile()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached profile", e)
            null
        }
    }

    suspend fun saveProfileToCache(profile: Profile) = withContext(Dispatchers.IO) {
        try {
            val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            val existing = db.profileDao().getProfileById(profile.id)
            val entity = com.example.data.database.ProfileEntity.fromProfile(profile).copy(
                avatarLocalPath = existing?.avatarLocalPath,
                coverLocalPath = existing?.coverLocalPath,
                lastSyncedAt = existing?.lastSyncedAt ?: System.currentTimeMillis()
            )
            db.profileDao().insertProfile(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving profile to cache", e)
        }
    }

    suspend fun completeUserProfile(
        userId: String,
        displayName: String,
        avatarUrl: String?,
        firstName: String? = null,
        lastName: String? = null,
        status: String? = "active",
        birthDate: String? = null,
        sex: String? = null,
        interests: List<String>? = emptyList(),
        coverUrl: String? = null
    ): Result<Profile> {
        return updateProfile(
            userId = userId,
            displayName = displayName,
            avatarUrl = avatarUrl,
            isProfileComplete = true,
            firstName = firstName,
            lastName = lastName,
            status = status,
            birthDate = birthDate,
            sex = sex,
            interests = interests,
            coverUrl = coverUrl
        )
    }

    suspend fun profileExists(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            return@withContext SupabaseClient.demoProfiles.containsKey(userId)
        }
        try {
            val service = SupabaseClient.apiService ?: return@withContext false
            val response = runCall { b -> service.getProfile(SupabaseClient.supabaseAnonKey, b, "eq.$userId") }
            if (response != null && response.isSuccessful) {
                val body = response.body()
                return@withContext !body.isNullOrEmpty()
            }
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking profile exists", e)
            false
        }
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String,
        avatarUrl: String?,
        isProfileComplete: Boolean? = null,
        firstName: String? = null,
        lastName: String? = null,
        status: String? = null,
        birthDate: String? = null,
        sex: String? = null,
        interests: List<String>? = null,
        coverUrl: String? = null,
        avatarLocalPath: String? = null,
        coverLocalPath: String? = null
    ): Result<Profile> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            delay(1000)
            val updated = Profile(
                id = userId, 
                displayName = displayName, 
                avatarUrl = avatarUrl,
                isProfileComplete = isProfileComplete ?: false,
                firstName = firstName,
                lastName = lastName,
                status = status ?: "active",
                birthDate = birthDate,
                sex = sex,
                interests = interests ?: emptyList(),
                coverUrl = coverUrl
            )
            SupabaseClient.demoProfiles[userId] = updated
            if (userId == SupabaseClient.currentUser?.id) {
                SupabaseClient.currentProfile = updated
            }
            return@withContext Result.success(updated)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val requestBody = UpdateProfileRequest(
                displayName = displayName,
                avatarUrl = avatarUrl,
                isProfileComplete = isProfileComplete,
                firstName = firstName,
                lastName = lastName,
                status = status,
                birthDate = birthDate,
                sex = sex,
                interests = interests,
                coverUrl = coverUrl
            )
            
            try {
                val jsonString = com.example.data.supabase.SupabaseClient.moshi.adapter(UpdateProfileRequest::class.java).toJson(requestBody)
                Log.i(TAG, "DEBUG_REAL_JSON_TO_SEND: $jsonString")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to serialize JSON for debug", e)
            }

            val currentToken = SupabaseClient.currentToken
            val tokenPresent = !currentToken.isNullOrEmpty()
            val baseUrl = SupabaseClient.supabaseUrl
            val fullUrl = "${if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"}rest/v1/profiles?id=eq.$userId"

            Log.i(TAG, "UPDATE_PROFILE_USER_ID: $userId")
            Log.i(TAG, "UPDATE_PROFILE_BODY: $requestBody")
            Log.i(TAG, "UPDATE_PROFILE_TOKEN_PRESENT: $tokenPresent")
            Log.i(TAG, "[UPDATE_PROFILE_START]")
            Log.i(TAG, "URL completa usada: $fullUrl")
            Log.i(TAG, "USER_ID enviado: $userId")
            Log.i(TAG, "TOKEN_PRESENT = $tokenPresent")
            Log.i(TAG, "BODY JSON enviado: $requestBody")

            if (SupabaseClient.currentToken.isNullOrEmpty()) {
                Log.i(TAG, "Token is null in ProfilesRepository, attempting refresh before failure...")
                SessionManager.validateAndRefreshSessionIfNeeded()
            }

            val validToken = SupabaseClient.currentToken
            if (validToken.isNullOrEmpty()) {
                Log.e(TAG, "UPDATE_PROFILE_ERROR_BODY: Token is null or empty after validation/refresh. Profile update aborted.")
                return@withContext Result.failure(Exception("Error de sesión: El token de acceso es nulo o expiró. Intenta iniciar sesión de nuevo para continuar."))
            }
            var bearer = "Bearer $validToken"

            var response: retrofit2.Response<List<Profile>>? = null
            var capturedException: Exception? = null

            try {
                response = service.updateProfile(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = bearer,
                    idFilter = "eq.$userId",
                    profile = requestBody,
                    prefer = "return=representation"
                )
            } catch (e: Exception) {
                capturedException = e
                Log.e(TAG, "First call exception", e)
            }

            // Retry if token expired or if first try had an exception (potential connection retry)
            if ((response != null && response.code() == 401) || (capturedException != null && response == null)) {
                Log.i(TAG, "Unauthorized or exception on first attempt. Trying session refresh...")
                val refreshed = SessionManager.refreshSession()
                if (refreshed) {
                    val newToken = SupabaseClient.currentToken
                    if (!newToken.isNullOrEmpty()) {
                        bearer = "Bearer $newToken"
                        try {
                            response = service.updateProfile(
                                apiKey = SupabaseClient.supabaseAnonKey,
                                authorization = bearer,
                                idFilter = "eq.$userId",
                                profile = requestBody,
                                prefer = "return=representation"
                            )
                            capturedException = null // Clear exception on successful retry call
                        } catch (e: Exception) {
                            capturedException = e
                            Log.e(TAG, "Retry exception", e)
                        }
                    }
                }
            }

            if (response != null) {
                val httpCode = response.code()
                val httpMessage = response.message()
                val errorBodyStr = response.errorBody()?.string()

                Log.i(TAG, "UPDATE_PROFILE_RESPONSE_CODE: $httpCode")
                Log.i(TAG, "UPDATE_PROFILE_ERROR_BODY: $errorBodyStr")
                Log.i(TAG, "[UPDATE_PROFILE_HTTP_CODE]=$httpCode")
                Log.i(TAG, "[UPDATE_PROFILE_MESSAGE]=$httpMessage")
                Log.i(TAG, "[UPDATE_PROFILE_ERROR_BODY]=$errorBodyStr")

                if (response.isSuccessful) {
                    val list = response.body()
                    if (!list.isNullOrEmpty()) {
                        val updatedProfile = list[0]
                        if (userId == SupabaseClient.currentUser?.id) {
                            SupabaseClient.currentProfile = updatedProfile
                        }
                        // Persist to local DB
                        try {
                            val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
                            val existing = db.profileDao().getProfileById(updatedProfile.id)
                            val entity = com.example.data.database.ProfileEntity.fromProfile(updatedProfile).copy(
                                avatarLocalPath = avatarLocalPath ?: existing?.avatarLocalPath,
                                coverLocalPath = coverLocalPath ?: existing?.coverLocalPath,
                                lastSyncedAt = existing?.lastSyncedAt ?: System.currentTimeMillis()
                            )
                            db.profileDao().insertProfile(entity)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to persist updated profile to local DB", e)
                        }
                        Result.success(updatedProfile)
                    } else {
                        Log.i(TAG, "Update succeeded but returned empty representation (likely due to RLS). Reconstructing profile locally to ensure zero registration failure.")
                        val existingProfile = SupabaseClient.currentProfile ?: Profile(
                            id = userId,
                            displayName = displayName,
                            avatarUrl = avatarUrl,
                            isProfileComplete = isProfileComplete ?: false,
                            firstName = firstName,
                            lastName = lastName,
                            status = status ?: "active",
                            birthDate = birthDate,
                            sex = sex,
                            interests = interests ?: emptyList(),
                            coverUrl = coverUrl
                        )
                        val reconstructedProfile = existingProfile.copy(
                            displayName = displayName,
                            avatarUrl = avatarUrl ?: existingProfile.avatarUrl,
                            isProfileComplete = isProfileComplete ?: existingProfile.isProfileComplete,
                            firstName = firstName ?: existingProfile.firstName,
                            lastName = lastName ?: existingProfile.lastName,
                            status = status ?: existingProfile.status,
                            birthDate = birthDate ?: existingProfile.birthDate,
                            sex = sex ?: existingProfile.sex,
                            interests = interests ?: existingProfile.interests,
                            coverUrl = coverUrl ?: existingProfile.coverUrl
                        )
                        if (userId == SupabaseClient.currentUser?.id) {
                            SupabaseClient.currentProfile = reconstructedProfile
                        }
                        // Persist reconstructed to local DB
                        try {
                            val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
                            val existing = db.profileDao().getProfileById(reconstructedProfile.id)
                            val entity = com.example.data.database.ProfileEntity.fromProfile(reconstructedProfile).copy(
                                avatarLocalPath = avatarLocalPath ?: existing?.avatarLocalPath,
                                coverLocalPath = coverLocalPath ?: existing?.coverLocalPath,
                                lastSyncedAt = existing?.lastSyncedAt ?: System.currentTimeMillis()
                            )
                            db.profileDao().insertProfile(entity)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to persist reconstructed profile to local DB", e)
                        }
                        Result.success(reconstructedProfile)
                    }
                } else {
                    val fullErr = if (!errorBodyStr.isNullOrEmpty()) {
                        "HTTP $httpCode: $httpMessage - $errorBodyStr"
                    } else {
                        "HTTP $httpCode: $httpMessage"
                    }
                    Result.failure(Exception(fullErr))
                }
            } else {
                val exc = capturedException
                if (exc != null) {
                    val className = exc.javaClass.name
                    val msg = exc.message ?: ""
                    Log.e(TAG, "UPDATE_PROFILE_RESPONSE_CODE: null")
                    Log.e(TAG, "UPDATE_PROFILE_ERROR_BODY: Exception captured: $className: $msg")
                    Log.e(TAG, "Exception Class: $className")
                    Log.e(TAG, "Exception Message: $msg")
                    Log.e(TAG, "Exception StackTrace: ${Log.getStackTraceString(exc)}")

                    val detailedMessage = when {
                        exc is java.io.InterruptedIOException || exc is java.net.SocketTimeoutException -> {
                            "TimeoutException: Connection/Read timeout. Class: $className, Message: $msg"
                        }
                        exc is java.io.IOException -> {
                            "IOException: Network connectivity issue. Class: $className, Message: $msg"
                        }
                        exc is retrofit2.HttpException -> {
                            "HttpException: HTTP protocol error. Class: $className, Message: $msg"
                        }
                        else -> {
                            "UnexpectedException: Class: $className, Message: $msg"
                        }
                    }
                    Result.failure(Exception(detailedMessage, exc))
                } else {
                    Log.e(TAG, "UPDATE_PROFILE_RESPONSE_CODE: null")
                    Log.e(TAG, "UPDATE_PROFILE_ERROR_BODY: Response null with no captured exception")
                    Result.failure(Exception("Response null with no captured exception"))
                }
            }
        } catch (e: Exception) {
            val className = e.javaClass.name
            val msg = e.message ?: ""
            Log.e(TAG, "Outer Exception in updateProfile", e)
            Log.e(TAG, "UPDATE_PROFILE_RESPONSE_CODE: null")
            Log.e(TAG, "UPDATE_PROFILE_ERROR_BODY: Outer Exception: $className: $msg")
            Log.e(TAG, "Exception Class: $className")
            Log.e(TAG, "Exception Message: $msg")
            Log.e(TAG, "Exception StackTrace: ${Log.getStackTraceString(e)}")
            Result.failure(e)
        }
    }

    suspend fun searchProfiles(query: String): Result<List<Profile>> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("Not authenticated"))
        if (!SupabaseClient.isConfigured) {
            delay(500)
            val results = SupabaseClient.demoProfiles.values.filter {
                it.displayName.contains(query, ignoreCase = true) && it.id != currentUid
            }
            return@withContext Result.success(results)
        }

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val apiKey = SupabaseClient.supabaseAnonKey
            
            val contactsResponse = runCall { b -> service.getContacts(apiKey, b, ownerFilter = "eq.$currentUid") }
            if (contactsResponse != null && contactsResponse.isSuccessful) {
                val contacts = contactsResponse.body() ?: emptyList()
                val contactIds = contacts.map { it.contactUserId }.toSet()

                val response = runCall { b -> service.getProfiles(apiKey, b, "*") }
                if (response != null && response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    val filtered = list.filter {
                        contactIds.contains(it.id) && it.displayName.contains(query, ignoreCase = true) && it.id != currentUid
                    }
                    Result.success(filtered)
                } else {
                    Result.failure(Exception(response?.errorBody()?.string() ?: "Error de búsqueda"))
                }
            } else {
                Result.failure(Exception(contactsResponse?.errorBody()?.string() ?: "Error de contactos"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyContactIdentifier(): Result<ContactIdentifierResponse> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b ->
                service.getMyContactIdentifier(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b
                )
            }
            if (response != null && response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errStr = response?.errorBody()?.string() ?: "Error al obtener identificador de contacto"
                Result.failure(Exception(errStr))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en getMyContactIdentifier: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getMyContacts(forceRefresh: Boolean = false): Result<List<Profile>> = withContext(Dispatchers.IO) {
        val currentUid = SupabaseClient.currentUser?.id ?: run {
            Log.d("CONTACTS_DEBUG", "[CONTACTS_DEBUG_START]")
            Log.d("CONTACTS_DEBUG", "currentUserId: NULL (Not authenticated)")
            Log.d("CONTACTS_DEBUG", "[CONTACTS_DEBUG_END]")
            return@withContext Result.failure(Exception("Not authenticated"))
        }

        Log.d("CONTACTS_DEBUG", "[CONTACTS_DEBUG_START]")
        Log.d("CONTACTS_DEBUG", "currentUserId: $currentUid")
        Log.d("CONTACTS_DEBUG", "forceRefresh: $forceRefresh")

        if (!forceRefresh) {
            val cached = SessionManager.getCacheList("cached_contacts", Profile::class.java)
            if (cached.isNotEmpty()) {
                Log.d(TAG, "getMyContacts: Returning ${cached.size} cached contacts")
                Log.d("CONTACTS_DEBUG", "Returned from cache! Cached contacts count: ${cached.size}")
                for (contact in cached) {
                    val finalAvatarUrl = CdnManager.resolveAvatarUrl(contact.avatarUrl)
                    Log.d("CONTACTS_DEBUG", "Cached contact detail -> contact_user_id: ${contact.id}, resolvedDisplayName: ${contact.displayName}, raw avatar_url: ${contact.avatarUrl}, avatar URL final: $finalAvatarUrl")
                }
                Log.d("CONTACTS_DEBUG", "cantidad final entregada al ViewModel: ${cached.size}")
                Log.d("CONTACTS_DEBUG", "[CONTACTS_DEBUG_END]")
                return@withContext Result.success(cached)
            }
        }

        if (!SupabaseClient.isConfigured) {
            delay(500)
            val results = SupabaseClient.demoProfiles.values.filter { it.id != currentUid }
            Log.d("CONTACTS_DEBUG", "Supabase not configured, returning demo profiles count: ${results.size}")
            Log.d("CONTACTS_DEBUG", "[CONTACTS_DEBUG_END]")
            return@withContext Result.success(results)
        }
        try {
            val service = SupabaseClient.apiService ?: run {
                Log.d("CONTACTS_DEBUG", "Supabase apiService is NULL")
                Log.d("CONTACTS_DEBUG", "[CONTACTS_DEBUG_END]")
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            val apiKey = SupabaseClient.supabaseAnonKey

            val response = runCall { b -> service.getContacts(apiKey, b, select = "*", ownerFilter = "eq.$currentUid") }
            val statusCode = response?.code() ?: -1
            val isSuccess = response != null && response.isSuccessful
            Log.d("CONTACTS_DEBUG", "HTTP status de la petición contacts: $statusCode")

            if (isSuccess) {
                val contacts = response!!.body() ?: emptyList()
                Log.d(TAG, "getMyContacts: Fetched ${contacts.size} contacts from server")
                Log.d("CONTACTS_DEBUG", "cantidad de contactos recibidos de Supabase: ${contacts.size}")

                val targetUserIds = contacts.map { it.contactUserId }.filter { it.isNotBlank() }.distinct()

                val publicProfileRepo = PublicProfileRepository.getInstance()
                val fetchResult = publicProfileRepo.getPublicProfiles(targetUserIds, forceRefresh = forceRefresh)

                val publicProfilesMap = when (fetchResult) {
                    is PublicProfileFetchResult.Success -> fetchResult.data
                    else -> emptyMap()
                }

                val contactProfiles = contacts.map { contact ->
                    val pub = publicProfilesMap[contact.contactUserId]
                    val displayName = pub?.displayName ?: pub?.firstName ?: contact.contactUserId
                    val avatarUrl = CdnManager.resolveAvatarUrl(pub?.avatarUrl)

                    Profile(
                        id = contact.contactUserId,
                        displayName = displayName,
                        firstName = pub?.firstName,
                        lastName = pub?.lastName,
                        avatarUrl = avatarUrl
                    )
                }

                Log.d("CONTACTS_DEBUG", "cantidad final entregada al ViewModel: ${contactProfiles.size}")
                Log.d("CONTACTS_DEBUG", "[CONTACTS_DEBUG_END]")

                SessionManager.saveCacheList("cached_contacts", contactProfiles, Profile::class.java)
                SessionManager.setOffline(false)
                Result.success(contactProfiles)
            } else {
                val errBody = response?.errorBody()?.string() ?: ""
                Log.e(TAG, "getMyContacts: Failed to fetch contacts, code: $statusCode, error: $errBody")
                Log.d("CONTACTS_DEBUG", "errorBody completo si HTTP != 2xx: $errBody")
                Log.d("CONTACTS_DEBUG", "cantidad de contactos recibidos de Supabase: 0")
                Log.d("CONTACTS_DEBUG", "cantidad final entregada al ViewModel: 0")

                val cached = SessionManager.getCacheList("cached_contacts", Profile::class.java)
                if (cached.isNotEmpty()) {
                    Log.i(TAG, "Request failed, returning cached contacts")
                    Log.d("CONTACTS_DEBUG", "Returned ${cached.size} cached contacts on failure")
                    Log.d("CONTACTS_DEBUG", "[CONTACTS_DEBUG_END]")
                    SessionManager.setOffline(true)
                    return@withContext Result.success(cached)
                }
                Log.d("CONTACTS_DEBUG", "[CONTACTS_DEBUG_END]")
                Result.failure(Exception("Error loading contacts: $errBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getMyContacts exception", e)
            Log.d("CONTACTS_DEBUG", "Exception in getMyContacts: ${e.message}")
            val cached = SessionManager.getCacheList("cached_contacts", Profile::class.java)
            if (cached.isNotEmpty()) {
                Log.d("CONTACTS_DEBUG", "Returned ${cached.size} cached contacts on exception")
                Log.d("CONTACTS_DEBUG", "[CONTACTS_DEBUG_END]")
                return@withContext Result.success(cached)
            }
            Log.d("CONTACTS_DEBUG", "[CONTACTS_DEBUG_END]")
            Result.failure(e)
        }
    }

    suspend fun addContactByPin(pin: String): Result<AddContactByPinResponse> = withContext(Dispatchers.IO) {
        val cleanIdentifier = pin.trim()
        
        if (!SupabaseClient.isConfigured) {
            delay(1000)
            val currentUid = SupabaseClient.currentUser?.id ?: ""
            val targetId = when (cleanIdentifier) {
                "111111" -> "user_yonaiker"
                "222222" -> "user_gabriel"
                "333333" -> "user_maria"
                else -> "user_yonaiker"
            }

            if (targetId == currentUid) {
                return@withContext Result.failure(Exception("No puedes agregarte a ti mismo como contacto de Pana."))
            }

            val targetProfile = SupabaseClient.demoProfiles[targetId] ?: return@withContext Result.failure(Exception("Perfil no encontrado."))
            val threadId = if (targetId == "user_gabriel") "chat_2" else "chat_new_${targetId}"
            
            val currentContacts = SessionManager.getCacheList("cached_contacts", Profile::class.java).toMutableList()
            val alreadyExists = currentContacts.any { it.id == targetProfile.id }
            if (!alreadyExists) {
                currentContacts.add(targetProfile)
                SessionManager.saveCacheList("cached_contacts", currentContacts, Profile::class.java)
            }

            return@withContext Result.success(
                AddContactByPinResponse(
                    success = true,
                    contactId = targetProfile.id,
                    displayName = targetProfile.displayName,
                    avatarUrl = targetProfile.avatarUrl,
                    threadId = threadId,
                    isAlreadyContact = alreadyExists
                )
            )
        }

        Log.d(TAG, "addContactByPin: Enviando identificador directamente a RPC. Identifier: $cleanIdentifier")

        val isPin = cleanIdentifier.matches(Regex("^[0-9]{6}$"))

        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            
            val response = runCall { b ->
                if (isPin) {
                    service.sendFriendRequestByPin(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = b,
                        params = mapOf("p_to_pin_raw" to cleanIdentifier)
                    )
                } else {
                    service.sendFriendRequestByQr(
                        apiKey = SupabaseClient.supabaseAnonKey,
                        authorization = b,
                        params = mapOf("p_qr_token" to cleanIdentifier)
                    )
                }
            }

            if (response != null && response.isSuccessful) {
                val rawBody = response.body()?.string() ?: ""
                Log.d(TAG, "send_friend_request RPC éxito raw response: $rawBody")
                
                val friendRequestId = rawBody.trim().removeSurrounding("\"")
                
                val resultObj = AddContactByPinResponse(
                    success = true,
                    contactId = friendRequestId,
                    displayName = "Solicitud enviada",
                    avatarUrl = null,
                    threadId = friendRequestId,
                    isAlreadyContact = false
                )
                
                Result.success(resultObj)
            } else {
                val errStr = response?.errorBody()?.string() ?: ""
                Log.e(TAG, "send_friend_request RPC falló: Código=${response?.code()}, Error=$errStr")
                val cleanedMsg = when {
                    errStr.contains("PIN inválido") || errStr.contains("no encontrado") || errStr.contains("inválido") -> "Identidad o PIN inválido o no encontrado"
                    errStr.contains("mismo") || errStr.contains("ti mismo") -> "No puedes enviarte solicitud a ti mismo"
                    errStr.contains("ya es") || errStr.contains("duplicate key") || errStr.contains("already exists") -> "Este usuario ya es un contacto o tiene una solicitud activa"
                    else -> "Identidad o PIN inválido o no encontrado"
                }
                Result.failure(Exception(cleanedMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception general en addContactByPin: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun removeContact(contactUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            val currentContacts = SessionManager.getCacheList("cached_contacts", Profile::class.java).toMutableList()
            currentContacts.removeAll { it.id == contactUserId }
            SessionManager.saveCacheList("cached_contacts", currentContacts, Profile::class.java)
            return@withContext Result.success(true)
        }
        
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b ->
                service.deleteContact(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    contactUserId = contactUserId
                )
            }
            if (response != null && response.isSuccessful) {
                // Actualizar cache local
                val currentContacts = SessionManager.getCacheList("cached_contacts", Profile::class.java).toMutableList()
                currentContacts.removeAll { it.id == contactUserId }
                SessionManager.saveCacheList("cached_contacts", currentContacts, Profile::class.java)
                
                Result.success(true)
            } else {
                val errMsg = response?.errorBody()?.string() ?: "Error eliminando contacto"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing contact", e)
            Result.failure(e)
        }
    }

    suspend fun uploadProfileImage(mediaFile: java.io.File, mediaMimeType: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Subiendo foto de perfil al CDN dinámico de Panalink")
            val currentUid = SupabaseClient.currentUser?.id ?: "anonymous"
            val result = UploadRepository().uploadVideo(mediaFile, mediaMimeType, "Profile Photo", currentUid)
            if (result.isSuccess) {
                Result.success(result.getOrThrow().url)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("La subida al CDN dinámico falló"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Excepción subiendo foto de perfil vía CDN dinámico", e)
            Result.failure(e)
        } finally {
            try {
                if (mediaFile.exists()) {
                    mediaFile.delete()
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error deleting temp profile photo file", ex)
            }
        }
    }

    suspend fun updateDeviceFingerprint(userId: String, fingerprint: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            Log.d(TAG, "[Demo Mode] Updated device fingerprint for $userId to $fingerprint")
            return@withContext Result.success(true)
        }
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val body = mapOf("device_fingerprint" to fingerprint)
            val response = runCall { b -> service.updateProfileMap(SupabaseClient.supabaseAnonKey, b, "eq.$userId", body) }
            if (response != null && response.isSuccessful) {
                Log.d(TAG, "Successfully updated device fingerprint for $userId")
                Result.success(true)
            } else {
                val errMsg = response?.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to update device fingerprint: $errMsg")
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception updating device fingerprint", e)
            Result.failure(e)
        }
    }

    suspend fun saveFcmTokenToEdgeFunction(userId: String, token: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            Log.d(TAG, "[Demo Mode] Called saveFcmTokenToEdgeFunction for $userId with token $token")
            return@withContext Result.success(true)
        }
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            
            val baseUrl = SupabaseClient.supabaseUrl
            val projectRef = if (baseUrl.contains(".supabase.co")) {
                baseUrl.substringAfter("https://").substringBefore(".supabase.co")
            } else {
                "tivqjfgjdxgzicrridaz"
            }
            val edgeFunctionUrl = "https://$projectRef.functions.supabase.co/save-token"
            
            val body = mapOf(
                "user_id" to userId,
                "token" to token,
                "platform" to "android"
            )
            
            val authHeader = if (!SupabaseClient.currentToken.isNullOrEmpty()) {
                "Bearer ${SupabaseClient.currentToken}"
            } else {
                "Bearer ${SupabaseClient.supabaseAnonKey}"
            }

            Log.d(TAG, "Calling save-token edge function at $edgeFunctionUrl for user $userId")
            val response = service.callEdgeFunction(
                url = edgeFunctionUrl,
                apiKey = SupabaseClient.supabaseAnonKey,
                authorization = authHeader,
                body = body
            )
            
            if (response.isSuccessful) {
                Log.d(TAG, "Successfully saved FCM token to Supabase Edge Function")
                Result.success(true)
            } else {
                val errMsg = response.errorBody()?.string() ?: "Unknown error"
                Log.e(TAG, "Failed to save FCM token to Supabase Edge Function: $errMsg (code=${response.code()})")
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling save-token edge function", e)
            Result.failure(e)
        }
    }

    companion object {
        val demoFollowers = mutableListOf<Pair<String, String>>(
            "me_demo_id" to "user_gabriel",
            "user_yonaiker" to "user_maria",
            "user_maria" to "user_gabriel",
            "user_gabriel" to "me_demo_id"
        )
    }

    suspend fun isFollowing(followerId: String, followedId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            val following = demoFollowers.any { it.first == followerId && it.second == followedId }
            return@withContext Result.success(following)
        }
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b ->
                service.getFollowers(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    select = "*",
                    followerIdFilter = "eq.$followerId",
                    followedIdFilter = "eq.$followedId"
                )
            }
            if (response != null && response.isSuccessful) {
                val list = response.body() ?: emptyList()
                Result.success(list.isNotEmpty())
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e("ProfilesRepository", "Error checking isFollowing", e)
            Result.failure(e)
        }
    }

    suspend fun followUser(followerId: String, followedId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            if (!demoFollowers.any { it.first == followerId && it.second == followedId }) {
                demoFollowers.add(followerId to followedId)
            }
            return@withContext Result.success(true)
        }
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val body = mapOf(
                "follower_id" to followerId,
                "followed_id" to followedId,
                "created_at" to SupabaseClient.getNowIsoString()
            )
            val response = runCall { b ->
                service.followUser(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    body = body
                )
            }
            if (response != null && response.isSuccessful) {
                try {
                    com.example.notification.engine.producers.social.FollowNotificationAdapter.publishFollowedYou(
                        targetUserId = followedId,
                        actorId = followerId,
                        actorName = followerId
                    )
                } catch (e: Exception) {
                    Log.e("ProfilesRepository", "Error emitting follow event", e)
                }
                Result.success(true)
            } else {
                val errMsg = response?.errorBody()?.string() ?: "Error de red"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e("ProfilesRepository", "Error following user", e)
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(followerId: String, followedId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            demoFollowers.removeAll { it.first == followerId && it.second == followedId }
            return@withContext Result.success(true)
        }
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b ->
                service.unfollowUser(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    followerIdFilter = "eq.$followerId",
                    followedIdFilter = "eq.$followedId"
                )
            }
            if (response != null && response.isSuccessful) {
                Result.success(true)
            } else {
                val errMsg = response?.errorBody()?.string() ?: "Error de red"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e("ProfilesRepository", "Error unfollowing user", e)
            Result.failure(e)
        }
    }

    suspend fun getFollowersList(userId: String): Result<List<com.example.data.model.FollowerDto>> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            val list = demoFollowers.filter { it.second == userId }.map { com.example.data.model.FollowerDto(it.first, it.second) }
            return@withContext Result.success(list)
        }
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b ->
                service.getFollowers(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    select = "*",
                    followedIdFilter = "eq.$userId"
                )
            }
            if (response != null && response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                val errMsg = response?.errorBody()?.string() ?: "Error de red"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e("ProfilesRepository", "Error getting followers", e)
            Result.failure(e)
        }
    }

    suspend fun getFollowingList(userId: String): Result<List<com.example.data.model.FollowerDto>> = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            val list = demoFollowers.filter { it.first == userId }.map { com.example.data.model.FollowerDto(it.first, it.second) }
            return@withContext Result.success(list)
        }
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b ->
                service.getFollowers(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    select = "*",
                    followerIdFilter = "eq.$userId"
                )
            }
            if (response != null && response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                val errMsg = response?.errorBody()?.string() ?: "Error de red"
                Result.failure(Exception(errMsg))
            }
        } catch (e: Exception) {
            Log.e("ProfilesRepository", "Error getting following list", e)
            Result.failure(e)
        }
    }

    suspend fun getPendingFriendRequests(): Result<List<FriendRequestEntity>> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b ->
                service.getFriendRequests(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    receiverFilter = "eq.${SupabaseClient.currentUser?.id}"
                )
            }
            if (response != null && response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Error getting friend requests"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting friend requests", e)
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b ->
                service.acceptFriendRequest(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    body = mapOf("p_request_id" to requestId)
                )
            }
            if (response != null && response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error accepting friend request"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error accepting friend request", e)
            Result.failure(e)
        }
    }

    suspend fun declineFriendRequest(requestId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b ->
                service.declineFriendRequest(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    body = mapOf("p_request_id" to requestId)
                )
            }
            if (response != null && response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error declining friend request"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error declining friend request", e)
            Result.failure(e)
        }
    }

    suspend fun updatePrivacyLevel(newLevel: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val userId = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("User not authenticated"))
            val response = runCall { b ->
                service.updateProfileMap(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    idFilter = "eq.$userId",
                    profile = mapOf("privacy_level" to newLevel)
                )
            }
            if (response != null && response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Error updating privacy level"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating privacy level", e)
            Result.failure(e)
        }
    }

    suspend fun sendFriendRequest(receiverId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.failure(Exception("Supabase not configured"))
            val response = runCall { b ->
                service.sendFriendRequest(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = b,
                    body = mapOf("p_receiver_id" to receiverId)
                )
            }
            if (response != null && response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = response?.errorBody()?.string() ?: "Error sending friend request"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending friend request", e)
            Result.failure(e)
        }
    }

    private val localBlockedCache = java.util.concurrent.ConcurrentHashMap<String, Boolean>()

    suspend fun fetchBlockedUsers(): Result<List<String>> = withContext(Dispatchers.IO) {
        val currentUid = com.example.data.supabase.SupabaseClient.currentUser?.id
            ?: return@withContext Result.success(emptyList())
        if (!SupabaseClient.isConfigured) {
            return@withContext Result.success(localBlockedCache.filter { it.value }.keys.toList())
        }
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(emptyList())
            val response = runCall { auth ->
                service.getBlockedUsers(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = auth,
                    userIdFilter = "eq.$currentUid"
                )
            }
            if (response != null && response.isSuccessful) {
                val list = response.body() ?: emptyList()
                localBlockedCache.clear()
                val blockedIds = list.map { it.blockedUserId }
                blockedIds.forEach { id -> localBlockedCache[id] = true }
                Result.success(blockedIds)
            } else {
                Result.success(localBlockedCache.filter { it.value }.keys.toList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching blocked users: ${e.localizedMessage}", e)
            Result.failure(e)
        }
    }

    suspend fun blockUser(targetUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        localBlockedCache[targetUserId] = true
        val currentUid = com.example.data.supabase.SupabaseClient.currentUser?.id
            ?: return@withContext Result.success(true)
        if (!SupabaseClient.isConfigured) {
            return@withContext Result.success(true)
        }
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(true)
            val response = runCall { auth ->
                service.blockUserApi(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = auth,
                    body = mapOf("user_id" to currentUid, "blocked_user_id" to targetUserId)
                )
            }
            Result.success(response?.isSuccessful == true)
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking user remotely: ${e.localizedMessage}", e)
            Result.success(true)
        }
    }

    suspend fun unblockUser(targetUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        localBlockedCache[targetUserId] = false
        val currentUid = com.example.data.supabase.SupabaseClient.currentUser?.id
            ?: return@withContext Result.success(true)
        if (!SupabaseClient.isConfigured) {
            return@withContext Result.success(true)
        }
        try {
            val service = SupabaseClient.apiService ?: return@withContext Result.success(true)
            val response = runCall { auth ->
                service.unblockUserApi(
                    apiKey = SupabaseClient.supabaseAnonKey,
                    authorization = auth,
                    userIdFilter = "eq.$currentUid",
                    blockedUserIdFilter = "eq.$targetUserId"
                )
            }
            Result.success(response?.isSuccessful == true)
        } catch (e: Exception) {
            Log.e(TAG, "Error unblocking user remotely: ${e.localizedMessage}", e)
            Result.success(true)
        }
    }

    fun isUserBlocked(targetUserId: String): Boolean {
        return localBlockedCache[targetUserId] == true
    }
}