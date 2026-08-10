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
object ShareNotificationAdapter {

    private fun getEngine(): NotificationEngine = NotificationEngine.getInstance()

    suspend fun publishPostShared(
        postId: String,
        authorId: String,
        actorId: String,
        actorName: String,
        actorAvatarUrl: String? = null,
        isRepost: Boolean = false,
        previewUrl: String? = null
    ) {
        val type = if (isRepost) NotificationTypeV2.POST_REPOSTED else NotificationTypeV2.POST_SHARED
        val body = if (isRepost) "$actorName republicó tu publicación" else "$actorName compartió tu publicación"

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
            domain = NotificationDomain.SOCIAL,
            type = type,
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
            title = "PanaLink Social",
            body = body,
            attachments = attachments,
            groupingKey = "post_shares_$postId"
        )
        getEngine().publish(event)
    }
}
