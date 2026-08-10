package com.example.notification.engine.core

import androidx.annotation.Keep
import com.example.notification.engine.model.EventActor
import com.example.notification.engine.model.EventTarget
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2
import java.util.UUID

@Keep
object NotificationProducerAdapters {

    private fun getEngine(): NotificationEngine {
        return NotificationEngine.getInstance()
    }

    suspend fun publishChatMessage(
        messageId: String = UUID.randomUUID().toString(),
        chatId: String,
        senderId: String,
        senderName: String,
        senderAvatarUrl: String? = null,
        messageText: String,
        chatTitle: String = senderName
    ) {
        val event = NotificationEvent(
            id = messageId,
            domain = NotificationDomain.CHAT,
            type = NotificationTypeV2.CHAT_MESSAGE,
            priority = NotificationPriority.HIGH,
            interruptiveness = InterruptivenessLevel.HEADS_UP,
            actor = EventActor(
                id = senderId,
                name = senderName,
                avatarUrl = senderAvatarUrl
            ),
            target = EventTarget(
                entityId = chatId,
                entityType = "chat",
                title = chatTitle,
                previewText = messageText,
                deepLinkUrl = "panalink://app/chat/$chatId"
            ),
            title = senderName,
            body = messageText,
            groupingKey = "chat_$chatId"
        )
        getEngine().publish(event)
    }

    suspend fun publishStoryViewOrReaction(
        storyId: String,
        actorId: String,
        actorName: String,
        actorAvatarUrl: String? = null,
        isReaction: Boolean = false,
        reactionEmoji: String? = null
    ) {
        val type = if (isReaction) NotificationTypeV2.STORY_REACTION else NotificationTypeV2.STORY_VIEW
        val body = if (isReaction) "$actorName reaccionó con $reactionEmoji a tu historia" else "$actorName vio tu historia"

        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.STORIES,
            type = type,
            priority = if (isReaction) NotificationPriority.NORMAL else NotificationPriority.LOW,
            interruptiveness = if (isReaction) NotificationPriority.NORMAL.toInterruptiveness() else InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(
                id = actorId,
                name = actorName,
                avatarUrl = actorAvatarUrl
            ),
            target = EventTarget(
                entityId = storyId,
                entityType = "story",
                deepLinkUrl = "panalink://app/story_viewer/$storyId"
            ),
            title = "Historias",
            body = body,
            groupingKey = "story_$storyId"
        )
        getEngine().publish(event)
    }

    suspend fun publishReelLikeOrComment(
        reelId: String,
        actorId: String,
        actorName: String,
        actorAvatarUrl: String? = null,
        isComment: Boolean = false,
        commentText: String? = null
    ) {
        val type = if (isComment) NotificationTypeV2.REEL_COMMENT else NotificationTypeV2.REEL_LIKE
        val body = if (isComment) "$actorName comentó: $commentText" else "A $actorName le gustó tu reel"

        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.REELS,
            type = type,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(
                id = actorId,
                name = actorName,
                avatarUrl = actorAvatarUrl
            ),
            target = EventTarget(
                entityId = reelId,
                entityType = "reel",
                previewText = commentText,
                deepLinkUrl = "panalink://app/reel_viewer/$reelId"
            ),
            title = "Reels",
            body = body,
            groupingKey = "reel_$reelId"
        )
        getEngine().publish(event)
    }

    suspend fun publishCallEvent(
        callId: String,
        callerId: String,
        callerName: String,
        callerAvatarUrl: String? = null,
        isIncoming: Boolean = true
    ) {
        val type = if (isIncoming) NotificationTypeV2.CALL_INCOMING else NotificationTypeV2.CALL_MISSED
        val body = if (isIncoming) "Llamada entrante de $callerName" else "Llamada perdida de $callerName"

        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.CALLS,
            type = type,
            priority = if (isIncoming) NotificationPriority.CRITICAL else NotificationPriority.HIGH,
            interruptiveness = if (isIncoming) InterruptivenessLevel.FULLSCREEN else InterruptivenessLevel.HEADS_UP,
            actor = EventActor(
                id = callerId,
                name = callerName,
                avatarUrl = callerAvatarUrl
            ),
            target = EventTarget(
                entityId = callId,
                entityType = "call",
                deepLinkUrl = "panalink://app/calls_history"
            ),
            title = "Llamada PanaLink",
            body = body,
            groupingKey = "call_$callId"
        )
        getEngine().publish(event)
    }

    suspend fun publishUploadStatus(
        taskId: String,
        isSuccess: Boolean,
        fileName: String,
        errorMessage: String? = null
    ) {
        val type = if (isSuccess) NotificationTypeV2.UPLOAD_COMPLETED else NotificationTypeV2.UPLOAD_FAILED
        val body = if (isSuccess) "La carga de $fileName se completó exitosamente" else "Falló la carga de $fileName: ${errorMessage ?: "Error desconocido"}"

        val event = NotificationEvent(
            id = taskId,
            domain = NotificationDomain.UPLOADS,
            type = type,
            priority = NotificationPriority.LOW,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            title = if (isSuccess) "Carga completada" else "Error de carga",
            body = body,
            groupingKey = "uploads"
        )
        getEngine().publish(event)
    }

    private fun NotificationPriority.toInterruptiveness(): InterruptivenessLevel {
        return when (this) {
            NotificationPriority.CRITICAL -> InterruptivenessLevel.FULLSCREEN
            NotificationPriority.HIGH -> InterruptivenessLevel.HEADS_UP
            NotificationPriority.NORMAL -> InterruptivenessLevel.STATUS_BAR_ONLY
            NotificationPriority.LOW, NotificationPriority.SILENT -> InterruptivenessLevel.SILENT
        }
    }
}
