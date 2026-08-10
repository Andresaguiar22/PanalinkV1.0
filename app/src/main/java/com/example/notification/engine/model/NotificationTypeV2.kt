package com.example.notification.engine.model

import androidx.annotation.Keep

/**
 * Granular event types supported by PanaLink Notification Engine V2.
 * Each type maps strictly to a parent NotificationDomain and defines specific payload contracts.
 */
@Keep
enum class NotificationTypeV2(val domain: NotificationDomain) {
    // CHAT Domain
    CHAT_MESSAGE(NotificationDomain.CHAT),
    CHAT_REPLY(NotificationDomain.CHAT),
    CHAT_REACTION(NotificationDomain.CHAT),
    CHAT_MENTION(NotificationDomain.CHAT),
    CHAT_PIN(NotificationDomain.CHAT),
    CHAT_TYPING(NotificationDomain.CHAT),
    CHAT_RECORDING(NotificationDomain.CHAT),

    // SOCIAL / POSTS / COMMENTS Domain
    POST_CREATED(NotificationDomain.POSTS),
    POST_UPDATED(NotificationDomain.POSTS),
    POST_LIKE(NotificationDomain.POSTS),
    POST_REACTION(NotificationDomain.POSTS),
    POST_COMMENT(NotificationDomain.COMMENTS),
    POST_REPLY(NotificationDomain.COMMENTS),
    POST_REPLY_COMMENT(NotificationDomain.COMMENTS),
    POST_SHARE(NotificationDomain.SOCIAL),
    POST_SHARED(NotificationDomain.SOCIAL),
    POST_REPOSTED(NotificationDomain.SOCIAL),
    POST_TAG(NotificationDomain.POSTS),
    POST_FAVORITE(NotificationDomain.POSTS),
    POST_MENTION(NotificationDomain.POSTS),
    COMMENT_MENTION(NotificationDomain.COMMENTS),

    // STORIES Domain
    STORY_REACTION(NotificationDomain.STORIES),
    STORY_REPLY(NotificationDomain.STORIES),
    STORY_VIEW(NotificationDomain.STORIES),
    STORY_MENTION(NotificationDomain.STORIES),

    // REELS Domain
    REEL_LIKE(NotificationDomain.REELS),
    REEL_COMMENT(NotificationDomain.REELS),
    REEL_REPLY(NotificationDomain.REELS),
    REEL_SHARE(NotificationDomain.REELS),

    // CALLS Domain
    CALL_INCOMING(NotificationDomain.CALLS),
    CALL_MISSED(NotificationDomain.CALLS),
    CALL_REJECTED(NotificationDomain.CALLS),
    CALL_ENDED(NotificationDomain.CALLS),
    CALL_BUSY(NotificationDomain.CALLS),

    // PROFILE Domain
    PROFILE_FOLLOW(NotificationDomain.PROFILE),
    PROFILE_UNFOLLOW(NotificationDomain.PROFILE),
    PROFILE_VIEW(NotificationDomain.PROFILE),
    FRIEND_REQUEST(NotificationDomain.PROFILE),
    FRIEND_ACCEPT(NotificationDomain.PROFILE),
    USER_FOLLOW_REQUEST(NotificationDomain.PROFILE),
    USER_FOLLOWED_YOU(NotificationDomain.PROFILE),
    USER_ACCEPTED_FOLLOW(NotificationDomain.PROFILE),

    // SECURITY Domain
    LOGIN_NEW_DEVICE(NotificationDomain.SECURITY),
    PASSWORD_CHANGED(NotificationDomain.SECURITY),
    SESSION_REVOKED(NotificationDomain.SECURITY),
    SECURITY_ALERT(NotificationDomain.SECURITY),

    // SYSTEM Domain
    SYSTEM_ANNOUNCEMENT(NotificationDomain.SYSTEM),
    APP_UPDATE(NotificationDomain.SYSTEM),
    BACKUP_COMPLETED(NotificationDomain.SYSTEM),
    BACKUP_FAILED(NotificationDomain.SYSTEM),

    // UPLOADS Domain
    UPLOAD_PROGRESS(NotificationDomain.UPLOADS),
    UPLOAD_COMPLETED(NotificationDomain.UPLOADS),
    UPLOAD_FAILED(NotificationDomain.UPLOADS),

    // GROUPS & CHANNELS & COMMUNITIES Domain
    GROUP_INVITE(NotificationDomain.GROUPS),
    GROUP_MEMBER_JOINED(NotificationDomain.GROUPS),
    CHANNEL_BROADCAST(NotificationDomain.CHANNELS),
    COMMUNITY_ANNOUNCEMENT(NotificationDomain.COMMUNITIES);

    companion object {
        fun fromString(value: String?): NotificationTypeV2 {
            if (value.isNullOrEmpty()) return SYSTEM_ANNOUNCEMENT
            val safeValue = value.trim()
            return try {
                valueOf(safeValue.uppercase())
            } catch (e: Exception) {
                // Backward compatibility mapping for legacy string types
                when (safeValue.lowercase()) {
                    "like" -> POST_LIKE
                    "comment" -> POST_COMMENT
                    "follow" -> PROFILE_FOLLOW
                    "chat", "message" -> CHAT_MESSAGE
                    "call", "llamada_entrante" -> CALL_INCOMING
                    "call_missed" -> CALL_MISSED
                    "story", "new_story" -> STORY_REPLY
                    "reel", "new_reel" -> REEL_LIKE
                    else -> SYSTEM_ANNOUNCEMENT
                }
            }
        }
    }
}

private fun String?.isNull_orEmpty(): Boolean = this == null || this.trim().isEmpty()
