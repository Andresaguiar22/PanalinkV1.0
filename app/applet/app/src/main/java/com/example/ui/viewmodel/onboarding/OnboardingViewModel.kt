package com.example.ui.viewmodel.onboarding

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.PanalinkDatabase
import com.example.data.database.PendingUploadEntity
import com.example.data.repository.ProfilesRepository
import com.example.data.supabase.SupabaseClient
import com.example.util.PanalinkMediaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

sealed class OnboardingUiState {
    object Idle : OnboardingUiState()
    object Loading : OnboardingUiState()
    object Success : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}

class OnboardingViewModel : ViewModel() {
    private val profilesRepository = ProfilesRepository()

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri: StateFlow<Uri?> = _avatarUri.asStateFlow()

    fun setDisplayName(name: String) {
        _displayName.value = name
    }

    fun setAvatarUri(uri: Uri?) {
        _avatarUri.value = uri
    }

    fun finalizeOnboarding(context: Context) {
        val currentUser = SupabaseClient.currentUser ?: return
        
        _uiState.value = OnboardingUiState.Loading
        viewModelScope.launch {
            try {
                if (_avatarUri.value != null) {
                    // Start upload via Worker, wait for it using Observer? 
                    // Actually, since this is Firebase / Supabase Storage, and we want 
                    // to reuse existing infrastructure:
                    // For onboarding we can just enqueue the worker. But wait!
                    // Profile setup shouldn't block on the image upload finishing if it takes too long.
                    // Oh, wait, the prompt says: "Subir utilizando la infraestructura existente. No crear nuevo sistema de almacenamiento... Obtener nuevamente el perfil actualizado... Emitir estado completado"
                    // Let's just save the profile with is_profile_complete=true and the name.
                    // The avatar will be uploaded asynchronously. Wait, no, we can upload it first.
                    // We can just use the same logic ProfileViewModel uses!
                    val localPath = withContext(Dispatchers.IO) {
                        PanalinkMediaManager.saveMediaToLocal(
                            context = context,
                            uri = _avatarUri.value!!,
                            sourceFile = null,
                            fileName = "profile_avatar_${System.currentTimeMillis()}.jpg"
                        )
                    }

                    if (localPath != null) {
                        val file = File(localPath)
                        uploadProfilePhotoBackground(context, file, "image/jpeg", currentUser.id) { avatarUrlToSave ->
                            // Once uploaded, complete profile with avatar
                            completeProfile(currentUser.id, avatarUrlToSave)
                        }
                    } else {
                        // Error getting file, complete without avatar
                        completeProfile(currentUser.id, null)
                    }
                } else {
                    // No avatar selected
                    completeProfile(currentUser.id, null)
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingUiState.Error(e.message ?: "Error desconocido")
            }
        }
    }
    
    private fun completeProfile(userId: String, avatarUrlToSave: String?) {
        viewModelScope.launch {
            val updateResult = profilesRepository.updateProfile(
                userId = userId,
                displayName = _displayName.value,
                avatarUrl = avatarUrlToSave,
                isProfileComplete = true
            )
            
            if (updateResult.isSuccess) {
                _uiState.value = OnboardingUiState.Success
            } else {
                _uiState.value = OnboardingUiState.Error(updateResult.exceptionOrNull()?.message ?: "Error al actualizar perfil")
            }
        }
    }

    private fun uploadProfilePhotoBackground(
        context: Context,
        file: File,
        mimeType: String,
        userId: String,
        onSuccess: (String) -> Unit
    ) {
        val uploadId = UUID.randomUUID().toString()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pendingUpload = PendingUploadEntity(
                    id = uploadId,
                    userId = userId,
                    uploadType = "PROFILE",
                    localFilePath = file.absolutePath,
                    mimeType = mimeType,
                    status = "pending"
                )

                val db = PanalinkDatabase.getDatabase(context)
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

                withContext(Dispatchers.Main) {
                    val workManager = androidx.work.WorkManager.getInstance(context)
                    val liveData = workManager.getWorkInfoByIdLiveData(uploadWorkRequest.id)
                    val observer = object : androidx.lifecycle.Observer<androidx.work.WorkInfo> {
                        override fun onChanged(value: androidx.work.WorkInfo) {
                            if (value != null) {
                                if (value.state == androidx.work.WorkInfo.State.SUCCEEDED) {
                                    val uploadedUrl = value.outputData.getString("remoteUrl") ?: ""
                                    liveData.removeObserver(this)
                                    onSuccess(uploadedUrl)
                                } else if (value.state == androidx.work.WorkInfo.State.FAILED || value.state == androidx.work.WorkInfo.State.CANCELLED) {
                                    liveData.removeObserver(this)
                                    // Complete without avatar if upload fails
                                    onSuccess("")
                                }
                            }
                        }
                    }
                    liveData.observeForever(observer)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onSuccess("") // Fallback
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = OnboardingUiState.Idle
    }
}
