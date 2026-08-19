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

object MessageStatusResolver {
    fun resolveMessageDeliveryState(message: Message, isOnline: Boolean): DeliveryState {
        // Trust the persisted delivery state. The message ID only identifies
        // the server record; it does not prove that the latest local state is
        // successfully synchronized.
        val effectiveStatus = message.status

        // 1. If it has seenAt or status is read/seen, it's strictly READ
        if (!message.seenAt.isNullOrEmpty() || effectiveStatus == "seen" || effectiveStatus == "read") {
            return DeliveryState.READ
        }

        // 2. If it has deliveredAt or status is delivered, it's DELIVERED
        if (!message.deliveredAt.isNullOrEmpty() || effectiveStatus == "delivered") {
            return DeliveryState.DELIVERED
        }

        // 3. Status "sent" means Supabase accepted it. It is unequivocally SENT.
        if (effectiveStatus == "sent") {
            return DeliveryState.SENT
        }

        // 4. Errors
        if (effectiveStatus == "failed") {
            return DeliveryState.FAILED
        }

        // 5. If it's pending/sending and we're offline -> OFFLINE_PENDING
        if (effectiveStatus == "pending" || effectiveStatus == "sending" || effectiveStatus == "pending_media") {
            if (!isOnline) {
                return DeliveryState.OFFLINE_PENDING
            }
            return DeliveryState.SENDING
        }

        return DeliveryState.UNKNOWN
    }
}
