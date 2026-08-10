package com.example.notification.preferences

import android.util.Log
import androidx.annotation.Keep

@Keep
class NotificationPreferenceSyncManager private constructor(
    private val preferenceRepository: NotificationPreferenceRepository = NotificationPreferenceRepository.getInstance()
) {

    private val pendingSyncQueue = java.util.concurrent.ConcurrentLinkedQueue<NotificationPreferenceEntity>()

    fun updatePreferenceLocally(pref: NotificationPreferenceEntity) {
        val current = preferenceRepository.getPreference(pref.userId, pref.domain)
        if (pref.updatedAt >= current.updatedAt) {
            preferenceRepository.setPreference(pref)
            pendingSyncQueue.add(pref)
            Log.d(TAG, "Updated preference locally and queued for remote sync: ${pref.domain} for user ${pref.userId}")
        } else {
            Log.d(TAG, "Ignored stale local preference update for ${pref.domain}")
        }
    }

    fun syncFromRemote(remotePref: NotificationPreferenceEntity): NotificationPreferenceEntity {
        val currentLocal = preferenceRepository.getPreference(remotePref.userId, remotePref.domain)
        return if (remotePref.updatedAt >= currentLocal.updatedAt) {
            preferenceRepository.setPreference(remotePref)
            Log.d(TAG, "Applied remote preference update for domain ${remotePref.domain}")
            remotePref
        } else {
            Log.d(TAG, "Local preference is newer than remote, retaining local for domain ${remotePref.domain}")
            currentLocal
        }
    }

    fun getPendingSyncQueue(): List<NotificationPreferenceEntity> {
        return pendingSyncQueue.toList()
    }

    fun clearPendingSync(pref: NotificationPreferenceEntity) {
        pendingSyncQueue.remove(pref)
    }

    companion object {
        private const val TAG = "NotifPrefSyncManager"

        @Volatile
        private var instance: NotificationPreferenceSyncManager? = null

        fun getInstance(): NotificationPreferenceSyncManager {
            return instance ?: synchronized(this) {
                instance ?: NotificationPreferenceSyncManager().also { instance = it }
            }
        }
    }
}
