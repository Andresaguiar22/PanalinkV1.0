package com.example.notification.engine.core

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent

/**
 * Consumer interface for pipeline stages or external listeners in Notification Engine V2.
 */
@Keep
interface NotificationSubscriber {
    /** Unique identifier for subscriber tracking and pipeline stage positioning. */
    val id: String

    /** Priority tier in the pipeline order (higher values executed earlier). */
    val pipelinePriority: Int get() = 0

    /**
     * Process an incoming notification event within the active context.
     * Return true if processing should continue down the pipeline, or false to halt/drop the event.
     */
    suspend fun process(event: NotificationEvent, context: NotificationContext): Boolean
}
