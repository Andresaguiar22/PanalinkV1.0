package com.example

import com.example.data.database.MessageEntity
import com.example.data.database.MessageDao
import com.example.data.model.Message
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class ExampleUnitTest {

    // Helper fake DAO to test pure/merging logic without DB runner
    private val fakeDao = object : MessageDao {
        override fun searchMessages(chatId: String, query: String) = throw NotImplementedError()
        override fun getMessagesForChatFlow(chatId: String) = throw NotImplementedError()
        override suspend fun getMessagesForChat(chatId: String) = throw NotImplementedError()
        override suspend fun getMessagesForChatPaged(chatId: String, limit: Int, oldestTimestamp: String?) = throw NotImplementedError()
        override suspend fun getPendingMessages() = throw NotImplementedError()
        override suspend fun getDistinctChatIds() = throw NotImplementedError()
        override suspend fun getEditPendingMessages() = throw NotImplementedError()
        override suspend fun getReactionPendingMessages() = throw NotImplementedError()
        override suspend fun getDeletePendingMessages() = throw NotImplementedError()
        override suspend fun getMessagesByUuid(uuid: String) = throw NotImplementedError()
        override suspend fun getMessageById(id: String) = throw NotImplementedError()
        override suspend fun insertMessage(message: MessageEntity) = throw NotImplementedError()
        override suspend fun insertMessages(messages: List<MessageEntity>) = throw NotImplementedError()
        override suspend fun deleteMessageById(id: String) = throw NotImplementedError()
        override suspend fun deleteTemporaryMessageByUuid(uuid: String) = throw NotImplementedError()
        override suspend fun updateMessageStatus(id: String, status: String) = throw NotImplementedError()
        override suspend fun updateMessageStatusAndContent(id: String, status: String, content: String) = throw NotImplementedError()
        override suspend fun updateMessageDelivered(id: String, deliveredAt: String, status: String) = throw NotImplementedError()
        override suspend fun updateMessageSeen(id: String, seenAt: String, status: String) = throw NotImplementedError()
        override suspend fun updateMessageReactions(id: String, reactionsJson: String) = throw NotImplementedError()
        override suspend fun markChatMessagesAsRead(chatId: String, myUserId: String, seenAt: String) = throw NotImplementedError()
        override suspend fun updateMessageFavoriteStatus(id: String, isFavorited: Boolean) = throw NotImplementedError()
        override fun getFavoritedMessagesFlow() = throw NotImplementedError()
        override suspend fun getFavoritedMessages() = throw NotImplementedError()
        override suspend fun clearChatMessages(chatId: String) = throw NotImplementedError()
        override suspend fun updateMessageContent(id: String, content: String) = throw NotImplementedError()
        override suspend fun markMessageEditPending(id: String, content: String) = throw NotImplementedError()
        override suspend fun clearMessageEditPending(id: String) = throw NotImplementedError()
        override suspend fun markMessageReactionPending(id: String, reactionsJson: String) = throw NotImplementedError()
        override suspend fun clearMessageReactionPending(id: String) = throw NotImplementedError()
        override suspend fun markMessageDeletePending(id: String, deletedAt: String) = throw NotImplementedError()
        override suspend fun clearMessageDeletePending(id: String) = throw NotImplementedError()
        override suspend fun updateGhostOpenedAt(id: String, openedAt: String) = throw NotImplementedError()
        override suspend fun getLastMessageForChat(chatId: String) = throw NotImplementedError()
        override suspend fun getUnreadCountForChat(chatId: String, myUserId: String) = throw NotImplementedError()
        override suspend fun getOldestMessageTimestamp(chatId: String) = throw NotImplementedError()
        override suspend fun getNewestMessageTimestamp(chatId: String) = throw NotImplementedError()
    }

    @Test
    fun testOfflineEditMessageMergeStrategy() {
        // Local state has editPending = true and modified content
        val local = MessageEntity(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Hello Local (Edited)",
            status = "sent",
            createdAt = "2026-08-01T12:00:00Z",
            editPending = true
        )

        // Remote comes with older/different text but newer updatedAt
        val remote = MessageEntity(
            id = "msg1",
            chatId = "chat1",
            senderId = "user1",
            content = "Hello Original",
            status = "sent",
            createdAt = "2026-08-01T12:00:00Z",
            updatedAt = "2026-08-01T12:05:00Z"
        )

        val merged = fakeDao.mergeEntities(local, remote)

        // Content must be preserved as local, because editPending is true
        assertEquals("Hello Local (Edited)", merged.content)
        assertTrue(merged.editPending)
        assertEquals("2026-08-01T12:05:00Z", merged.updatedAt)
    }

    @Test
    fun testOfflineDeleteMessageMergeStrategy() {
        // Local has deletePending = true
        val local = MessageEntity(
            id = "msg2",
            chatId = "chat1",
            senderId = "user1",
            content = "To Be Deleted",
            status = "deleted",
            createdAt = "2026-08-01T12:00:00Z",
            deletedAt = "2026-08-01T12:02:00Z",
            deletePending = true
        )

        val remote = MessageEntity(
            id = "msg2",
            chatId = "chat1",
            senderId = "user1",
            content = "To Be Deleted",
            status = "sent",
            createdAt = "2026-08-01T12:00:00Z",
            updatedAt = "2026-08-01T12:01:00Z"
        )

        val merged = fakeDao.mergeEntities(local, remote)

        // Status must remain deleted
        assertEquals("deleted", merged.status)
        assertEquals("2026-08-01T12:02:00Z", merged.deletedAt)
        assertTrue(merged.deletePending)
    }

    @Test
    fun testOfflineReactionMergeStrategy() {
        val local = MessageEntity(
            id = "msg3",
            chatId = "chat1",
            senderId = "user1",
            content = "Cool message",
            status = "sent",
            createdAt = "2026-08-01T12:00:00Z",
            reactionsJson = "{\"user_me\":\"👍\"}",
            reactionPending = true
        )

        val remote = MessageEntity(
            id = "msg3",
            chatId = "chat1",
            senderId = "user1",
            content = "Cool message",
            status = "sent",
            createdAt = "2026-08-01T12:00:00Z",
            reactionsJson = "{\"user_other\":\"❤️\"}"
        )

        val merged = fakeDao.mergeEntities(local, remote)

        // Reactions must be combined
        val reactionsObj = JSONObject(merged.reactionsJson)
        assertEquals("👍", reactionsObj.getString("user_me"))
        assertEquals("❤️", reactionsObj.getString("user_other"))
        assertTrue(merged.reactionPending)
    }

    @Test
    fun testBulkDecryptionPerformanceAndPurity() {
        // Pre-warm to trigger class loading and JIT compilation
        val warmUpMsg = Message(id = "warmup", chatId = "chat1", senderId = "user_other", content = "Warmup", createdAt = "2026-08-01T12:00:00Z")
        com.example.util.CryptoManager.decryptMessagePure(warmUpMsg, "direct", "FakePublicKey")

        // Verify we can decrypt 150 messages bulk without database calls
        val messages = (1..150).map { i ->
            Message(
                id = "msg_$i",
                chatId = "chat1",
                senderId = "user_other",
                content = "EncryptedPayload_$i",
                createdAt = "2026-08-01T12:00:00Z"
            )
        }

        val startTime = System.currentTimeMillis()
        // decryptMessagePure is a pure function. Let's call it directly in a loop to ensure sub-millisecond execution
        val decrypted = messages.map { msg ->
            com.example.util.CryptoManager.decryptMessagePure(msg, "direct", "FakePublicKey")
        }
        val duration = System.currentTimeMillis() - startTime

        assertEquals(150, decrypted.size)
        // Ensure execution of 150 decrypts is extremely fast (under 500ms on a cold container CPU)
        assertTrue("Bulk decryption took too long: ${duration}ms", duration < 500)
    }
}
