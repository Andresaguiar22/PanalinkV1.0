package com.example.ui.components.chat.state

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import com.example.data.model.Message
import kotlinx.coroutines.launch

@Composable
fun rememberSmartScrollController(
    lazyListState: LazyListState,
    messages: List<Message>,
    currentUserId: String
): SmartScrollController {
    val coroutineScope = rememberCoroutineScope()
    
    val controller = remember(lazyListState, coroutineScope) {
        SmartScrollController(lazyListState)
    }

    // Monitor for new messages
    var lastMessageId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            val latestMessage = messages.last()
            if (lastMessageId != null && latestMessage.id != lastMessageId) {
                val isMe = latestMessage.senderId == currentUserId
                val isAtBottom = controller.isAtBottom()
                
                if (isMe || isAtBottom) {
                    coroutineScope.launch {
                        lazyListState.animateScrollToItem(messages.size - 1)
                    }
                }
            }
            lastMessageId = latestMessage.id
        }
    }

    return controller
}

class SmartScrollController(val lazyListState: LazyListState) {
    fun isAtBottom(threshold: Int = 3): Boolean {
        val layoutInfo = lazyListState.layoutInfo
        val totalItems = layoutInfo.totalItemsCount
        if (totalItems == 0) return true
        val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        return lastVisibleItem >= totalItems - threshold
    }

    suspend fun scrollToBottom(itemCount: Int) {
        if (itemCount > 0) {
            lazyListState.animateScrollToItem(itemCount - 1)
        }
    }
}
