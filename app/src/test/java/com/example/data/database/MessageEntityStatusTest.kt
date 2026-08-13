package com.example.data.database

import com.example.data.model.Message
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageEntityStatusTest {
    private fun message(status: String?, deliveredAt: String? = null, seenAt: String? = null) = Message(
        id = "m1",
        chatId = "c1",
        senderId = "u1",
        receiverId = "u2",
        content = "hello",
        createdAt = "2026-08-13T12:00:00Z",
        status = status,
        clientMessageUuid = "uuid-1",
        deliveredAt = deliveredAt,
        seenAt = seenAt
    )

    @Test fun unknownStatusDoesNotBecomeSent() {
        assertEquals("pending", MessageEntity.fromMessage(message("unexpected")).status)
    }

    @Test fun nullStatusDoesNotBecomeSent() {
        assertEquals("pending", MessageEntity.fromMessage(message(null)).status)
    }

    @Test fun seenLegacyValueNormalizesToRead() {
        assertEquals("read", MessageEntity.fromMessage(message("seen")).status)
    }

    @Test fun seenTimestampDominatesLowerStatus() {
        assertEquals("read", MessageEntity.fromMessage(message("sent", seenAt = "2026-08-13T12:01:00Z")).status)
    }

    @Test fun deliveredTimestampDominatesSent() {
        assertEquals("delivered", MessageEntity.fromMessage(message("sent", deliveredAt = "2026-08-13T12:01:00Z")).status)
    }
}
