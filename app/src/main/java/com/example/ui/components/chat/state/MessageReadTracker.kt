package com.example.ui.components.chat.state

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import com.example.data.model.Message
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun MessageReadTracker(
    lazyListState: LazyListState,
    messages: List<Message>,
    onMessagesVisible: (List<String>) -> Unit
) {
    if (messages.isEmpty()) return

    LaunchedEffect(lazyListState, messages) {
        snapshotFlow {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) emptyList<String>()
            else {
                visibleItems.mapNotNull { item ->
                    // The item.key is the message.id (or clientMessageUuid)
                    // We need to ensure we only include actual message IDs
                    val key = item.key.toString()
                    if (key.startsWith("temp_") || key == "typing_indicator" || key.contains("date_")) null
                    else key
                }
            }
        }
        .distinctUntilChanged()
        .collectLatest { visibleIds ->
            if (visibleIds.isNotEmpty()) {
                onMessagesVisible(visibleIds)
            }
        }
    }
}
