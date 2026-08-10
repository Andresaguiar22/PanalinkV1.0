package com.example.notification.engine.model.social

import androidx.annotation.Keep

@Keep
data class ReactionEvent(
    val entityId: String, // postId or commentId or reelId
    val entityType: String, // "post", "comment", "reel"
    val authorId: String,
    val actorId: String,
    val actorName: String,
    val actorAvatarUrl: String? = null,
    val reactionType: String = "like", // e.g., "like", "love", "haha", "fire"
    val previewUrl: String? = null,
    override val timestamp: Long = System.currentTimeMillis()
) : WallEvent()
