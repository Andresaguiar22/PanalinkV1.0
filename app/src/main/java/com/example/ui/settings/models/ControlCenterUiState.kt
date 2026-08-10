package com.example.ui.settings.models

data class ControlCenterUiState(
    val isLoading: Boolean = false,
    val userName: String = "Usuario Pana",
    val userHandle: String = "@usuario_pana",
    val avatarUrl: String = "",
    val presenceStatus: String = "online",
    val activeDevicesCount: Int = 1,
    val isOnline: Boolean = true,
    val storageUsedSummary: String = "0 MB",
    val appVersion: String = "2.0.0",
    val hasPin: Boolean = false,
    val is2FaEnabled: Boolean = false,
    val messagesCount: Long = 0L,
    val lastSynchronization: String = "Al día",
    val connectionStatus: String = "Excelente",
    val errorMessage: String? = null,
    
    // Category Summaries
    val profileSummary: String = "Foto de perfil y nombre",
    val presenceSummary: String = "En línea • Visible para todos",
    val privacySummary: String = "Última conexión protegida",
    val securitySummary: String = "PIN y 2FA configurados",
    val chatsSummary: String = "Ajustes de chat y fuentes",
    val notificationsSummary: String = "Sonidos y alertas activas",
    val customizationSummary: String = "Tema Oscuro PanaLink",
    val storageSummary: String = "0 MB utilizados",
    val activitySummary: String = "Diagnóstico de red y llamadas"
)
