package com.example.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.supabase.SessionManager
import com.example.data.supabase.SupabaseClient
import com.example.ui.settings.models.ControlCenterUiState
import com.example.ui.settings.models.DashboardAction
import com.example.ui.settings.repository.ActivityRepository
import com.example.ui.settings.repository.PresenceRepository
import com.example.ui.settings.repository.PrivacyRepository
import com.example.ui.settings.repository.SecurityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val presenceRepo = PresenceRepository(application.applicationContext)
    private val activityRepo = ActivityRepository(application.applicationContext)
    private val securityRepo = SecurityRepository(application.applicationContext)
    private val privacyRepo = PrivacyRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ControlCenterUiState(isLoading = true))
    val uiState: StateFlow<ControlCenterUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val presenceState = presenceRepo.loadPresenceSettings()
                val activityState = activityRepo.loadActivitySummary()
                val securityState = securityRepo.loadSecuritySettings()
                val privacyState = privacyRepo.loadPrivacySettings()

                val currentUser = SupabaseClient.currentUser
                val cachedProfile = SessionManager.getCachedProfile() ?: SupabaseClient.currentProfile

                val rawName = cachedProfile?.displayName
                val userName = if (!rawName.isNullOrBlank()) {
                    rawName
                } else {
                    currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercase() } ?: "Usuario Pana"
                }

                val emailHandle = currentUser?.email?.substringBefore("@") ?: "usuario_pana"
                val userHandle = "@$emailHandle"

                val avatarUrl = cachedProfile?.avatarUrl ?: ""

                val presenceSummaryStr = when {
                    presenceState.isInvisibleMode -> "Modo Invisible 👻"
                    presenceState.status == "busy" -> "Ocupado 🔴"
                    presenceState.status == "invisible" -> "Invisible ⚪"
                    else -> "Disponible 🟢 • Visibilidad: ${privacyState.lastSeenVisibility}"
                }

                val securitySummaryStr = when {
                    securityState.hasPin && securityState.is2FaEnabled -> "PIN activo • 2FA activado 🛡️"
                    securityState.hasPin -> "PIN activo • 2FA desactivado"
                    securityState.is2FaEnabled -> "2FA activado • Sin PIN"
                    else -> "Protección básica sin PIN"
                }

                val storageSummaryStr = "${activityState.storageUsed} utilizados localmente"
                val activitySummaryStr = "Diagnóstico: ${activityState.connectionStatus} • ${activityState.messagesCount} msgs"

                _uiState.value = ControlCenterUiState(
                    isLoading = false,
                    userName = userName,
                    userHandle = userHandle,
                    avatarUrl = avatarUrl,
                    presenceStatus = presenceState.status,
                    activeDevicesCount = activityState.activeDevices.size,
                    isOnline = activityState.isOnline,
                    storageUsedSummary = activityState.storageUsed,
                    appVersion = "2.0.0",
                    hasPin = securityState.hasPin,
                    is2FaEnabled = securityState.is2FaEnabled,
                    messagesCount = activityState.messagesCount,
                    lastSynchronization = activityState.lastSynchronization,
                    connectionStatus = activityState.connectionStatus,
                    profileSummary = "Perfil completo • $userName",
                    presenceSummary = presenceSummaryStr,
                    privacySummary = "Última vez: ${privacyState.lastSeenVisibility}",
                    securitySummary = securitySummaryStr,
                    chatsSummary = "${activityState.messagesCount} mensajes locales",
                    notificationsSummary = "Notificaciones y alertas activas",
                    customizationSummary = "Tema Oscuro PanaLink",
                    storageSummary = storageSummaryStr,
                    activitySummary = activitySummaryStr
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar dashboard"
                    )
                }
            }
        }
    }

    fun dispatch(action: DashboardAction) {
        when (action) {
            is DashboardAction.RefreshDashboard -> loadDashboard()
            is DashboardAction.ClearError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val authManager = com.example.data.supabase.AuthManager()
            authManager.signOut()
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().unregister()
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Failed to unregister FCM on logout", e)
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun deleteAccount(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val authManager = com.example.data.supabase.AuthManager()
            authManager.deleteAccount()
            try {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().unregister()
            } catch (e: Exception) {
                android.util.Log.e("DashboardViewModel", "Failed to unregister FCM on deleteAccount", e)
            }
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}