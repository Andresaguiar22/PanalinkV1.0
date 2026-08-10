package com.example.notification.engine.analytics

import androidx.annotation.Keep

@Keep
object NotificationAnalytics {

    private val repository = NotificationAnalyticsRepository()
    private val engine = NotificationAnalyticsEngine.getInstance()

    fun trackCreated(eventId: String, type: String) {
        engine.recordEventReceived()
        repository.logMetric(
            NotificationMetric(
                eventId = eventId,
                notificationType = type,
                eventType = NotificationAnalyticsEventType.NOTIFICATION_CREATED
            )
        )
    }

    fun trackDisplayed(eventId: String, type: String, latencyMs: Long = 0) {
        engine.recordNotificationDelivered()
        engine.recordEventProcessed(latencyMs)
        repository.logMetric(
            NotificationMetric(
                eventId = eventId,
                notificationType = type,
                eventType = NotificationAnalyticsEventType.NOTIFICATION_DISPLAYED,
                latencyMs = latencyMs
            )
        )
    }

    fun trackOpened(eventId: String, type: String) {
        engine.recordNotificationClicked()
        repository.logMetric(
            NotificationMetric(
                eventId = eventId,
                notificationType = type,
                eventType = NotificationAnalyticsEventType.NOTIFICATION_OPENED
            )
        )
    }

    fun trackDismissed(eventId: String, type: String) {
        engine.recordNotificationDismissed()
        repository.logMetric(
            NotificationMetric(
                eventId = eventId,
                notificationType = type,
                eventType = NotificationAnalyticsEventType.NOTIFICATION_DISMISSED
            )
        )
    }

    fun trackAction(eventId: String, type: String, actionId: String) {
        repository.logMetric(
            NotificationMetric(
                eventId = eventId,
                notificationType = type,
                eventType = NotificationAnalyticsEventType.NOTIFICATION_ACTION_USED,
                actionId = actionId
            )
        )
    }

    fun getRepository(): NotificationAnalyticsRepository = repository
}
