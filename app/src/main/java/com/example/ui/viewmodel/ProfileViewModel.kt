package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ContactIdentifierResponse
import com.example.data.model.Profile
import com.example.data.model.UserStateWithUser
import com.example.data.repository.ProfilesRepository
import com.example.data.repository.StatesRepository
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    object Idle : ProfileUiState()
    object Loading : ProfileUiState()
    data class Success(val profile: Profile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

sealed class SaveProfileUiState {
    object Idle : SaveProfileUiState()
    object Loading : SaveProfileUiState()
    object Success : SaveProfileUiState()
    data class Error(val message: String) : SaveProfileUiState()
}

class ProfileViewModel(
    private val profilesRepository: ProfilesRepository = ProfilesRepository(),
    private val privacyRepository: com.example.data.repository.PrivacyRepository = com.example.data.repository.PrivacyRepository()
) : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileUiState>(ProfileUiState.Idle)
    val profileState: StateFlow<ProfileUiState> = _profileState

    private val _saveState = MutableStateFlow<SaveProfileUiState>(SaveProfileUiState.Idle)
    val saveState: StateFlow<SaveProfileUiState> = _saveState

    // --- Privacy & Premium Settings State ---
    private val _entitlements = MutableStateFlow<List<com.example.data.model.UserEntitlementDto>>(emptyList())
    val entitlements: StateFlow<List<com.example.data.model.UserEntitlementDto>> = _entitlements

    private val _privacySettings = MutableStateFlow<List<com.example.data.model.UserPrivacySettingDto>>(emptyList())
    val privacySettings: StateFlow<List<com.example.data.model.UserPrivacySettingDto>> = _privacySettings
    
    private val _presenceSessions = MutableStateFlow<List<com.example.data.model.PresenceSession>>(emptyList())
    val presenceSessions: StateFlow<List<com.example.data.model.PresenceSession>> = _presenceSessions

    val currentUserId: String
        get() = com.example.data.supabase.SupabaseClient.currentUser?.id ?: "me_demo_id"

    val isConfigured: Boolean
        get() = com.example.data.supabase.SupabaseClient.isConfigured
    
    init {
        loadPrivacyData()
        loadPresenceSessions()
    }
    
    fun loadPresenceSessions() {
        viewModelScope.launch {
            val userId = com.example.data.supabase.SupabaseClient.currentUser?.id
            if (userId != null) {
                try {
                    val api = com.example.data.supabase.SupabaseClient.apiService
                    val token = com.example.data.supabase.SupabaseClient.currentToken
                    if (api != null && token != null) {
                        val response = api.getPresenceSessions(
                            apiKey = com.example.data.supabase.SupabaseClient.supabaseAnonKey,
                            authorization = "Bearer $token",
                            userIdFilter = "eq.$userId"
                        )
                        if (response.isSuccessful) {
                            _presenceSessions.value = response.body() ?: emptyList()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun loadPrivacyData() {
        viewModelScope.launch {
            val ents = privacyRepository.getEntitlements()
            if (ents.isSuccess) {
                _entitlements.value = ents.getOrNull() ?: emptyList()
            }
            val sets = privacyRepository.getPrivacySettings()
            if (sets.isSuccess) {
                _privacySettings.value = sets.getOrNull() ?: emptyList()
            }
        }
    }

    fun togglePrivacySetting(featureCode: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val currentSettings = _privacySettings.value.toMutableList()
            val existingIndex = currentSettings.indexOfFirst { it.featureCode == featureCode }
            
            val newValue = mapOf("enabled" to isEnabled)
            if (existingIndex >= 0) {
                currentSettings[existingIndex] = currentSettings[existingIndex].copy(value = newValue)
            } else {
                currentSettings.add(
                    com.example.data.model.UserPrivacySettingDto(
                        userId = com.example.data.supabase.SupabaseClient.currentUser?.id ?: "",
                        featureCode = featureCode,
                        value = newValue
                    )
                )
            }
            _privacySettings.value = currentSettings
            privacyRepository.updatePrivacySetting(featureCode, newValue)
        }
    }

    private val _reelsState = MutableStateFlow<Result<List<UserStateWithUser>>>(Result.success(emptyList()))
    val reelsState: StateFlow<Result<List<UserStateWithUser>>> = _reelsState

    private val _savedState = MutableStateFlow<Result<List<UserStateWithUser>>>(Result.success(emptyList()))
    val savedState: StateFlow<Result<List<UserStateWithUser>>> = _savedState

    private val _followersCount = MutableStateFlow(0)
    val followersCount: StateFlow<Int> = _followersCount

    private val _followingCount = MutableStateFlow(0)
    val followingCount: StateFlow<Int> = _followingCount

    private val _totalLikesCount = MutableStateFlow(0)
    val totalLikesCount: StateFlow<Int> = _totalLikesCount

    fun deleteReel(stateId: String, userId: String) {
        val reelsList = _reelsState.value.getOrNull() ?: emptyList()
        val match = reelsList.find { it.state.id == stateId }
        val mediaUrl = match?.state?.mediaUrl
        viewModelScope.launch {
            val result = StatesRepository().deleteUserStatus(stateId, isReel = true, mediaUrl = mediaUrl)
            if (result.isSuccess) {
                loadReels(userId)
            }
        }
    }

    fun loadStats(userId: String) {
        viewModelScope.launch {
            val followers = ProfilesRepository().getFollowersList(userId).getOrNull() ?: emptyList()
            _followersCount.value = followers.size

            val following = ProfilesRepository().getFollowingList(userId).getOrNull() ?: emptyList()
            _followingCount.value = following.size
            
            // For likes, sum the likes_count of each reel/status
            val reels = StatesRepository().getUserReels(userId).getOrNull() ?: emptyList()
            val totalLikes = reels.sumOf { it.state.likesCount ?: 0 }
            _totalLikesCount.value = totalLikes
        }
    }

    fun loadSavedContent() {
        viewModelScope.launch {
            _savedState.value = StatesRepository().getSavedStates()
        }
    }

    fun loadReels(userId: String) {
        viewModelScope.launch {
            _reelsState.value = StatesRepository().getUserReels(userId)
            loadSavedContent()
            loadStats(userId)
        }
    }

    private val _contactIdentifierState = MutableStateFlow<ContactIdentifierResponse?>(null)
    val contactIdentifierState: StateFlow<ContactIdentifierResponse?> = _contactIdentifierState

    fun loadProfile() {
        val currentUid = SupabaseClient.currentUser?.id
        val cachedProfile = com.example.data.supabase.SessionManager.getCachedProfile() ?: SupabaseClient.currentProfile

        if (currentUid == null && cachedProfile == null) {
            _profileState.value = ProfileUiState.Error("Usuario no autenticado")
            return
        }

        if (cachedProfile != null) {
            _profileState.value = ProfileUiState.Success(cachedProfile)
        } else {
            _profileState.value = ProfileUiState.Loading
        }

        val targetUid = currentUid ?: cachedProfile?.id ?: return

        viewModelScope.launch {
            profilesRepository.getProfile(targetUid)
                .onSuccess { profile ->
                    _profileState.value = ProfileUiState.Success(profile)
                }
                .onFailure { error ->
                    if (_profileState.value !is ProfileUiState.Success) {
                        _profileState.value = ProfileUiState.Error(error.localizedMessage ?: "Error al cargar perfil")
                    }
                }

            profilesRepository.getMyContactIdentifier()
                .onSuccess { identifier ->
                    _contactIdentifierState.value = identifier
                    val currentProf = (_profileState.value as? ProfileUiState.Success)?.profile
                    if (currentProf != null) {
                        _profileState.value = ProfileUiState.Success(currentProf.copy(pin = identifier.pin))
                    }
                }
        }
    }

    fun saveProfile(
        displayName: String,
        avatarUrl: String?,
        firstName: String? = null,
        lastName: String? = null,
        status: String? = null,
        birthDate: String? = null,
        sex: String? = null,
        interests: List<String>? = null,
        coverUrl: String? = null,
        avatarLocalPath: String? = null,
        coverLocalPath: String? = null
    ) {
        val currentUid = SupabaseClient.currentUser?.id ?: return
        if (displayName.isBlank()) {
            _saveState.value = SaveProfileUiState.Error("El nombre no puede estar vacío.")
            return
        }

        _saveState.value = SaveProfileUiState.Loading
        viewModelScope.launch {
            profilesRepository.updateProfile(
                userId = currentUid,
                displayName = displayName,
                avatarUrl = avatarUrl,
                firstName = firstName,
                lastName = lastName,
                status = status,
                birthDate = birthDate,
                sex = sex,
                interests = interests,
                coverUrl = coverUrl,
                avatarLocalPath = avatarLocalPath,
                coverLocalPath = coverLocalPath
            )
                .onSuccess { updatedProfile ->
                    _profileState.value = ProfileUiState.Success(updatedProfile)
                    _saveState.value = SaveProfileUiState.Success
                }
                .onFailure { error ->
                    _saveState.value = SaveProfileUiState.Error(error.localizedMessage ?: "Error al guardar perfil")
                }
        }
    }

    fun uploadProfilePhoto(context: android.content.Context, file: java.io.File, mimeType: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val uploadId = java.util.UUID.randomUUID().toString()
        val currentUser = com.example.data.supabase.SupabaseClient.currentUser
        val userId = currentUser?.id ?: ""

        viewModelScope.launch {
            try {
                val pendingUpload = com.example.data.database.PendingUploadEntity(
                    id = uploadId,
                    userId = userId,
                    uploadType = "PROFILE",
                    localFilePath = file.absolutePath,
                    mimeType = mimeType,
                    status = "pending"
                )
                
                val db = com.example.data.database.PanalinkDatabase.getDatabase(context)
                db.pendingUploadDao().insertUpload(pendingUpload)
                
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
                    
                val inputData = androidx.work.workDataOf("uploadId" to uploadId)
                
                val uploadWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.worker.SocialMediaUploadWorker>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .addTag("social_upload")
                    .addTag("upload_$uploadId")
                    .addTag("social_upload_$uploadId")
                    .setBackoffCriteria(
                        androidx.work.BackoffPolicy.EXPONENTIAL,
                        androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                    )
                    .build()
                    
                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                    "social_upload_$uploadId",
                    androidx.work.ExistingWorkPolicy.KEEP,
                    uploadWorkRequest
                )

                val workManager = androidx.work.WorkManager.getInstance(context)
                val liveData = workManager.getWorkInfoByIdLiveData(uploadWorkRequest.id)
                val observer = object : androidx.lifecycle.Observer<androidx.work.WorkInfo> {
                    override fun onChanged(value: androidx.work.WorkInfo) {
                        when (value.state) {
                            androidx.work.WorkInfo.State.SUCCEEDED -> {
                                viewModelScope.launch {
                                    val entity = db.pendingUploadDao().getUploadById(uploadId)
                                    val remoteUrl = entity?.remoteUrl ?: ""
                                    onSuccess(remoteUrl)
                                }
                                liveData.removeObserver(this)
                            }
                            androidx.work.WorkInfo.State.FAILED -> {
                                viewModelScope.launch {
                                    val entity = db.pendingUploadDao().getUploadById(uploadId)
                                    val errorMsg = entity?.errorMessage ?: "Error al subir la imagen"
                                    onFailure(errorMsg)
                                }
                                liveData.removeObserver(this)
                            }
                            else -> Unit
                        }
                    }
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    liveData.observeForever(observer)
                }
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Fallo al iniciar el worker de subida de perfil")
            }
        }
    }

    fun uploadCoverPhoto(context: android.content.Context, file: java.io.File, mimeType: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        val uploadId = java.util.UUID.randomUUID().toString()
        val currentUser = com.example.data.supabase.SupabaseClient.currentUser
        val userId = currentUser?.id ?: ""

        viewModelScope.launch {
            try {
                val pendingUpload = com.example.data.database.PendingUploadEntity(
                    id = uploadId,
                    userId = userId,
                    uploadType = "PROFILE_COVER",
                    localFilePath = file.absolutePath,
                    mimeType = mimeType,
                    status = "pending"
                )
                
                val db = com.example.data.database.PanalinkDatabase.getDatabase(context)
                db.pendingUploadDao().insertUpload(pendingUpload)
                
                val constraints = androidx.work.Constraints.Builder()
                    .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                    .build()
                    
                val inputData = androidx.work.workDataOf("uploadId" to uploadId)
                
                val uploadWorkRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.worker.SocialMediaUploadWorker>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .addTag("social_upload")
                    .addTag("upload_$uploadId")
                    .addTag("social_upload_$uploadId")
                    .setBackoffCriteria(
                        androidx.work.BackoffPolicy.EXPONENTIAL,
                        androidx.work.WorkRequest.MIN_BACKOFF_MILLIS,
                        java.util.concurrent.TimeUnit.MILLISECONDS
                    )
                    .build()
                    
                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                    "social_upload_$uploadId",
                    androidx.work.ExistingWorkPolicy.KEEP,
                    uploadWorkRequest
                )

                val workManager = androidx.work.WorkManager.getInstance(context)
                val liveData = workManager.getWorkInfoByIdLiveData(uploadWorkRequest.id)
                val observer = object : androidx.lifecycle.Observer<androidx.work.WorkInfo> {
                    override fun onChanged(value: androidx.work.WorkInfo) {
                        when (value.state) {
                            androidx.work.WorkInfo.State.SUCCEEDED -> {
                                viewModelScope.launch {
                                    val entity = db.pendingUploadDao().getUploadById(uploadId)
                                    val remoteUrl = entity?.remoteUrl ?: ""
                                    onSuccess(remoteUrl)
                                }
                                liveData.removeObserver(this)
                            }
                            androidx.work.WorkInfo.State.FAILED -> {
                                viewModelScope.launch {
                                    val entity = db.pendingUploadDao().getUploadById(uploadId)
                                    val errorMsg = entity?.errorMessage ?: "Error al subir la portada"
                                    onFailure(errorMsg)
                                }
                                liveData.removeObserver(this)
                            }
                            else -> Unit
                        }
                    }
                }
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    liveData.observeForever(observer)
                }
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Fallo al iniciar el worker de subida de portada")
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveProfileUiState.Idle
    }

    fun updatePrivacy(level: String) {
        viewModelScope.launch {
            _saveState.value = SaveProfileUiState.Loading
            profilesRepository.updatePrivacyLevel(level)
                .onSuccess {
                    _saveState.value = SaveProfileUiState.Success
                }
                .onFailure { e ->
                    _saveState.value = SaveProfileUiState.Error(e.message ?: "Error actualizando privacidad")
                }
        }
    }

    fun resetPrivacyState() {
        _saveState.value = SaveProfileUiState.Idle
    }

    fun addContactByPin(pin: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            profilesRepository.addContactByPin(pin)
                .onSuccess { response ->
                    onSuccess(response.displayName ?: "Pana")
                }
                .onFailure { error ->
                    onFailure(error.localizedMessage ?: "Error al agregar contacto por PIN")
                }
        }
    }
}
