package com.example.notification.engine.model.social

import androidx.annotation.Keep

@Keep
enum class PostAction {
    CREATE,
    UPDATE,
    LIKE,
    REACTION,
    COMMENT,
    REPLY_COMMENT,
    SHARE,
    REPOST,
    MENTION
}

@Keep
enum class FollowAction {
    REQUEST,
    FOLLOW,
    ACCEPT
}

@Keep
sealed class WallEvent {
    abstract val timestamp: Long
}
