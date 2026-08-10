package com.example.ui.components.chat.state

import androidx.compose.foundation.lazy.LazyListState
import com.example.data.model.Message
import kotlinx.coroutines.delay

class MessageJumpController(private val lazyListState: LazyListState) {
    suspend fun jumpToMessage(messageId: String, messages: List<Message>): Boolean {
        val index = messages.indexOfFirst { it.id == messageId }
        if (index != -1) {
            lazyListState.animateScrollToItem(index)
            return true
        }
        return false
    }
}
