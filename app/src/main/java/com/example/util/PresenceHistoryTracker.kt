package com.example.util

import com.example.data.repository.UserPresenceStatus
import java.util.concurrent.ConcurrentHashMap

data class PresenceHistoryEvent(
    val userId: String,
    val status: UserPresenceStatus,
    val timestamp: Long = System.currentTimeMillis()
)

object PresenceHistoryTracker {
    private const val MAX_EVENTS_PER_USER = 50
    private val historyMap = ConcurrentHashMap<String, MutableList<PresenceHistoryEvent>>()

    fun recordEvent(userId: String, status: UserPresenceStatus, timestamp: Long = System.currentTimeMillis()) {
        val list = historyMap.getOrPut(userId) { mutableListOf() }
        synchronized(list) {
            if (list.lastOrNull()?.status != status) {
                list.add(PresenceHistoryEvent(userId, status, timestamp))
                if (list.size > MAX_EVENTS_PER_USER) {
                    list.removeAt(0)
                }
            }
        }
    }

    fun getHistoryForUser(userId: String): List<PresenceHistoryEvent> {
        val list = historyMap[userId] ?: return emptyList()
        synchronized(list) {
            return list.toList()
        }
    }

    fun clearHistory() {
        historyMap.clear()
    }
}
