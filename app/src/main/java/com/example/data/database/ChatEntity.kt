package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Chat

@Entity(tableName = "local_chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val createdAt: String?,
    val type: String = "dm",
    val otherUserId: String? = null, // Cache other member ID for easy retrieval
    val lastMessageId: String? = null,
    val unreadCount: Int = 0,
    val name: String? = null,
    val description: String? = null,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val visibility: String = "private",
    val isReadonly: Boolean = false,
    val ownerId: String? = null,
    val isArchived: Boolean = false,
    val isMuted: Boolean = false,
    @androidx.room.ColumnInfo(name = "is_pinned") val isPinned: Boolean = false,
    @androidx.room.ColumnInfo(name = "pinned_at") val pinnedAt: String? = null
) {
    fun toChat(): Chat {
        otherUserId?.let {
            com.example.util.CryptoManager.chatToOtherUserCache[id] = it
        }
        return Chat(
            id = id,
            createdAt = createdAt,
            type = type,
            name = name,
            description = description,
            avatarUrl = avatarUrl,
            coverUrl = coverUrl,
            visibility = visibility,
            isReadonly = isReadonly,
            ownerId = ownerId,
            isArchived = isArchived,
            isMuted = isMuted,
            isPinned = isPinned,
            pinnedAt = pinnedAt
        )
    }

    companion object {
        fun fromChat(chat: Chat, otherUserId: String? = null, lastMessageId: String? = null, unreadCount: Int = 0): ChatEntity {
            otherUserId?.let {
                com.example.util.CryptoManager.chatToOtherUserCache[chat.id] = it
            }
            return ChatEntity(
                id = chat.id,
                createdAt = chat.createdAt,
                type = chat.type,
                otherUserId = otherUserId,
                lastMessageId = lastMessageId,
                unreadCount = unreadCount,
                name = chat.name,
                description = chat.description,
                avatarUrl = chat.avatarUrl,
                coverUrl = chat.coverUrl,
                visibility = chat.visibility,
                isReadonly = chat.isReadonly,
                ownerId = chat.ownerId,
                isArchived = chat.isArchived,
                isMuted = chat.isMuted,
                isPinned = chat.isPinned,
                pinnedAt = chat.pinnedAt
            )
        }
    }
}
