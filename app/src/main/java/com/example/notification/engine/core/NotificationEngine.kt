package com.example.notification.engine.core

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Singleton master coordinator for Notification Engine V2.
 * Serves as the single unified entry point for event publishing and subscriber pipeline management.
 */
@Keep
class NotificationEngine private constructor() : NotificationPublisher {

    private val engineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val pipeline = NotificationPipeline()
    private val dispatcher = NotificationDispatcher(pipeline, engineScope)

    @Volatile
    private var activeContext: NotificationContext = NotificationContext()

    companion object {
        @Volatile
        private var instance: NotificationEngine? = null

        fun getInstance(): NotificationEngine {
            return instance ?: synchronized(this) {
                instance ?: NotificationEngine().also { instance = it }
            }
        }

        fun getContext(): NotificationContext {
            return getInstance().activeContext
        }

        fun updateContext(newContext: NotificationContext) {
            getInstance().activeContext = newContext
        }
    }

    override fun publish(event: NotificationEvent) {
        dispatcher.dispatch(event)
    }

    override fun publish(events: List<NotificationEvent>) {
        dispatcher.dispatch(events)
    }

    fun registerSubscriber(subscriber: NotificationSubscriber) {
        pipeline.registerSubscriber(subscriber)
    }

    fun unregisterSubscriber(subscriberId: String) {
        pipeline.unregisterSubscriber(subscriberId)
    }

    fun getSubscribers(): List<NotificationSubscriber> {
        return pipeline.getSubscribers()
    }

    /**
     * For direct unit/integration testing: synchronously execute pipeline bypassing the dispatcher queue.
     */
    suspend fun executeDirectlyForTesting(event: NotificationEvent) {
        pipeline.execute(event, activeContext)
    }
}
