package com.example.notification.security

import android.util.Log
import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

@Keep
class NotificationSecurityGuard private constructor() {

    private val producerCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val suspendedProducers = ConcurrentHashMap<String, Long>()

    fun inspectEvent(event: NotificationEvent): SecurityDecision {
        val producerId = event.actor?.id?.ifBlank { "unknown" } ?: "unknown"

        // Check if producer is currently suspended
        val suspendedUntil = suspendedProducers[producerId]
        if (suspendedUntil != null && System.currentTimeMillis() < suspendedUntil) {
            Log.w(TAG, "Event blocked from suspended producer: $producerId")
            return SecurityDecision.BLOCK_SUSPENDED
        }

        // Count events in current window
        val count = producerCounts.computeIfAbsent(producerId) { AtomicInteger(0) }.incrementAndGet()

        // High frequency threshold: > 100 events in active window
        if (count > 500) {
            suspendedProducers[producerId] = System.currentTimeMillis() + 60_000L // 1 minute suspension
            Log.e(TAG, "Producer $producerId suspended for high volume anomaly ($count events)")
            return SecurityDecision.SUSPEND_PRODUCER
        }

        if (count > 50) {
            Log.w(TAG, "High event burst detected for $producerId ($count events). Enforcing aggressive grouping.")
            return SecurityDecision.FORCE_AGGRESSIVE_GROUPING
        }

        return SecurityDecision.ALLOW
    }

    fun resetProducerWindow() {
        producerCounts.clear()
    }

    companion object {
        private const val TAG = "NotificationSecurityGuard"

        @Volatile
        private var instance: NotificationSecurityGuard? = null

        fun getInstance(): NotificationSecurityGuard {
            return instance ?: synchronized(this) {
                instance ?: NotificationSecurityGuard().also { instance = it }
            }
        }
    }
}

@Keep
enum class SecurityDecision {
    ALLOW,
    FORCE_AGGRESSIVE_GROUPING,
    SUSPEND_PRODUCER,
    BLOCK_SUSPENDED
}
