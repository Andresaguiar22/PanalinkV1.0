package com.example.notification.preferences

import androidx.annotation.Keep
import java.util.concurrent.ConcurrentHashMap

@Keep
class NotificationPreferenceRepository private constructor() {

    private val userPreferences = ConcurrentHashMap<String, MutableMap<String, NotificationPreferenceEntity>>()
    private val mutedEntities = ConcurrentHashMap.newKeySet<String>()

    fun setPreference(pref: NotificationPreferenceEntity) {
        val userMap = userPreferences.computeIfAbsent(pref.userId) { ConcurrentHashMap() }
        userMap[pref.domain] = pref
    }

    fun getPreference(userId: String, domain: String): NotificationPreferenceEntity {
        return userPreferences[userId]?.get(domain)
            ?: NotificationPreferenceEntity(userId = userId, domain = domain)
    }

    fun muteEntity(entityId: String) {
        mutedEntities.add(entityId)
    }

    fun unmuteEntity(entityId: String) {
        mutedEntities.remove(entityId)
    }

    fun isEntityMuted(entityId: String): Boolean = mutedEntities.contains(entityId)

    companion object {
        @Volatile
        private var instance: NotificationPreferenceRepository? = null

        fun getInstance(): NotificationPreferenceRepository {
            return instance ?: synchronized(this) {
                instance ?: NotificationPreferenceRepository().also { instance = it }
            }
        }
    }
}
