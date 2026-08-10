package com.example.notification.engine.producers.social

import androidx.annotation.Keep
import com.example.notification.engine.core.NotificationEngine
import com.example.notification.engine.model.EventActor
import com.example.notification.engine.model.EventTarget
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationAttachment
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2
import java.util.UUID

@Keep
object PostNotificationAdapter {

    private fun getEngine(): NotificationEngine = NotificationEngine.getInstance()

    suspend fun publishPostCreated(
        postId: String,
        authorId: String,
        authorName: String,
        authorAvatarUrl: String? = null,
        caption: String? = null,
        previewUrl: String? = null
    ) {
        val attachments = if (!previewUrl.isNullOrEmpty()) {
            listOf(
                NotificationAttachment(
                    type = NotificationAttachment.AttachmentType.IMAGE,
                    url = previewUrl
                )
            )
        } else emptyList()

        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.POSTS,
            type = NotificationTypeV2.POST_CREATED,
            priority = NotificationPriority.LOW,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(
                id = authorId,
                name = authorName,
                avatarUrl = authorAvatarUrl
            ),
            target = EventTarget(
                entityId = postId,
                entityType = "post",
                title = authorName,
                previewText = caption,
                deepLinkUrl = "panalink://app/post/$postId"
            ),
            title = authorName,
            body = caption ?: "Nueva publicación creada",
            attachments = attachments,
            groupingKey = "post_$postId"
        )
        getEngine().publish(event)
    }

    suspend fun publishPostLike(
        postId: String,
        postAuthorId: String,
        actorId: String,
        actorName: String,
        actorAvatarUrl: String? = null,
        previewUrl: String? = null
    ) {
        val attachments = if (!previewUrl.isNullOrEmpty()) {
            listOf(
                NotificationAttachment(
                    type = NotificationAttachment.AttachmentType.IMAGE,
                    url = previewUrl
                )
            )
        } else emptyList()

        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.POSTS,
            type = NotificationTypeV2.POST_LIKE,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(
                id = actorId,
                name = actorName,
                avatarUrl = actorAvatarUrl
            ),
            target = EventTarget(
                entityId = postId,
                entityType = "post",
                deepLinkUrl = "panalink://app/post/$postId"
            ),
            title = "Muro PanaLink",
            body = "A $actorName le gusta tu publicación",
            attachments = attachments,
            groupingKey = "post_likes_$postId",
            deduplicationKey = "post_like_${actorId}_$postId"
        )
        getEngine().publish(event)

        // Remote bridge to notification_events table
        runCatching {
            com.example.notification.engine.producers.NotificationEventPublisher.publishEvent(
                eventType = "POST_LIKE",
                actorId = actorId,
                targetUserId = postAuthorId,
                entityId = postId,
                title = "Muro PanaLink",
                body = "A $actorName le gusta tu publicación",
                domain = "SOCIAL",
                actorName = actorName,
                actorAvatarUrl = actorAvatarUrl
            )
        }
    }
}
