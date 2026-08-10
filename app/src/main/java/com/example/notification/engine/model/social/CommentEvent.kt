package com.example.notification.engine.model.social

import androidx.annotation.Keep

@Keep
data class CommentEvent(
    val commentId: String,
    val postId: String,
    val postAuthorId: String,
    val actorId: String,
    val actorName: String,
    val actorAvatarUrl: String? = null,
    val commentText: String,
    val isReply: Boolean = false,
    val parentCommentId: String? = null,
    val mediaPreviewUrl: String? = null,
    override val timestamp: Long = System.currentTimeMillis()
) : WallEvent()
