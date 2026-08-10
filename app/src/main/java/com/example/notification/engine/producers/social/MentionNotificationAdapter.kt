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
object MentionNotificationAdapter {

    private fun getEngine(): NotificationEngine = NotificationEngine.getInstance()

    suspend fun publishMention(
        mentionedUserId: String,
        postId: String,
        actorId: String,
        actorName: String,
        actorAvatarUrl: String? = null,
        isCommentMention: Boolean = false,
        commentId: String? = null,
        contentSnippet: String? = null,
        previewUrl: String? = null
    ) {
        val type = if (isCommentMention) NotificationTypeV2.COMMENT_MENTION else NotificationTypeV2.POST_MENTION
        val deepLink = if (isCommentMention && !commentId.isNullOrEmpty()) {
            "panalink://app/comment/$commentId"
        } else {
            "panalink://app/post/$postId"
        }

        val body = if (isCommentMention) {
            "$actorName te mencionó en un comentario: ${contentSnippet ?: ""}"
        } else {
            "$actorName, te mencionaron en una publicación"
        }

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
            domain = if (isCommentMention) NotificationDomain.COMMENTS else NotificationDomain.POSTS,
            type = type,
            priority = NotificationPriority.HIGH,
            interruptiveness = InterruptivenessLevel.HEADS_UP,
            actor = EventActor(
                id = actorId,
                name = actorName,
                avatarUrl = actorAvatarUrl
            ),
            target = EventTarget(
                entityId = commentId ?: postId,
                parentEntityId = postId,
                entityType = if (isCommentMention) "comment" else "post",
                previewText = contentSnippet,
                deepLinkUrl = deepLink
            ),
            title = "Menciones",
            body = body,
            attachments = attachments,
            groupingKey = "mentions_$mentionedUserId"
        )
        getEngine().publish(event)
    }
}
