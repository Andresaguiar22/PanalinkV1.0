package com.example.notification.engine.core

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent

/**
 * Contract for publishing notification events into the engine without coupling to implementation details.
 */
@Keep
interface NotificationPublisher {
    fun publish(event: NotificationEvent)
    fun publish(events: List<NotificationEvent>)
}
