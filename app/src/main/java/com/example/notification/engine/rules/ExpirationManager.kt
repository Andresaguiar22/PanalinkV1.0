package com.example.notification.engine.rules

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationTypeV2

@Keep
class ExpirationManager {

    companion object {
        const val TTL_CALL_INCOMING = 45_000L          // 45s
        const val TTL_TYPING = 5_000L                  // 5s
        const val TTL_STORY_VIEW = 86_400_000L        // 24 hours
        const val TTL_UPLOAD_FAILED = 43_200_000L      // 12 hours
        const val TTL_SECURITY_ALERT = 604_800_000L    // 7 days
    }

    fun isExpired(event: NotificationEvent, now: Long = System.currentTimeMillis()): Boolean {
        // 1. Explicit expiration set on event
        if (event.isExpired(now)) return true

        // 2. Default domain/type fallback TTL rules
        val fallbackTtl = when (event.type) {
            NotificationTypeV2.CALL_INCOMING -> TTL_CALL_INCOMING
            NotificationTypeV2.CHAT_TYPING, NotificationTypeV2.CHAT_RECORDING -> TTL_TYPING
            NotificationTypeV2.STORY_VIEW -> TTL_STORY_VIEW
            NotificationTypeV2.UPLOAD_FAILED -> TTL_UPLOAD_FAILED
            NotificationTypeV2.SECURITY_ALERT -> TTL_SECURITY_ALERT
            else -> null
        }

        if (fallbackTtl != null && (now - event.timestamp) > fallbackTtl) {
            return true
        }

        return false
    }
}
