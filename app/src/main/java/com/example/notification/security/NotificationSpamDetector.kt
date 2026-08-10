package com.example.notification.security

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent

@Keep
class NotificationSpamDetector(
    private val rateLimiter: NotificationRateLimiter = NotificationRateLimiter(maxEventsPerWindow = 10, windowSizeMs = 5000L)
) {

    fun isSpam(event: NotificationEvent): Boolean {
        val actorId = event.actor?.id ?: return false
        val allowed = rateLimiter.shouldAllow(actorId)
        return !allowed
    }
}
