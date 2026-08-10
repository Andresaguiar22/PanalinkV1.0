package com.example.notification.engine.sync

import androidx.annotation.Keep

@Keep
enum class NotificationSyncState {
    IDLE,
    SYNCING,
    SYNC_COMPLETED,
    SYNC_ERROR,
    OFFLINE
}
