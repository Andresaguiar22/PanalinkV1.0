package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Message

@Entity(
    tableName = "local_messages",
    indices = [
        androidx.room.Index(value = ["chatId", "createdAt"]),
        androidx.room.Index(value = ["clientMessageUuid"], unique = true)
    ]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val senderId: String,
    val receiverId: String? = null,
    val content: String? = null,
    val createdAt: String,
    val status: String? = "pending",
    val replyToMessageId: String? = null,
    val clientMessageUuid: String? = null,
    val reactionsJson: String = "{}",
    val deliveredAt: String? = null,
    val seenAt: String? = null,
    val thumbnailUrl: String? = null,
    val mediaUrl: String? = null,
    val mediaMime: String? = null,
    val mediaSize: Long? = null,
    val mediaDuration: Long? = null,
    val mediaWidth: Int? = null,
    val mediaHeight: Int? = null,
    val messageType: String? = "text",
    val localMediaUri: String? = null,
    val localThumbnailUri: String? = null,
    val isFavorited: Boolean = false,
    val isEdited: Boolean = false,
    val deletedAt: String? = null,
    val isGhost: Boolean = false,
    val ghostOpenedAt: String? = null,
    val updatedAt: String? = null,
    val editPending: Boolean = false,
    val reactionPending: Boolean = false,
    val deletePending: Boolean = false,
    val musicPlaylistId: String? = null
) {
    fun toMessage(): Message = Message(
        id = id,
        chatId = chatId,
        senderId = senderId,
        receiverId = receiverId,
        content = content ?: "",
        createdAt = createdAt,
        status = resolveStatus(status, deliveredAt, seenAt),
        replyToMessageId = replyToMessageId,
        clientMessageUuid = clientMessageUuid ?: "",
        deliveredAt = deliveredAt,
        seenAt = seenAt,
        thumbnailUrl = thumbnailUrl ?: localThumbnailUri,
        mediaUrl = mediaUrl ?: localMediaUri,
        mediaMime = mediaMime,
        mediaSize = mediaSize,
        duration = mediaDuration,
        width = mediaWidth,
        height = mediaHeight,
        messageType = messageType ?: "text",
        isFavorited = isFavorited,
        isEdited = isEdited,
        deletedAt = deletedAt,
        isGhost = isGhost || content?.startsWith("[Ghost]") == true || messageType == "ghost",
        ghostOpenedAt = ghostOpenedAt,
        updatedAt = updatedAt,
        musicPlaylistId = musicPlaylistId
    )

    companion object {
        fun fromMessage(msg: Message, reactions: String = "{}"): MessageEntity = MessageEntity(
            id = msg.id,
            chatId = msg.chatId,
            senderId = msg.senderId,
            receiverId = msg.receiverId,
            content = msg.content,
            createdAt = msg.createdAt,
            status = resolveStatus(msg.status, msg.deliveredAt, msg.seenAt),
            replyToMessageId = msg.replyToMessageId,
            clientMessageUuid = msg.clientMessageUuid.takeIf { it.isNotBlank() },
            reactionsJson = reactions,
            deliveredAt = msg.deliveredAt,
            seenAt = msg.seenAt,
            thumbnailUrl = msg.thumbnailUrl,
            mediaUrl = msg.mediaUrl,
            mediaMime = msg.mediaMime,
            mediaSize = msg.mediaSize,
            mediaDuration = msg.duration,
            mediaWidth = msg.width,
            mediaHeight = msg.height,
            messageType = msg.messageType ?: "text",
            isFavorited = msg.isFavorited,
            isEdited = msg.isEdited,
            deletedAt = msg.deletedAt,
            isGhost = msg.isGhost || msg.content?.startsWith("[Ghost]") == true || msg.messageType == "ghost",
            ghostOpenedAt = msg.ghostOpenedAt,
            updatedAt = msg.updatedAt,
            musicPlaylistId = msg.musicPlaylistId
        )

        private fun resolveStatus(status: String?, deliveredAt: String?, seenAt: String?): String {
            return when {
                !seenAt.isNullOrBlank() -> "read"
                !deliveredAt.isNullOrBlank() -> "delivered"
                status.equals("seen", ignoreCase = true) -> "read"
                status.equals("read", ignoreCase = true) -> "read"
                status.equals("delivered", ignoreCase = true) -> "delivered"
                status.equals("sent", ignoreCase = true) -> "sent"
                status.equals("sending", ignoreCase = true) -> "sending"
                status.equals("pending", ignoreCase = true) || status.equals("pending_media", ignoreCase = true) -> "pending"
                status.equals("failed", ignoreCase = true) -> "failed"
                status.equals("deleted", ignoreCase = true) -> "deleted"
                else -> "pending"
            }
        }
    }
}
