package com.example.ui.viewmodel.onboarding

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ProfilesRepository
import com.example.data.supabase.SessionManager
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class OnboardingUiState {
    object Idle : OnboardingUiState()
    object Loading : OnboardingUiState()
    object Success : OnboardingUiState()
    data class Error(val message: String) : OnboardingUiState()
}

class OnboardingViewModel(
    private val profilesRepository: ProfilesRepository = ProfilesRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Idle)
    val uiState: StateFlow<OnboardingUiState> = _uiState

    private val _displayName = MutableStateFlow(
        SupabaseClient.currentProfile?.displayName ?: ""
    )
    val displayName: StateFlow<String> = _displayName

    private val _firstName = MutableStateFlow(SupabaseClient.currentProfile?.firstName ?: "")
    val firstName: StateFlow<String> = _firstName

    private val _lastName = MutableStateFlow(SupabaseClient.currentProfile?.lastName ?: "")
    val lastName: StateFlow<String> = _lastName

    private val _status = MutableStateFlow(SupabaseClient.currentProfile?.status ?: "¡Activo en Panalink! ⚡")
    val status: StateFlow<String> = _status

    private val _birthDate = MutableStateFlow(SupabaseClient.currentProfile?.birthDate ?: "")
    val birthDate: StateFlow<String> = _birthDate

    private val _sex = MutableStateFlow(SupabaseClient.currentProfile?.sex ?: "")
    val sex: StateFlow<String> = _sex

    private val _interests = MutableStateFlow<List<String>>(SupabaseClient.currentProfile?.interests ?: emptyList())
    val interests: StateFlow<List<String>> = _interests

    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri: StateFlow<Uri?> = _avatarUri

    private val _avatarUrl = MutableStateFlow<String?>(SupabaseClient.currentProfile?.avatarUrl)
    val avatarUrl: StateFlow<String?> = _avatarUrl

    private val _coverUrl = MutableStateFlow<String?>(SupabaseClient.currentProfile?.coverUrl)
    val coverUrl: StateFlow<String?> = _coverUrl

    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar

    private val _isUploadingCover = MutableStateFlow(false)
    val isUploadingCover: StateFlow<Boolean> = _isUploadingCover

    private val _avatarUploadError = MutableStateFlow<String?>(null)
    val avatarUploadError: StateFlow<String?> = _avatarUploadError

    private val _coverUploadError = MutableStateFlow<String?>(null)
    val coverUploadError: StateFlow<String?> = _coverUploadError

    fun setDisplayName(name: String) {
        _displayName.value = name
    }

    fun setFirstName(name: String) {
        _firstName.value = name
    }

    fun setLastName(name: String) {
        _lastName.value = name
    }

    fun setStatus(value: String) {
        _status.value = value
    }

    fun setBirthDate(value: String) {
        _birthDate.value = value
    }

    fun setSex(value: String) {
        _sex.value = value
    }

    fun setInterests(list: List<String>) {
        _interests.value = list
    }

    fun setAvatarUri(uri: Uri?) {
        _avatarUri.value = uri
    }

    fun setAvatarUrl(url: String?) {
        _avatarUrl.value = url
    }

    fun setCoverUrl(url: String?) {
        _coverUrl.value = url
    }

    fun uploadAvatar(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            _isUploadingAvatar.value = true
            _avatarUploadError.value = null
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val extension = if (mimeType == "image/png") "png" else "jpg"
                val tempFile = java.io.File.createTempFile("avatar_upload_", ".$extension", context.cacheDir)
                
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                val result = profilesRepository.uploadProfileImage(tempFile, mimeType)
                if (result.isSuccess) {
                    val url = result.getOrThrow()
                    _avatarUrl.value = url
                    Log.i("OnboardingViewModel", "Avatar uploaded successfully to CDN: $url")
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Error al subir la imagen"
                    _avatarUploadError.value = errMsg
                    Log.e("OnboardingViewModel", "Avatar upload failed: $errMsg")
                }
            } catch (e: Exception) {
                val errMsg = e.localizedMessage ?: "Excepción al procesar imagen"
                _avatarUploadError.value = errMsg
                Log.e("OnboardingViewModel", "Avatar upload exception", e)
            } finally {
                _isUploadingAvatar.value = false
            }
        }
    }

    fun uploadCover(context: android.content.Context, uri: Uri) {
        viewModelScope.launch {
            _isUploadingCover.value = true
            _coverUploadError.value = null
            try {
                val contentResolver = context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
                val extension = if (mimeType == "image/png") "png" else "jpg"
                val tempFile = java.io.File.createTempFile("cover_upload_", ".$extension", context.cacheDir)
                
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    tempFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                
                val result = profilesRepository.uploadProfileImage(tempFile, mimeType)
                if (result.isSuccess) {
                    val url = result.getOrThrow()
                    _coverUrl.value = url
                    Log.i("OnboardingViewModel", "Cover uploaded successfully: $url")
                } else {
                    val errMsg = result.exceptionOrNull()?.message ?: "Error al subir la portada"
                    _coverUploadError.value = errMsg
                }
            } catch (e: Exception) {
                _coverUploadError.value = e.localizedMessage ?: "Error al procesar la portada"
            } finally {
                _isUploadingCover.value = false
            }
        }
    }

    fun finalizeOnboarding(onSuccess: () -> Unit) {
        val currentUser = SupabaseClient.currentUser
        if (currentUser == null) {
            _uiState.value = OnboardingUiState.Error("No hay sesión activa de usuario.")
            Log.e("OnboardingViewModel", "[FINALIZE_PROFILE_FAILED] No active user session.")
            return
        }

        val name = _displayName.value.trim().ifEmpty {
            currentUser.email?.substringBefore("@") ?: "Pana"
        }

        _uiState.value = OnboardingUiState.Loading
        Log.i("OnboardingViewModel", "[FINALIZE_PROFILE_START] Starting onboarding finalization for user: ${currentUser.id}, name: $name")

        viewModelScope.launch {
            try {
                // ASEGURAR QUE TENEMOS TOKEN ANTES DE PROCEDER
                Log.i("OnboardingViewModel", "[FINALIZE_ONBOARDING] Checking session token...")
                if (SupabaseClient.currentToken.isNullOrEmpty()) {
                    Log.i("OnboardingViewModel", "Token is null, attempting to restore session before finalizing...")
                    SessionManager.validateAndRefreshSessionIfNeeded()
                }

                if (SupabaseClient.currentToken.isNullOrEmpty()) {
                    val errMessage = "Sesión inválida (Token nulo). Por favor intenta iniciar sesión de nuevo para continuar."
                    Log.e("OnboardingViewModel", "[FINALIZE_PROFILE_FAILED] $errMessage")
                    _uiState.value = OnboardingUiState.Error(errMessage)
                    return@launch
                }

                val finalAvatarUrl: String? = (_avatarUrl.value ?: SupabaseClient.currentProfile?.avatarUrl)?.trim()?.ifEmpty { null }
                val finalCoverUrl: String? = (_coverUrl.value ?: SupabaseClient.currentProfile?.coverUrl)?.trim()?.ifEmpty { null }

                // 2. Ejecutar creación/actualización del perfil en Supabase.
                Log.i("OnboardingViewModel", "DEBUG_FINAL_PAYLOAD:")
                Log.i("OnboardingViewModel", " - userId: ${currentUser.id}")
                Log.i("OnboardingViewModel", " - displayName: $name")
                Log.i("OnboardingViewModel", " - avatarUrl: $finalAvatarUrl")
                Log.i("OnboardingViewModel", " - coverUrl: $finalCoverUrl")
                Log.i("OnboardingViewModel", " - firstName: ${_firstName.value.trim().ifEmpty { null }}")
                Log.i("OnboardingViewModel", " - lastName: ${_lastName.value.trim().ifEmpty { null }}")
                Log.i("OnboardingViewModel", " - status: ${_status.value.trim().ifEmpty { "¡Activo en Panalink! ⚡" }}")
                Log.i("OnboardingViewModel", " - birthDate: ${_birthDate.value.trim().ifEmpty { null }}")
                Log.i("OnboardingViewModel", " - sex: ${_sex.value.trim().ifEmpty { null }}")
                Log.i("OnboardingViewModel", " - interests: ${_interests.value}")

                val result = profilesRepository.completeUserProfile(
                    userId = currentUser.id,
                    displayName = name,
                    avatarUrl = finalAvatarUrl,
                    firstName = _firstName.value.trim().ifEmpty { null },
                    lastName = _lastName.value.trim().ifEmpty { null },
                    status = _status.value.trim().ifEmpty { "¡Activo en Panalink! ⚡" },
                    birthDate = _birthDate.value.trim().ifEmpty { null },
                    sex = _sex.value.trim().ifEmpty { null },
                    interests = _interests.value,
                    coverUrl = finalCoverUrl
                )

                if (result.isSuccess) {
                    val finalProfile = result.getOrNull()
                    Log.i("OnboardingViewModel", "[FINALIZE_PROFILE_RESPONSE] Success: $finalProfile")

                    if (finalProfile != null) {
                        SupabaseClient.currentProfile = finalProfile

                        // Persist session
                        SessionManager.saveSession(
                            SupabaseClient.currentToken,
                            SupabaseClient.currentRefreshToken,
                            SupabaseClient.currentUser,
                            SupabaseClient.currentProfile
                        )

                        Log.i("OnboardingViewModel", "Profile onboarding finalized for user: ${currentUser.id}")
                        _uiState.value = OnboardingUiState.Success
                        onSuccess()
                    } else {
                        val errMessage = "El perfil devuelto es nulo."
                        Log.e("OnboardingViewModel", "[FINALIZE_PROFILE_FAILED] $errMessage")
                        _uiState.value = OnboardingUiState.Error(errMessage)
                    }
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Error al guardar el perfil."
                    Log.e("OnboardingViewModel", "[FINALIZE_PROFILE_RESPONSE] Failed response received from server: $err")
                    Log.e("OnboardingViewModel", "[FINALIZE_PROFILE_FAILED] Onboarding failed: $err")
                    _uiState.value = OnboardingUiState.Error(err)
                }
            } catch (e: Exception) {
                val errMessage = e.localizedMessage ?: "Error inesperado al guardar perfil."
                Log.e("OnboardingViewModel", "[FINALIZE_PROFILE_FAILED] Onboarding unexpected exception", e)
                _uiState.value = OnboardingUiState.Error(errMessage)
            }
        }
    }

    fun clearError() {
        _uiState.value = OnboardingUiState.Idle
    }
}
