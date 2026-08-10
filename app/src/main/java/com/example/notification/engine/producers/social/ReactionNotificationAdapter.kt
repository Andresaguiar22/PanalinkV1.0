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
object ReactionNotificationAdapter {

    private fun getEngine(): NotificationEngine = NotificationEngine.getInstance()

    suspend fun publishReaction(
        entityId: String,
        entityType: String, // "post", "comment", "reel"
        authorId: String,
        actorId: String,
        actorName: String,
        actorAvatarUrl: String? = null,
        reactionEmojiOrType: String = "❤️",
        previewUrl: String? = null
    ) {
        val body = "$actorName reaccionó con $reactionEmojiOrType a tu ${if (entityType == "comment") "comentario" else "publicación"}"

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
            domain = if (entityType == "comment") NotificationDomain.COMMENTS else NotificationDomain.POSTS,
            type = NotificationTypeV2.POST_REACTION,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(
                id = actorId,
                name = actorName,
                avatarUrl = actorAvatarUrl
            ),
            target = EventTarget(
                entityId = entityId,
                entityType = entityType,
                deepLinkUrl = "panalink://app/post/$entityId"
            ),
            title = "Reacciones",
            body = body,
            attachments = attachments,
            groupingKey = "${entityType}_reactions_$entityId"
        )
        getEngine().publish(event)
    }
}
