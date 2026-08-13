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
        val normalizedStatus = normalizeDeliveryStatus(message)
        val shouldIncrementUnread = if (message.senderId != myUserId && !isChatActive && normalizedStatus != "read" && message.seenAt == null) 1 else 0

        val chatExists = hasChat(message.chatId) > 0
        if (!chatExists) {
            val otherUserId = if (message.senderId != myUserId) message.senderId else null
            val newChat = ChatEntity(id = message.chatId, createdAt = message.createdAt, type = "dm", otherUserId = otherUserId, lastMessageId = message.id, unreadCount = shouldIncrementUnread)
            insertChatPlaceholder(newChat)
        } else {
            updateChatLastMessageAndUnread(message.chatId, message.id, shouldIncrementUnread)
        }
    }

    @Transaction suspend fun insertMessage(message: MessageEntity) { insertMessageRaw(message); updateChatMetadataForMessage(message) }
    @Transaction suspend fun insertMessages(messages: List<MessageEntity>) { insertMessagesRaw(messages); messages.forEach { updateChatMetadataForMessage(it) } }

    @Query("DELETE FROM local_messages WHERE id = :id") suspend fun deleteMessageById(id: String)
    @Query("DELETE FROM local_messages WHERE clientMessageUuid = :uuid AND id LIKE 'temp_%'") suspend fun deleteTemporaryMessageByUuid(uuid: String)

    private fun localVersionIsNewer(local: MessageEntity, remote: MessageEntity): Boolean {
        val l = local.updatedAt; val r = remote.updatedAt
        return when { l == null && r == null -> false; l == null -> false; r == null -> true; else -> l > r }
    }

    private fun deliveryRank(status: String?): Int = when (status?.lowercase()) {
        "read", "seen" -> 5
        "delivered" -> 4
        "sent" -> 3
        "sending" -> 2
        "pending", "pending_media" -> 1
        "failed" -> 0
        "deleted" -> 6
        else -> 0
    }

    private fun normalizeDeliveryStatus(entity: MessageEntity): String = when {
        !entity.seenAt.isNullOrBlank() -> "read"
        !entity.deliveredAt.isNullOrBlank() -> "delivered"
        entity.status.equals("seen", true) -> "read"
        entity.status.equals("read", true) -> "read"
        entity.status.equals("delivered", true) -> "delivered"
        entity.status.equals("sent", true) -> "sent"
        entity.status.equals("sending", true) -> "sending"
        entity.status.equals("pending", true) || entity.status.equals("pending_media", true) -> "pending"
        entity.status.equals("failed", true) -> "failed"
        entity.status.equals("deleted", true) -> "deleted"
        else -> "pending"
    }

    private fun mergeReactions(localJson: String?, remoteJson: String?, preserveLocal: Boolean): String {
        if (preserveLocal && !localJson.isNullOrEmpty()) return localJson
        return remoteJson ?: localJson ?: ""
    }

    fun mergeEntities(local: MessageEntity, remote: MessageEntity): MessageEntity {
        val localWins = localVersionIsNewer(local, remote)
        val localHasPendingMutation = local.editPending || local.reactionPending || local.deletePending
        val preserveLocalMutation = localWins && localHasPendingMutation
        val localStatus = normalizeDeliveryStatus(local)
        val remoteStatus = normalizeDeliveryStatus(remote)
        val deliveryStatus = if (deliveryRank(localStatus) >= deliveryRank(remoteStatus)) localStatus else remoteStatus
        val finalStatus = when {
            preserveLocalMutation && local.deletePending -> "deleted"
            preserveLocalMutation && local.editPending -> local.status
            remote.status.equals("deleted", true) -> "deleted"
            else -> deliveryStatus
        }
        return remote.copy(
            content = if (preserveLocalMutation && local.editPending) local.content else remote.content,
            status = finalStatus,
            deliveredAt = local.deliveredAt ?: remote.deliveredAt,
            seenAt = local.seenAt ?: remote.seenAt,
            deletedAt = if (preserveLocalMutation && local.deletePending) local.deletedAt ?: remote.deletedAt else remote.deletedAt,
            reactionsJson = mergeReactions(local.reactionsJson, remote.reactionsJson, localWins && local.reactionPending),
            updatedAt = if (localWins) local.updatedAt else remote.updatedAt,
            localMediaUri = local.localMediaUri ?: remote.localMediaUri,
            localThumbnailUri = local.localThumbnailUri ?: remote.localThumbnailUri,
            isFavorited = if (localWins) local.isFavorited else remote.isFavorited,
            clientMessageUuid = remote.clientMessageUuid ?: local.clientMessageUuid,
            editPending = preserveLocalMutation && local.editPending,
            reactionPending = preserveLocalMutation && local.reactionPending,
            deletePending = preserveLocalMutation && local.deletePending,
            ghostOpenedAt = local.ghostOpenedAt ?: remote.ghostOpenedAt
        )
    }

    @Transaction suspend fun insertOrMergeMessage(remote: MessageEntity) { mergeAndSaveMessage(remote) }
    @Transaction suspend fun insertOrMergeMessages(remoteList: List<MessageEntity>) { remoteList.forEach { mergeAndSaveMessage(it) } }

    @Transaction suspend fun mergeAndSaveMessage(remote: MessageEntity) {
        val uuid = remote.clientMessageUuid
        val localByUuid = if (!uuid.isNullOrBlank()) getMessagesByUuid(uuid).firstOrNull() else null
        if (!uuid.isNullOrBlank()) deleteTemporaryMessageByUuid(uuid)
        val local = getMessageById(remote.id) ?: localByUuid
        if (local != null) insertMessage(mergeEntities(local, remote)) else insertMessage(remote)
    }

    @Transaction suspend fun replaceMessageByUuid(finalEntity: MessageEntity) { mergeAndSaveMessage(finalEntity) }

    @Transaction suspend fun replaceTemporaryMessage(tempId: String, finalEntity: MessageEntity) {
        val uuid = finalEntity.clientMessageUuid
        val localTemp = getMessageById(tempId) ?: if (!uuid.isNullOrBlank()) getMessagesByUuid(uuid).firstOrNull() else null
        if (!uuid.isNullOrBlank()) deleteTemporaryMessageByUuid(uuid)
        deleteMessageById(tempId)
        val localById = getMessageById(finalEntity.id)
        val baseLocal = localById ?: localTemp
        insertMessage(if (baseLocal != null) mergeEntities(baseLocal, finalEntity) else finalEntity)
    }

    @Query("UPDATE local_messages SET status = CASE WHEN status = 'read' THEN 'read' WHEN status = 'delivered' AND :status IN ('sent','sending','pending') THEN status WHEN status = 'sent' AND :status IN ('sending','pending') THEN status ELSE :status END WHERE id = :id")
    suspend fun updateMessageStatus(id: String, status: String)

    @Query("UPDATE local_messages SET status = CASE WHEN status IN ('read','delivered') THEN status ELSE :status END, content = :content WHERE id = :id")
    suspend fun updateMessageStatusAndContent(id: String, status: String, content: String)

    @Query("UPDATE local_messages SET status = CASE WHEN status IN ('read','delivered') THEN status ELSE 'delivered' END, deliveredAt = COALESCE(deliveredAt, :deliveredAt) WHERE id = :id")
    suspend fun updateMessageDelivered(id: String, deliveredAt: String, status: String)

    @Query("UPDATE local_messages SET status = 'read', seenAt = COALESCE(seenAt, :seenAt) WHERE id = :id")
    suspend fun updateMessageSeen(id: String, seenAt: String, status: String)

    @Query("UPDATE local_messages SET reactionsJson = :reactionsJson WHERE id = :id") suspend fun updateMessageReactions(id: String, reactionsJson: String)

    @Query("UPDATE local_messages SET status = 'read', seenAt = COALESCE(seenAt, :seenAt) WHERE chatId = :chatId AND senderId != :myUserId AND status != 'read'")
    suspend fun markChatMessagesAsRead(chatId: String, myUserId: String, seenAt: String)

    @Query("UPDATE local_messages SET isFavorited = :isFavorited WHERE id = :id") suspend fun updateMessageFavoriteStatus(id: String, isFavorited: Boolean)
    @Query("SELECT * FROM local_messages WHERE isFavorited = 1 ORDER BY createdAt DESC") fun getFavoritedMessagesFlow(): Flow<List<MessageEntity>>
    @Query("SELECT * FROM local_messages WHERE isFavorited = 1 ORDER BY createdAt DESC") suspend fun getFavoritedMessages(): List<MessageEntity>
    @Query("DELETE FROM local_messages WHERE chatId = :chatId") suspend fun clearChatMessages(chatId: String)
    @Query("UPDATE local_messages SET content = :content, isEdited = 1 WHERE id = :id") suspend fun updateMessageContent(id: String, content: String)
    @Query("UPDATE local_messages SET content = :content, isEdited = 1, editPending = 1 WHERE id = :id") suspend fun markMessageEditPending(id: String, content: String)
    @Query("UPDATE local_messages SET editPending = 0 WHERE id = :id") suspend fun clearMessageEditPending(id: String)
    @Query("UPDATE local_messages SET reactionPending = 1, reactionsJson = :reactionsJson WHERE id = :id") suspend fun markMessageReactionPending(id: String, reactionsJson: String)
    @Query("UPDATE local_messages SET reactionPending = 0 WHERE id = :id") suspend fun clearMessageReactionPending(id: String)
    @Query("UPDATE local_messages SET status = 'deleted', deletedAt = :deletedAt, deletePending = 1 WHERE id = :id") suspend fun markMessageDeletePending(id: String, deletedAt: String)
    @Query("UPDATE local_messages SET deletePending = 0 WHERE id = :id") suspend fun clearMessageDeletePending(id: String)
    @Query("UPDATE local_messages SET ghostOpenedAt = :openedAt WHERE id = :id") suspend fun updateGhostOpenedAt(id: String, openedAt: String)
    @Query("UPDATE local_messages SET receiverId = :receiverId WHERE id = :id") suspend fun updateMessageReceiverId(id: String, receiverId: String)
    @Query("SELECT * FROM local_messages WHERE chatId = :chatId ORDER BY createdAt DESC LIMIT 1") suspend fun getLastMessageForChat(chatId: String): MessageEntity?
    @Query("SELECT COUNT(*) FROM local_messages WHERE chatId = :chatId AND senderId != :myUserId AND status != 'read' AND seenAt IS NULL") suspend fun getUnreadCountForChat(chatId: String, myUserId: String): Int
    @Query("SELECT createdAt FROM local_messages WHERE chatId = :chatId ORDER BY createdAt ASC LIMIT 1") suspend fun getOldestMessageTimestamp(chatId: String): String?
    @Query("SELECT createdAt FROM local_messages WHERE chatId = :chatId ORDER BY createdAt DESC LIMIT 1") suspend fun getNewestMessageTimestamp(chatId: String): String?
}
