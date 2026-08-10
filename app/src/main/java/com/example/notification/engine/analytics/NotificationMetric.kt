package com.example.notification.engine.analytics

import androidx.annotation.Keep

@Keep
enum class NotificationAnalyticsEventType {
    NOTIFICATION_CREATED,
    NOTIFICATION_DISPLAYED,
    NOTIFICATION_OPENED,
    NOTIFICATION_DISMISSED,
    NOTIFICATION_ACTION_USED
}

@Keep
data class NotificationMetric(
    val eventId: String,
    val notificationType: String,
    val eventType: NotificationAnalyticsEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val latencyMs: Long = 0,
    val actionId: String? = null
)
