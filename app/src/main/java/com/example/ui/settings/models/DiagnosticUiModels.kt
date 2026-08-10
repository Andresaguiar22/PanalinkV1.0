package com.example.ui.settings.models

data class DiagnosticCenterState(
    val syncStatus: ComponentStatus = ComponentStatus.UNKNOWN,
    val connectionStatus: ComponentStatus = ComponentStatus.UNKNOWN,
    val realtimeStatus: ComponentStatus = ComponentStatus.UNKNOWN,
    val callsStatus: ComponentStatus = ComponentStatus.UNKNOWN,
    val mediaStatus: ComponentStatus = ComponentStatus.UNKNOWN,
    val storageStatus: ComponentStatus = ComponentStatus.UNKNOWN,
    val notificationStatus: ComponentStatus = ComponentStatus.UNKNOWN,
    val securityStatus: ComponentStatus = ComponentStatus.UNKNOWN,
    val pendingWorkersCount: Int = 0,
    val lastSyncTimeMs: Long = 0L,
    val connectionQuality: ConnectionQuality = ConnectionQuality.UNKNOWN
)

enum class ComponentStatus {
    OK,
    WARNING,
    ERROR,
    SYNCING,
    OFFLINE,
    UNKNOWN
}

enum class ConnectionQuality {
    EXCELLENT,
    GOOD,
    POOR,
    OFFLINE,
    UNKNOWN
}
