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
        // Priority hierarchy: READ > DELIVERED > SENT > FAILED > OFFLINE_PENDING > SENDING
        
        // Force server-confirmed messages to NOT be pending/sending
        val isServerConfirmed = !message.id.startsWith("temp_")
        val effectiveStatus = if (isServerConfirmed && (message.status == "pending" || message.status == "sending" || message.status == "pending_media")) {
            "sent"
        } else {
            message.status
        }
        
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
