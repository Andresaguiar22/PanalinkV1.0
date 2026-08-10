package com.example.notification.preferences

import androidx.annotation.Keep

@Keep
data class NotificationPreferenceEntity(
    val userId: String,
    val domain: String,
    val entityId: String? = null,
    val enabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val priorityOverride: String? = null,
    val quietHoursOverride: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
