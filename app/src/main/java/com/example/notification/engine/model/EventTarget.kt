package com.example.notification.engine.model

import androidx.annotation.Keep

/**
 * Encapsulates the target entity associated with a notification event.
 * (e.g. postId, commentId, chatId, callId, storyId).
 */
@Keep
data class EventTarget(
    val entityId: String,
    val entityType: String,
    val parentEntityId: String? = null,
    val title: String? = null,
    val previewText: String? = null,
    val deepLinkUrl: String? = null
)
