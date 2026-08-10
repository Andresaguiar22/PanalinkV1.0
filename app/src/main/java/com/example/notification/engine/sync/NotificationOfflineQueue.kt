package com.example.notification.engine.sync

import android.util.Log
import androidx.annotation.Keep
import com.example.notification.engine.producers.NotificationEventPublisher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

@Keep
data class PendingNotificationEvent(
    val id: String,
    val eventType: String,
    val actorId: String,
    val targetUserId: String,
    val entityId: String,
    val payload: Map<String, Any> = emptyMap(),
    val title: String? = null,
    val body: String? = null,
    val domain: String? = null,
    val createdTime: Long = System.currentTimeMillis(),
    var retryCount: Int = 0,
    var status: String = "PENDING"
)

@Keep
class NotificationOfflineQueue private constructor() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val queue = ConcurrentLinkedQueue<PendingNotificationEvent>()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    fun enqueue(event: PendingNotificationEvent) {
        queue.add(event)
        _pendingCount.value = queue.size
        Log.d(TAG, "Event enqueued offline: ${event.id} (Total pending: ${queue.size})")
    }

    fun processPendingQueue() {
        if (queue.isEmpty()) return

        scope.launch {
            Log.d(TAG, "Processing offline queued events count: ${queue.size}")
            val iterator = queue.iterator()
            while (iterator.hasNext()) {
                val item = iterator.next()
                try {
                    item.status = "SENDING"
                    val result = NotificationEventPublisher.publishEvent(
                        eventType = item.eventType,
                        actorId = item.actorId,
                        targetUserId = item.targetUserId,
                        entityId = item.entityId,
                        title = item.title,
                        body = item.body,
                        domain = item.domain
                    )
                    if (result.isSuccess) {
                        item.status = "SENT"
                        iterator.remove()
                        _pendingCount.value = queue.size
                        Log.d(TAG, "Successfully flushed queued event: ${item.id}")
                    } else {
                        item.retryCount++
                        item.status = if (item.retryCount >= 5) "FAILED" else "PENDING"
                        if (item.status == "FAILED") iterator.remove()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to flush queued event ${item.id}", e)
                    item.retryCount++
                    item.status = if (item.retryCount >= 5) "FAILED" else "PENDING"
                    if (item.status == "FAILED") iterator.remove()
                }
            }
            _pendingCount.value = queue.size
        }
    }

    companion object {
        private const val TAG = "NotificationOfflineQueue"

        @Volatile
        private var instance: NotificationOfflineQueue? = null

        fun getInstance(): NotificationOfflineQueue {
            return instance ?: synchronized(this) {
                instance ?: NotificationOfflineQueue().also { instance = it }
            }
        }
    }
}
