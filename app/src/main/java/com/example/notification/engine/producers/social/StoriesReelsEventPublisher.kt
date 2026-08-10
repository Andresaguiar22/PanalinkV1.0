package com.example.notification.engine.producers.social

import androidx.annotation.Keep
import com.example.notification.engine.producers.NotificationEventPublisher

@Keep
object StoriesReelsEventPublisher {

    // Stories events
    suspend fun publishStoryLike(
        storyId: String,
        storyAuthorId: String,
        actorId: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "STORY_LIKE",
            actorId = actorId,
            targetUserId = storyAuthorId,
            entityId = storyId,
            title = actorName?.let { "A $it le gustó tu historia" },
            body = "Reaccionó a tu historia",
            domain = "SOCIAL",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }

    suspend fun publishStoryReply(
        storyId: String,
        storyAuthorId: String,
        actorId: String,
        replyText: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "STORY_REPLY",
            actorId = actorId,
            targetUserId = storyAuthorId,
            entityId = storyId,
            payload = mapOf("text" to replyText),
            title = actorName?.let { "$it respondió a tu historia" },
            body = replyText,
            domain = "SOCIAL",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }

    suspend fun publishStoryMention(
        storyId: String,
        mentionedUserId: String,
        actorId: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "STORY_MENTION",
            actorId = actorId,
            targetUserId = mentionedUserId,
            entityId = storyId,
            title = actorName?.let { "$it te mencionó en una historia" },
            body = "Mira la historia antes de que expire",
            domain = "SOCIAL",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }

    // Reels events
    suspend fun publishReelLike(
        reelId: String,
        reelAuthorId: String,
        actorId: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "REEL_LIKE",
            actorId = actorId,
            targetUserId = reelAuthorId,
            entityId = reelId,
            title = actorName?.let { "A $it le gustó tu Reel" },
            body = "Reaccionó a tu video Reel",
            domain = "SOCIAL",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }

    suspend fun publishReelComment(
        reelId: String,
        commentId: String,
        reelAuthorId: String,
        actorId: String,
        commentText: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "REEL_COMMENT",
            actorId = actorId,
            targetUserId = reelAuthorId,
            entityId = reelId,
            payload = mapOf("comment_id" to commentId, "text" to commentText),
            title = actorName?.let { "$it comentó tu Reel" },
            body = commentText,
            domain = "SOCIAL",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }

    suspend fun publishReelShare(
        reelId: String,
        reelAuthorId: String,
        actorId: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "REEL_SHARED",
            actorId = actorId,
            targetUserId = reelAuthorId,
            entityId = reelId,
            title = actorName?.let { "$it compartió tu Reel" },
            body = "Ha compartido tu video con sus amigos",
            domain = "SOCIAL",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }
}
