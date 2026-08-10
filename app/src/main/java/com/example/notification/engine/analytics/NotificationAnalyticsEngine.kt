package com.example.notification.engine.analytics

import androidx.annotation.Keep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

@Keep
data class NotificationAnalyticsMetrics(
    val totalEventsReceived: Long = 0,
    val totalEventsProcessed: Long = 0,
    val totalEventsFiltered: Long = 0,
    val totalNotificationsDelivered: Long = 0,
    val totalNotificationsClicked: Long = 0,
    val totalNotificationsDismissed: Long = 0,
    val averageLatencyMs: Long = 0
)

@Keep
class NotificationAnalyticsEngine private constructor() {

    private val _totalReceived = AtomicLong(0)
    private val _totalProcessed = AtomicLong(0)
    private val _totalFiltered = AtomicLong(0)
    private val _totalDelivered = AtomicLong(0)
    private val _totalClicked = AtomicLong(0)
    private val _totalDismissed = AtomicLong(0)
    private val _totalLatencyMs = AtomicLong(0)

    private val _metricsState = MutableStateFlow(NotificationAnalyticsMetrics())
    val metricsState: StateFlow<NotificationAnalyticsMetrics> = _metricsState.asStateFlow()

    fun recordEventReceived() {
        _totalReceived.incrementAndGet()
        updateState()
    }

    fun recordEventProcessed(latencyMs: Long) {
        _totalProcessed.incrementAndGet()
        _totalLatencyMs.addAndGet(latencyMs)
        updateState()
    }

    fun recordEventFiltered() {
        _totalFiltered.incrementAndGet()
        updateState()
    }

    fun recordNotificationDelivered() {
        _totalDelivered.incrementAndGet()
        updateState()
    }

    fun recordNotificationClicked() {
        _totalClicked.incrementAndGet()
        updateState()
    }

    fun recordNotificationDismissed() {
        _totalDismissed.incrementAndGet()
        updateState()
    }

    fun recordNotificationIgnored() {
        _totalFiltered.incrementAndGet()
        updateState()
    }

    fun recordNotificationMuted() {
        _totalFiltered.incrementAndGet()
        updateState()
    }

    fun recordNotificationOpenTime(timeMs: Long) {
        _totalLatencyMs.addAndGet(timeMs)
        updateState()
    }

    fun recordUserBehavior(userId: String, notificationId: String, action: String) {
        when (action) {
            "clicked" -> recordNotificationClicked()
            "ignored" -> recordNotificationIgnored()
            "muted" -> recordNotificationMuted()
            else -> updateState()
        }
    }

    private fun updateState() {
        val processed = _totalProcessed.get()
        val avgLatency = if (processed > 0) _totalLatencyMs.get() / processed else 0L
        _metricsState.value = NotificationAnalyticsMetrics(
            totalEventsReceived = _totalReceived.get(),
            totalEventsProcessed = processed,
            totalEventsFiltered = _totalFiltered.get(),
            totalNotificationsDelivered = _totalDelivered.get(),
            totalNotificationsClicked = _totalClicked.get(),
            totalNotificationsDismissed = _totalDismissed.get(),
            averageLatencyMs = avgLatency
        )
    }

    fun reset() {
        _totalReceived.set(0)
        _totalProcessed.set(0)
        _totalFiltered.set(0)
        _totalDelivered.set(0)
        _totalClicked.set(0)
        _totalDismissed.set(0)
        _totalLatencyMs.set(0)
        updateState()
    }

    companion object {
        @Volatile
        private var instance: NotificationAnalyticsEngine? = null

        fun getInstance(): NotificationAnalyticsEngine {
            return instance ?: synchronized(this) {
                instance ?: NotificationAnalyticsEngine().also { instance = it }
            }
        }
    }
}
