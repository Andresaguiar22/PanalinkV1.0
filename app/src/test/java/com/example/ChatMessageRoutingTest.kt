package com.example

import com.example.data.model.ThreadMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageRoutingTest {
    @Test
    fun threadMessage_prefersCanonicalChatIdOverThreadId() {
        val message = ThreadMessage(
            id = "11111111-1111-1111-1111-111111111111",
            threadId = "22222222-2222-2222-2222-222222222222",
            chatId = "33333333-3333-3333-3333-333333333333",
            senderId = "44444444-4444-4444-4444-444444444444",
            receiverId = "55555555-5555-5555-5555-555555555555",
            createdAt = "2026-08-16T22:00:00Z",
            clientMessageUuid = "66666666-6666-6666-6666-666666666666"
        )

        assertEquals(message.chatId, message.toMessage().chatId)
    }
}
