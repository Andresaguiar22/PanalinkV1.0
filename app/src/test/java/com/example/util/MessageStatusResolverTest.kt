package com.example.util

import com.example.data.model.Message
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageStatusResolverTest {

    private fun message(
        status: String? = null,
        deliveredAt: String? = null,
        seenAt: String? = null
    ) = Message(
        id = "m1",
        chatId = "c1",
        senderId = "u1",
        receiverId = "u2",
        content = "hello",
        createdAt = "2026-08-13T12:00:00Z",
        status = status,
        deliveredAt = deliveredAt,
        seenAt = seenAt
    )

    @Test
    fun `read timestamp outranks delivered status`() {
        val state = MessageStatusResolver.resolveMessageDeliveryState(message("delivered", seenAt = "2026-08-13T12:00:00Z"), true)
        assertEquals(DeliveryState.READ, state)
    }

    @Test
    fun `legacy seen status is normalized to read`() {
        val state = MessageStatusResolver.resolveMessageDeliveryState(message("seen"), true)
        assertEquals(DeliveryState.READ, state)
    }

    @Test
    fun `delivered timestamp outranks sent status`() {
        val state = MessageStatusResolver.resolveMessageDeliveryState(message("sent", deliveredAt = "2026-08-13T12:00:00Z"), true)
        assertEquals(DeliveryState.DELIVERED, state)
    }

    @Test
    fun `missing confirmation does not manufacture sent from ordinary id`() {
        val state = MessageStatusResolver.resolveMessageDeliveryState(message(null), true)
        assertEquals(DeliveryState.UNKNOWN, state)
    }

    @Test
    fun `pending becomes offline pending while disconnected`() {
        val state = MessageStatusResolver.resolveMessageDeliveryState(message("pending"), false)
        assertEquals(DeliveryState.OFFLINE_PENDING, state)
    }
}
