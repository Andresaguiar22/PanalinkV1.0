package com.example.notification.engine.analytics

import androidx.annotation.Keep

@Keep
object NotificationAnalytics {
    fun logEvent(name: String, params: Map<String, Any> = emptyMap()) {
        // No-op stub
    }

    fun trackCreated(eventId: String, type: String) {
        // No-op stub
    }
}
