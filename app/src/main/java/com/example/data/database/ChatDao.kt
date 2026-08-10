package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM local_chats ORDER BY createdAt DESC")
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM local_chats ORDER BY createdAt DESC")
    suspend fun getAllChats(): List<ChatEntity>

    @Query("SELECT * FROM local_chats WHERE id = :id")
    suspend fun getChatById(id: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("UPDATE local_chats SET lastMessageId = :lastMessageId WHERE id = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMessageId: String)

    @Query("UPDATE local_chats SET unreadCount = :unreadCount WHERE id = :chatId")
    suspend fun updateUnreadCount(chatId: String, unreadCount: Int)

    @Query("UPDATE local_chats SET isMuted = :isMuted WHERE id = :chatId")
    suspend fun updateChatMuteStatus(chatId: String, isMuted: Boolean)

    @Query("UPDATE local_chats SET is_pinned = :isPinned, pinned_at = :pinnedAt WHERE id = :chatId")
    suspend fun updateChatPinStatus(chatId: String, isPinned: Boolean, pinnedAt: String?)

    @Query("DELETE FROM local_chats WHERE id = :id")
    suspend fun deleteChatById(id: String)

    @Query("DELETE FROM local_chats")
    suspend fun clearAllChats()
}
