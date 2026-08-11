package com.example.notification.engine.core

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent

/**
 * Legacy shell for Notification Engine V2.
 * Currently disabled - all logic moved to direct Supabase publishing via adapters.
 */
@Keep
class NotificationEngine private constructor() : NotificationPublisher {

    companion object {
        @Volatile
        private var instance: NotificationEngine? = null

        fun getInstance(): NotificationEngine {
            return instance ?: synchronized(this) {
                instance ?: NotificationEngine().also { instance = it }
            }
        }
    }

    override fun publish(event: NotificationEvent) {
        // No-op: Local pipeline disabled
    }

    override fun publish(events: List<NotificationEvent>) {
        // No-op: Local pipeline disabled
    }
}
