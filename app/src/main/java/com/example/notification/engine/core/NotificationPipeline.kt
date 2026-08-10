package com.example.notification.engine.core

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent

/**
 * Sequentially executes registered subscribers / processing stages for a given NotificationEvent.
 */
@Keep
class NotificationPipeline {

    private val subscribers = mutableListOf<NotificationSubscriber>()
    private val lock = Any()

    fun registerSubscriber(subscriber: NotificationSubscriber) {
        synchronized(lock) {
            subscribers.removeAll { it.id == subscriber.id }
            subscribers.add(subscriber)
            subscribers.sortByDescending { it.pipelinePriority }
        }
    }

    fun unregisterSubscriber(subscriberId: String) {
        synchronized(lock) {
            subscribers.removeAll { it.id == subscriberId }
        }
    }

    fun getSubscribers(): List<NotificationSubscriber> {
        synchronized(lock) {
            return subscribers.toList()
        }
    }

    /**
     * Runs event sequentially through subscribers sorted by priority.
     * Stops if any subscriber returns false (halt signal).
     */
    suspend fun execute(event: NotificationEvent, context: NotificationContext) {
        val currentSubscribers = getSubscribers()
        for (subscriber in currentSubscribers) {
            val continuePipeline = subscriber.process(event, context)
            if (!continuePipeline) {
                break
            }
        }
    }
}
