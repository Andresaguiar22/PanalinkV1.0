package com.example.notification.engine.model.social

import androidx.annotation.Keep

@Keep
data class FollowEvent(
    val targetUserId: String,
    val actorId: String,
    val actorName: String,
    val actorAvatarUrl: String? = null,
    val action: FollowAction,
    override val timestamp: Long = System.currentTimeMillis()
) : WallEvent()
