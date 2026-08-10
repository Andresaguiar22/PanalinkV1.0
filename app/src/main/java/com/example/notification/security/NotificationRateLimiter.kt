package com.example.notification.security

import androidx.annotation.Keep
import java.util.concurrent.ConcurrentHashMap

@Keep
class NotificationRateLimiter(
    private val maxEventsPerWindow: Int = 10,
    private val windowSizeMs: Long = 5000L // 5 seconds window
) {

    private val userTimestamps = ConcurrentHashMap<String, MutableList<Long>>()

    fun shouldAllow(userId: String): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = userTimestamps.computeIfAbsent(userId) { mutableListOf() }

        synchronized(timestamps) {
            timestamps.removeAll { now - it > windowSizeMs }
            if (timestamps.size < maxEventsPerWindow) {
                timestamps.add(now)
                return true
            }
            return false
        }
    }

    fun clear(userId: String) {
        userTimestamps.remove(userId)
    }

    fun clearAll() {
        userTimestamps.clear()
    }
}
