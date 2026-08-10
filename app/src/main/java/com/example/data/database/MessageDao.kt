package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM local_messages WHERE chatId = :chatId AND content LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchMessages(chatId: String, query: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM local_messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    fun getMessagesForChatFlow(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM local_messages WHERE chatId = :chatId ORDER BY createdAt ASC")
    suspend fun getMessagesForChat(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM local_messages WHERE chatId = :chatId AND (:oldestTimestamp IS NULL OR createdAt < :oldestTimestamp) ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getMessagesForChatPaged(chatId: String, limit: Int, oldestTimestamp: String?): List<MessageEntity>

    @Query("SELECT * FROM local_messages WHERE status = 'sending' OR status = 'failed'")
    suspend fun getPendingMessages(): List<MessageEntity>

    @Query("SELECT DISTINCT chatId FROM local_messages")
    suspend fun getDistinctChatIds(): List<String>

    @Query("SELECT * FROM local_messages WHERE clientMessageUuid = :uuid")
    suspend fun getMessagesByUuid(uuid: String): List<MessageEntity>

    @Query("SELECT * FROM local_messages WHERE editPending = 1")
    suspend fun getEditPendingMessages(): List<MessageEntity>

    @Query("SELECT * FROM local_messages WHERE reactionPending = 1")
    suspend fun getReactionPendingMessages(): List<MessageEntity>

    @Query("SELECT * FROM local_messages WHERE deletePending = 1")
    suspend fun getDeletePendingMessages(): List<MessageEntity>

    @Query("SELECT * FROM local_messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessageRaw(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessagesRaw(messages: List<MessageEntity>)

    @Query("UPDATE local_chats SET lastMessageId = :lastMessageId, unreadCount = CASE WHEN :shouldIncrementUnread = 1 THEN unreadCount + 1 ELSE unreadCount END WHERE id = :chatId")
    suspend fun updateChatLastMessageAndUnread(chatId: String, lastMessageId: String, shouldIncrementUnread: Int)

    @Query("SELECT COUNT(*) FROM local_chats WHERE id = :chatId")
    suspend fun hasChat(chatId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChatPlaceholder(chat: ChatEntity)

    @Transaction
    suspend fun updateChatMetadataForMessage(message: MessageEntity) {
        val myUserId = try { com.example.data.supabase.SupabaseClient.currentUser?.id ?: "" } catch (e: Exception) { "" }
        val isChatActive = com.example.data.supabase.SupabaseClient.isChatScreenActive && com.example.data.supabase.SupabaseClient.activeChatId == message.chatId
        val shouldIncrementUnread = if (message.senderId != myUserId && !isChatActive && message.status != "seen" && message.seenAt == null) 1 else 0
        
        val chatExists = hasChat(message.chatId) > 0
        if (!chatExists) {
            val otherUserId = if (message.senderId != myUserId) message.senderId else null
            val newChat = ChatEntity(
                id = message.chatId,
                createdAt = message.createdAt,
                type = "dm",
                otherUserId = otherUserId,
                lastMessageId = message.id,
                unreadCount = shouldIncrementUnread
            )
            insertChatPlaceholder(newChat)
        } else {
            updateChatLastMessageAndUnread(message.chatId, message.id, shouldIncrementUnread)
        }
    }

    @Transaction
    suspend fun insertMessage(message: MessageEntity) {
        insertMessageRaw(message)
        updateChatMetadataForMessage(message)
    }

    @Transaction
    suspend fun insertMessages(messages: List<MessageEntity>) {
        insertMessagesRaw(messages)
        messages.forEach { updateChatMetadataForMessage(it) }
    }

    @Query("DELETE FROM local_messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM local_messages WHERE clientMessageUuid = :uuid AND id LIKE 'temp_%'")
    suspend fun deleteTemporaryMessageByUuid(uuid: String)

    fun isTimestampBeforeOrEqual(ts1: String?, ts2: String?): Boolean {
        if (ts1 == null) return true
        if (ts2 == null) return false
        return ts1 <= ts2
    }

    fun mergeReactions(localJson: String?, remoteJson: String?): String {
        if (localJson.isNullOrEmpty()) return remoteJson ?: ""
        if (remoteJson.isNullOrEmpty()) return localJson ?: ""
        return try {
            val localObj = org.json.JSONObject(localJson)
            val remoteObj = org.json.JSONObject(remoteJson)
            val merged = org.json.JSONObject(remoteJson)
            val keys = localObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                merged.put(key, localObj.get(key))
            }
            merged.toString()
        } catch (e: Exception) {
            localJson
        }
    }

    fun mergeEntities(local: MessageEntity, remote: MessageEntity): MessageEntity {
        val mergedUpdatedAt = if (isTimestampBeforeOrEqual(local.updatedAt, remote.updatedAt)) {
            remote.updatedAt
        } else {
            local.updatedAt
        }

        val finalStatus = when {
            local.deletePending -> "deleted"
            local.editPending -> local.status
            else -> remote.status
        }

        val finalContent = if (local.editPending) {
            local.content
        } else {
            remote.content
        }

        val finalDeletedAt = if (local.deletePending) {
            local.deletedAt ?: remote.deletedAt
        } else {
            remote.deletedAt
        }

        val finalReactionsJson = mergeReactions(local.reactionsJson, remote.reactionsJson)

        return remote.copy(
            content = finalContent,
            status = finalStatus,
            deletedAt = finalDeletedAt,
            reactionsJson = finalReactionsJson,
            updatedAt = mergedUpdatedAt,
            localMediaUri = local.localMediaUri ?: remote.localMediaUri,
            localThumbnailUri = local.localThumbnailUri ?: remote.localThumbnailUri,
            isFavorited = local.isFavorited || remote.isFavorited,
            clientMessageUuid = remote.clientMessageUuid ?: local.clientMessageUuid,
            editPending = local.editPending,
            reactionPending = local.reactionPending,
            deletePending = local.deletePending,
            ghostOpenedAt = local.ghostOpenedAt ?: remote.ghostOpenedAt
        )
    }

    @Transaction
    suspend fun insertOrMergeMessage(remote: MessageEntity) {
        val local = getMessageById(remote.id)
        if (local != null) {
            val merged = mergeEntities(local, remote)
            insertMessage(merged)
        } else {
            insertMessage(remote)
        }
    }

    @Transaction
    suspend fun insertOrMergeMessages(remoteList: List<MessageEntity>) {
        remoteList.forEach { remote ->
            insertOrMergeMessage(remote)
        }
    }

    @Transaction
    suspend fun mergeAndSaveMessage(remote: MessageEntity) {
        val uuid = remote.clientMessageUuid
        var localByUuid: MessageEntity? = null
        if (!uuid.isNullOrBlank()) {
            val found = getMessagesByUuid(uuid)
            localByUuid = found.firstOrNull()
            deleteTemporaryMessageByUuid(uuid)
        }
        val localById = getMessageById(remote.id)
        val local = localById ?: localByUuid
        if (local != null) {
            val merged = mergeEntities(local, remote)
            insertMessage(merged)
        } else {
            insertMessage(remote)
        }
    }

    @Transaction
    suspend fun replaceMessageByUuid(finalEntity: MessageEntity) {
        val uuid = finalEntity.clientMessageUuid
        var localByUuid: MessageEntity? = null
        if (!uuid.isNullOrBlank()) {
            val found = getMessagesByUuid(uuid)
            localByUuid = found.firstOrNull()
            deleteTemporaryMessageByUuid(uuid)
        }
        val localById = getMessageById(finalEntity.id)
        val local = localById ?: localByUuid
        if (local != null) {
            val merged = mergeEntities(local, finalEntity)
            insertMessage(merged)
        } else {
            insertMessage(finalEntity)
        }
    }

    @Transaction
    suspend fun replaceTemporaryMessage(tempId: String, finalEntity: MessageEntity) {
        val uuid = finalEntity.clientMessageUuid
        val localTempByUuid = if (!uuid.isNullOrBlank()) getMessagesByUuid(uuid).firstOrNull() else null
        val localTempById = getMessageById(tempId)
        val localTemp = localTempById ?: localTempByUuid
        
        if (!uuid.isNullOrBlank()) {
            deleteTemporaryMessageByUuid(uuid)
        }
        deleteMessageById(tempId)
        
        val localById = getMessageById(finalEntity.id)
        val baseLocal = localById ?: localTemp
        
        if (baseLocal != null) {
            val merged = mergeEntities(baseLocal, finalEntity)
            insertMessage(merged)
        } else {
            insertMessage(finalEntity)
        }
    }

    @Query("UPDATE local_messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: String, status: String)

    @Query("UPDATE local_messages SET status = :status, content = :content WHERE id = :id")
    suspend fun updateMessageStatusAndContent(id: String, status: String, content: String)

    @Query("UPDATE local_messages SET status = :status, deliveredAt = :deliveredAt WHERE id = :id")
    suspend fun updateMessageDelivered(id: String, deliveredAt: String, status: String)

    @Query("UPDATE local_messages SET status = :status, seenAt = :seenAt WHERE id = :id")
    suspend fun updateMessageSeen(id: String, seenAt: String, status: String)

    @Query("UPDATE local_messages SET reactionsJson = :reactionsJson WHERE id = :id")
    suspend fun updateMessageReactions(id: String, reactionsJson: String)

    @Query("UPDATE local_messages SET status = 'seen', seenAt = :seenAt WHERE chatId = :chatId AND senderId != :myUserId AND status != 'seen'")
    suspend fun markChatMessagesAsRead(chatId: String, myUserId: String, seenAt: String)

    @Query("UPDATE local_messages SET isFavorited = :isFavorited WHERE id = :id")
    suspend fun updateMessageFavoriteStatus(id: String, isFavorited: Boolean)

    @Query("SELECT * FROM local_messages WHERE isFavorited = 1 ORDER BY createdAt DESC")
    fun getFavoritedMessagesFlow(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM local_messages WHERE isFavorited = 1 ORDER BY createdAt DESC")
    suspend fun getFavoritedMessages(): List<MessageEntity>

    @Query("DELETE FROM local_messages WHERE chatId = :chatId")
    suspend fun clearChatMessages(chatId: String)
    
    @Query("UPDATE local_messages SET content = :content, isEdited = 1 WHERE id = :id")
    suspend fun updateMessageContent(id: String, content: String)

    @Query("UPDATE local_messages SET content = :content, isEdited = 1, editPending = 1 WHERE id = :id")
    suspend fun markMessageEditPending(id: String, content: String)

    @Query("UPDATE local_messages SET editPending = 0 WHERE id = :id")
    suspend fun clearMessageEditPending(id: String)

    @Query("UPDATE local_messages SET reactionPending = 1, reactionsJson = :reactionsJson WHERE id = :id")
    suspend fun markMessageReactionPending(id: String, reactionsJson: String)

    @Query("UPDATE local_messages SET reactionPending = 0 WHERE id = :id")
    suspend fun clearMessageReactionPending(id: String)

    @Query("UPDATE local_messages SET status = 'deleted', deletedAt = :deletedAt, deletePending = 1 WHERE id = :id")
    suspend fun markMessageDeletePending(id: String, deletedAt: String)

    @Query("UPDATE local_messages SET deletePending = 0 WHERE id = :id")
    suspend fun clearMessageDeletePending(id: String)

    @Query("UPDATE local_messages SET ghostOpenedAt = :openedAt WHERE id = :id")
    suspend fun updateGhostOpenedAt(id: String, openedAt: String)

    @Query("SELECT * FROM local_messages WHERE chatId = :chatId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLastMessageForChat(chatId: String): MessageEntity?

    @Query("SELECT COUNT(*) FROM local_messages WHERE chatId = :chatId AND senderId != :myUserId AND status != 'seen' AND seenAt IS NULL")
    suspend fun getUnreadCountForChat(chatId: String, myUserId: String): Int

    @Query("SELECT createdAt FROM local_messages WHERE chatId = :chatId ORDER BY createdAt ASC LIMIT 1")
    suspend fun getOldestMessageTimestamp(chatId: String): String?

    @Query("SELECT createdAt FROM local_messages WHERE chatId = :chatId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getNewestMessageTimestamp(chatId: String): String?
}
