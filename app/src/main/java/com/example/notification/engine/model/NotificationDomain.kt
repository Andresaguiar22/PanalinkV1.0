package com.example.notification.engine.model

import androidx.annotation.Keep

/**
 * Top-level classification for Notification Events in PanaLink V2.0.
 * Decouples system domains to enable domain-level routing, filtering, and settings.
 */
@Keep
enum class NotificationDomain {
    CHAT,
    SOCIAL,
    STORIES,
    REELS,
    POSTS,
    COMMENTS,
    CALLS,
    SECURITY,
    SYSTEM,
    UPLOADS,
    PROFILE,
    GROUPS,
    COMMUNITIES,
    CHANNELS,
    BUSINESS
}
