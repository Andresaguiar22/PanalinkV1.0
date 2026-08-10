package com.example.ui.settings.models

data class PresenceUiState(
    val status: String = "online", // "online", "busy", "invisible"
    val lastSeenVisibility: String = "Mis Contactos", // "Todos", "Mis Contactos", "Nadie"
    val isInvisibleMode: Boolean = false,
    val lastSeenTimestamp: String = "Ahora mismo",
    val isSyncing: Boolean = false,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
