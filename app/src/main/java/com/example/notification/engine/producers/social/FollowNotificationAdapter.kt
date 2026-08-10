package com.example.notification.engine.producers.social

import androidx.annotation.Keep
import com.example.notification.engine.core.NotificationEngine
import com.example.notification.engine.model.EventActor
import com.example.notification.engine.model.EventTarget
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2
import java.util.UUID

@Keep
object FollowNotificationAdapter {

    private fun getEngine(): NotificationEngine = NotificationEngine.getInstance()

    suspend fun publishFollowRequest(
        targetUserId: String,
        actorId: String,
        actorName: String,
        actorAvatarUrl: String? = null
    ) {
        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.PROFILE,
            type = NotificationTypeV2.USER_FOLLOW_REQUEST,
            priority = NotificationPriority.HIGH,
            interruptiveness = InterruptivenessLevel.HEADS_UP,
            actor = EventActor(
                id = actorId,
                name = actorName,
                avatarUrl = actorAvatarUrl
            ),
            target = EventTarget(
                entityId = actorId,
                entityType = "user",
                deepLinkUrl = "panalink://app/follow_requests"
            ),
            title = "Solicitud de seguimiento",
            body = "$actorName te ha enviado una solicitud para seguirte",
            groupingKey = "follow_requests"
        )
        getEngine().publish(event)
    }

    suspend fun publishFollowedYou(
        targetUserId: String,
        actorId: String,
        actorName: String,
        actorAvatarUrl: String? = null
    ) {
        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.PROFILE,
            type = NotificationTypeV2.USER_FOLLOWED_YOU,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(
                id = actorId,
                name = actorName,
                avatarUrl = actorAvatarUrl
            ),
            target = EventTarget(
                entityId = actorId,
                entityType = "user",
                deepLinkUrl = "panalink://app/profile/$actorId"
            ),
            title = "Nuevo seguidor",
            body = "$actorName comenzó a seguirte",
            groupingKey = "followers"
        )
        getEngine().publish(event)
    }

    suspend fun publishAcceptedFollow(
        targetUserId: String,
        actorId: String,
        actorName: String,
        actorAvatarUrl: String? = null
    ) {
        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.PROFILE,
            type = NotificationTypeV2.USER_ACCEPTED_FOLLOW,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(
                id = actorId,
                name = actorName,
                avatarUrl = actorAvatarUrl
            ),
            target = EventTarget(
                entityId = actorId,
                entityType = "user",
                deepLinkUrl = "panalink://app/profile/$actorId"
            ),
            title = "Solicitud aceptada",
            body = "$actorName aceptó tu solicitud de seguimiento",
            groupingKey = "follow_accepts"
        )
        getEngine().publish(event)
    }
}
