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
object CommentNotificationAdapter {

    private fun getEngine(): NotificationEngine = NotificationEngine.getInstance()

    suspend fun publishPostComment(
        postId: String,
        commentId: String,
        postAuthorId: String,
        actorId: String,
        actorName: String,
        actorAvatarUrl: String? = null,
        commentText: String,
        previewUrl: String? = null,
        isReply: Boolean = false
    ) {
        val type = if (isReply) NotificationTypeV2.POST_REPLY_COMMENT else NotificationTypeV2.POST_COMMENT
        val body = if (isReply) "$actorName respondió a tu comentario: $commentText" else "$actorName comentó tu publicación: $commentText"

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
            domain = NotificationDomain.COMMENTS,
            type = type,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(
                id = actorId,
                name = actorName,
                avatarUrl = actorAvatarUrl
            ),
            target = EventTarget(
                entityId = commentId,
                parentEntityId = postId,
                entityType = "comment",
                previewText = commentText,
                deepLinkUrl = "panalink://app/comment/$commentId"
            ),
            title = "Comentarios",
            body = body,
            attachments = attachments,
            groupingKey = "post_comments_$postId"
        )
        getEngine().publish(event)

        // Remote bridge to notification_events table
        runCatching {
            com.example.notification.engine.producers.NotificationEventPublisher.publishEvent(
                eventType = if (isReply) "POST_REPLY" else "POST_COMMENT",
                actorId = actorId,
                targetUserId = postAuthorId,
                entityId = postId,
                payload = mapOf("comment_id" to commentId, "text" to commentText),
                title = "Comentarios",
                body = body,
                domain = "SOCIAL",
                actorName = actorName,
                actorAvatarUrl = actorAvatarUrl
            )
        }
    }
}
