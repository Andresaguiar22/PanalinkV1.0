package com.example.ui.settings.models

data class PrivacyUiState(
    val lastSeenVisibility: String = "Mis Contactos",
    val readReceiptsEnabled: Boolean = true,
    val invisibleModeEnabled: Boolean = false,
    val smartReadReceiptsEnabled: Boolean = true,
    val profilePresence: String = "online",
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
