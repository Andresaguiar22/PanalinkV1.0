package com.example.ui.components.chat.bubble

import androidx.compose.runtime.Immutable
import com.example.data.model.Message

@Immutable
enum class MessageGroupPosition {
    SINGLE,
    FIRST,
    MIDDLE,
    LAST
}

fun calculateMessageGroupPosition(
    current: Message,
    previous: Message?,
    next: Message?
): MessageGroupPosition {
    val hasPrev = previous != null && previous.senderId == current.senderId
    val hasNext = next != null && next.senderId == current.senderId

    return when {
        !hasPrev && !hasNext -> MessageGroupPosition.SINGLE
        !hasPrev && hasNext -> MessageGroupPosition.FIRST
        hasPrev && hasNext -> MessageGroupPosition.MIDDLE
        else -> MessageGroupPosition.LAST
    }
}
