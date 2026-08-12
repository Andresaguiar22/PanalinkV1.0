package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    /**
     * Returns one local DM row per remote participant. If stale duplicate DM rows
     * exist from an older sync, the newest row wins. Group/channel chats keep all
     * rows because they intentionally do not have an otherUserId identity key.
     */
    @Query("""
        SELECT c.*
        FROM local_chats c
        WHERE c.type != 'dm'
           OR c.otherUserId IS NULL
           OR NOT EXISTS (
                SELECT 1
                FROM local_chats newer
                WHERE newer.type = 'dm'
                  AND newer.otherUserId = c.otherUserId
                  AND (
                      COALESCE(newer.createdAt, '') > COALESCE(c.createdAt, '')
                      OR (
                          COALESCE(newer.createdAt, '') = COALESCE(c.createdAt, '')
                          AND newer.id > c.id
                      )
                  )
           )
        ORDER BY c.createdAt DESC
    """)
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Query("""
        SELECT c.*
        FROM local_chats c
        WHERE c.type != 'dm'
           OR c.otherUserId IS NULL
           OR NOT EXISTS (
                SELECT 1
                FROM local_chats newer
                WHERE newer.type = 'dm'
                  AND newer.otherUserId = c.otherUserId
                  AND (
                      COALESCE(newer.createdAt, '') > COALESCE(c.createdAt, '')
                      OR (
                          COALESCE(newer.createdAt, '') = COALESCE(c.createdAt, '')
                          AND newer.id > c.id
                      )
                  )
           )
        ORDER BY c.createdAt DESC
    """)
    suspend fun getAllChats(): List<ChatEntity>

    @Query("SELECT * FROM local_chats WHERE id = :id")
    suspend fun getChatById(id: String): ChatEntity?

    @Query("SELECT * FROM local_chats WHERE type = 'dm' AND otherUserId = :otherUserId ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun getDmChatByOtherUserId(otherUserId: String): ChatEntity?

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
