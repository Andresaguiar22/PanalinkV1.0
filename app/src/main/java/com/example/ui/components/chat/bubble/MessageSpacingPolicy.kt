package com.example.ui.components.chat.bubble

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.Message
import com.example.data.model.formatIsoDateTime

/**
 * MessageSpacingPolicy
 * Pure functional policy for calculating spacing between chat messages in Panalink Chat 3.0.
 *
 * Rules:
 * - Consecutive messages from same sender: 4.dp
 * - Change of sender: 12.dp
 * - Change of date: 24.dp
 */
@Immutable
object MessageSpacingPolicy {

    /**
     * Calculates the vertical spacing/padding above [current] message based on [previous] and [next] messages.
     * Pure function without state, side-effects, or Compose recomposition overhead.
     */
    fun calculateMessageSpacing(
        current: Message,
        previous: Message?,
        next: Message?
    ): Dp {
        if (previous == null) {
            return 12.dp
        }

        // Check for date change
        val currentDate = formatIsoDateTime(current.createdAt).split(" ").firstOrNull() ?: ""
        val previousDate = formatIsoDateTime(previous.createdAt).split(" ").firstOrNull() ?: ""

        if (currentDate.isNotEmpty() && previousDate.isNotEmpty() && currentDate != previousDate) {
            return 24.dp
        }

        // Compare senders
        return if (current.senderId == previous.senderId) {
            4.dp
        } else {
            12.dp
        }
    }
}
