package com.example.ui.components.chat.search

import com.example.data.database.MessageDao
import com.example.data.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*

class ChatSearchEngine(private val messageDao: MessageDao) {

    @OptIn(FlowPreview::class)
    fun search(chatId: String, queryFlow: Flow<String>): Flow<List<Message>> {
        return queryFlow
            .debounce(300)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(emptyList())
                } else {
                    messageDao.searchMessages(chatId, query)
                        .map { entities ->
                            entities.map { it.toMessage() }
                        }
                        .flowOn(Dispatchers.IO)
                }
            }
    }
}
