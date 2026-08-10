package com.example.notification.engine.model.social

import androidx.annotation.Keep

@Keep
data class PostEvent(
    val postId: String,
    val authorId: String,
    val actorId: String,
    val actorName: String,
    val actorAvatarUrl: String? = null,
    val action: PostAction,
    val previewUrl: String? = null,
    val postTitleOrCaption: String? = null,
    override val timestamp: Long = System.currentTimeMillis()
) : WallEvent()
