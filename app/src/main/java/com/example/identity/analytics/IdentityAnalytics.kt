package com.example.identity.analytics

import android.util.Log

object IdentityAnalytics {
    private const val TAG = "IdentityAnalytics"

    var profilesSynced = 0
    var profilesDownloaded = 0
    var avatarsDownloaded = 0
    var syncFailures = 0
    var roomHits = 0
    var networkHits = 0
    
    // Average sync time variables
    private var totalSyncTimeMs = 0L
    private var syncCount = 0

    fun trackProfileSync(success: Boolean, timeMs: Long) {
        if (success) {
            profilesSynced++
            totalSyncTimeMs += timeMs
            syncCount++
        } else {
            syncFailures++
        }
        logStats()
    }

    fun trackProfileDownloaded() {
        profilesDownloaded++
    }

    fun trackAvatarDownloaded() {
        avatarsDownloaded++
    }

    fun trackRoomHit() {
        roomHits++
    }

    fun trackNetworkHit() {
        networkHits++
    }

    private fun logStats() {
        val totalHits = roomHits + networkHits
        val roomPercentage = if (totalHits > 0) (roomHits.toFloat() / totalHits) * 100 else 0f
        val networkPercentage = if (totalHits > 0) (networkHits.toFloat() / totalHits) * 100 else 0f
        val avgSyncTime = if (syncCount > 0) totalSyncTimeMs / syncCount else 0L

        Log.d(TAG, """
            IMCE Analytics Stats:
            Profiles Synced: $profilesSynced
            Profiles Downloaded: $profilesDownloaded
            Avatars Downloaded: $avatarsDownloaded
            Sync Failures: $syncFailures
            Average Sync Time: ${avgSyncTime}ms
            Room Hits: $roomHits ($roomPercentage%)
            Network Hits: $networkHits ($networkPercentage%)
        """.trimIndent())
    }
}
