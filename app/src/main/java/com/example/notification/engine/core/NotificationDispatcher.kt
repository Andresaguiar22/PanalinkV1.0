package com.example.notification.engine.core

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Handles incoming notification queueing, thread synchronization, and sequential delivery to the NotificationPipeline.
 */
@Keep
class NotificationDispatcher(
    private val pipeline: NotificationPipeline,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val eventChannel = Channel<NotificationEvent>(Channel.UNLIMITED)

    init {
        coroutineScope.launch {
            for (event in eventChannel) {
                val context = NotificationEngine.getContext()
                pipeline.execute(event, context)
            }
        }
    }

    fun dispatch(event: NotificationEvent) {
        eventChannel.trySend(event)
    }

    fun dispatch(events: List<NotificationEvent>) {
        events.forEach { eventChannel.trySend(it) }
    }
}
