package com.example.notification.engine.producers.chat

import androidx.annotation.Keep
import com.example.notification.engine.producers.NotificationEventPublisher

@Keep
object ChatEventPublisher {

    suspend fun publishChatMessage(
        chatId: String,
        messageId: String,
        recipientUserId: String,
        senderId: String,
        messageText: String,
        senderName: String? = null,
        senderAvatarUrl: String? = null
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "CHAT_MESSAGE",
            actorId = senderId,
            targetUserId = recipientUserId,
            entityId = chatId,
            payload = mapOf("message_id" to messageId, "text" to messageText),
            title = senderName ?: "Mensaje nuevo",
            body = messageText,
            domain = "CHAT",
            priority = "HIGH",
            groupingKey = "chat_$chatId",
            actorName = senderName,
            actorAvatarUrl = senderAvatarUrl
        )
    }

    suspend fun publishMessageReaction(
        chatId: String,
        messageId: String,
        recipientUserId: String,
        actorId: String,
        emoji: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "MESSAGE_REACTION",
            actorId = actorId,
            targetUserId = recipientUserId,
            entityId = chatId,
            payload = mapOf("message_id" to messageId, "emoji" to emoji),
            title = actorName?.let { "$it reaccionó con $emoji" } ?: "Reacción a tu mensaje",
            body = "Reaccionó a un mensaje en el chat",
            domain = "CHAT",
            priority = "NORMAL",
            groupingKey = "chat_$chatId",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }

    suspend fun publishChatMention(
        chatId: String,
        messageId: String,
        mentionedUserId: String,
        actorId: String,
        messageSnippet: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "CHAT_MENTION",
            actorId = actorId,
            targetUserId = mentionedUserId,
            entityId = chatId,
            payload = mapOf("message_id" to messageId, "snippet" to messageSnippet),
            title = actorName?.let { "$it te mencionó en el chat" } ?: "Mención en chat",
            body = messageSnippet,
            domain = "CHAT",
            priority = "HIGH",
            groupingKey = "chat_$chatId",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }
}
