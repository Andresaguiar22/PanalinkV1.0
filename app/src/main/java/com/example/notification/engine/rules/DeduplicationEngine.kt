package com.example.notification.engine.rules

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent

@Keep
class DeduplicationEngine(
    private val maxCacheSize: Int = 500,
    private val defaultTtlMillis: Long = 10_000L
) {
    private val cache = object : LinkedHashMap<String, Long>(maxCacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Long>?): Boolean {
            return size > maxCacheSize
        }
    }
    private val lock = Any()

    fun isDuplicate(event: NotificationEvent, now: Long = System.currentTimeMillis()): Boolean {
        val key = if (event.id.isNotBlank()) event.id else event.effectiveDeduplicationKey()
        synchronized(lock) {
            val lastSeen = cache[key]
            if (lastSeen != null && (now - lastSeen) < defaultTtlMillis) {
                return true
            }
            cache[key] = now
            return false
        }
    }

    fun clear() {
        synchronized(lock) {
            cache.clear()
        }
    }
}
