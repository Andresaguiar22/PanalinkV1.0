package com.example.notification.engine.model

import androidx.annotation.Keep

/**
 * Technical priority level assigned to a notification event.
 * Dictates processing precedence in the pipeline and storage tier.
 */
@Keep
enum class NotificationPriority(val level: Int) {
    /** Immediate processing; bypasses quiet hours and throttling (e.g. Incoming Calls, Critical Security Alerts). */
    CRITICAL(5),

    /** High precedence; processed immediately (e.g. Direct Messages, Direct Mentions, Group Invites). */
    HIGH(4),

    /** Standard precedence; normal event processing (e.g. Likes, Comments, New Followers). */
    NORMAL(3),

    /** Low precedence; processed during idle or grouped (e.g. Story views, Profile views). */
    LOW(2),

    /** Silent processing; background telemetry, typing indicators, state updates without sound or vibration. */
    SILENT(1)
}
