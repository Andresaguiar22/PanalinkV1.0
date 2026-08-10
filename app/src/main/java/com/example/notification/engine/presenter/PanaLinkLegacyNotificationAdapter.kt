package com.example.notification.engine.presenter

import android.content.Context
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
object PanaLinkLegacyNotificationAdapter {

    suspend fun showLegacyMessageNotification(
        context: Context,
        senderId: String,
        senderName: String,
        messageText: String,
        chatId: String
    ) {
        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.CHAT,
            type = NotificationTypeV2.CHAT_MESSAGE,
            priority = NotificationPriority.HIGH,
            interruptiveness = InterruptivenessLevel.HEADS_UP,
            actor = EventActor(id = senderId, name = senderName),
            target = EventTarget(entityId = chatId, entityType = "chat", previewText = messageText),
            title = senderName,
            body = messageText,
            groupingKey = "chat_$chatId"
        )
        NotificationEngine.getInstance().publish(event)

        runCatching {
            val currentUserId = com.example.data.supabase.SupabaseClient.currentUser?.id ?: ""
            com.example.notification.engine.producers.chat.ChatEventPublisher.publishChatMessage(
                chatId = chatId,
                messageId = UUID.randomUUID().toString(),
                recipientUserId = currentUserId,
                senderId = senderId,
                messageText = messageText,
                senderName = senderName
            )
        }
    }

    suspend fun showLegacyCallNotification(
        context: Context,
        callerId: String,
        callerName: String,
        callId: String,
        isIncoming: Boolean
    ) {
        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.CALLS,
            type = if (isIncoming) NotificationTypeV2.CALL_INCOMING else NotificationTypeV2.CALL_MISSED,
            priority = if (isIncoming) NotificationPriority.CRITICAL else NotificationPriority.HIGH,
            interruptiveness = if (isIncoming) InterruptivenessLevel.FULLSCREEN else InterruptivenessLevel.HEADS_UP,
            actor = EventActor(id = callerId, name = callerName),
            target = EventTarget(entityId = callId, entityType = "call"),
            title = if (isIncoming) "Llamada entrante" else "Llamada perdida",
            body = callerName,
            groupingKey = "call_$callId"
        )
        NotificationEngine.getInstance().publish(event)

        runCatching {
            val currentUserId = com.example.data.supabase.SupabaseClient.currentUser?.id ?: ""
            if (isIncoming) {
                com.example.notification.engine.producers.calls.CallEventPublisher.publishIncomingCall(
                    callId = callId,
                    targetUserId = currentUserId,
                    callerId = callerId,
                    callerName = callerName
                )
            } else {
                com.example.notification.engine.producers.calls.CallEventPublisher.publishMissedCall(
                    callId = callId,
                    targetUserId = currentUserId,
                    callerId = callerId,
                    callerName = callerName
                )
            }
        }
    }
}
