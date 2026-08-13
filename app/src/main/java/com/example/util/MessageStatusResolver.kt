package com.example.util

import com.example.data.model.Message

enum class DeliveryState {
    SENDING,
    OFFLINE_PENDING,
    FAILED,
    SENT,
    DELIVERED,
    READ,
    UNKNOWN
}

/**
 * Resolves the visual delivery state from the strongest evidence available.
 * The hierarchy is strictly monotonic: READ > DELIVERED > SENT > pending.
 * Server timestamps are authoritative over textual status values.
 */
object MessageStatusResolver {
    fun resolveMessageDeliveryState(message: Message, isOnline: Boolean): DeliveryState {
        val status = message.status?.trim()?.lowercase()

        // Real server timestamps always win and can never be downgraded by a
        // late/out-of-order status event.
        if (!message.seenAt.isNullOrBlank() || status == "seen" || status == "read") {
            return DeliveryState.READ
        }

        if (!message.deliveredAt.isNullOrBlank() || status == "delivered") {
            return DeliveryState.DELIVERED
        }

        // A non-temporary id is NOT proof that Supabase accepted the message.
        // Do not manufacture SENT merely because temp_ disappeared.
        if (status == "sent") {
            return DeliveryState.SENT
        }

        if (status == "failed") {
            return DeliveryState.FAILED
        }

        if (status == "pending" || status == "sending" || status == "pending_media") {
            return if (isOnline) DeliveryState.SENDING else DeliveryState.OFFLINE_PENDING
        }

        return DeliveryState.UNKNOWN
    }
}
