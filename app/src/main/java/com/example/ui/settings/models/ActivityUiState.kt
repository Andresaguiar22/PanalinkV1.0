package com.example.ui.settings.models

data class DeviceInfo(
    val name: String,
    val lastActive: String,
    val isCurrent: Boolean,
    val iconType: String = "smartphone"
)

data class ActivityUiState(
    val messagesCount: Long = 0L,
    val callsCount: Long = 0L,
    val storageUsed: String = "0 MB",
    val databaseSize: String = "0 MB",
    val mediaSize: String = "0 MB",
    val connectionStatus: String = "Excelente",
    val isOnline: Boolean = true,
    val activeDevices: List<DeviceInfo> = emptyList(),
    val lastSynchronization: String = "Al día",
    val dataUsageToday: String = "0 MB",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
