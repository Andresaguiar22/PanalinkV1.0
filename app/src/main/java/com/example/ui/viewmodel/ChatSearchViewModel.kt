package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.PanalinkDatabase
import com.example.data.model.Message
import com.example.ui.components.chat.search.ChatSearchEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ChatSearchUiState {
    object Idle : ChatSearchUiState()
    object Searching : ChatSearchUiState()
    data class Results(val messages: List<Message>) : ChatSearchUiState()
    object Empty : ChatSearchUiState()
    data class Error(val message: String) : ChatSearchUiState()
}

class ChatSearchViewModel(application: Application) : AndroidViewModel(application) {
    private val database = PanalinkDatabase.getDatabase(application)
    private val searchEngine = ChatSearchEngine(database.messageDao())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _uiState = MutableStateFlow<ChatSearchUiState>(ChatSearchUiState.Idle)
    val uiState: StateFlow<ChatSearchUiState> = _uiState.asStateFlow()

    private var currentChatId: String = ""

    fun initSearch(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch {
            searchEngine.search(chatId, _query)
                .onEach { results ->
                    _uiState.value = if (results.isEmpty() && _query.value.isNotBlank()) {
                        ChatSearchUiState.Empty
                    } else if (_query.value.isBlank()) {
                        ChatSearchUiState.Idle
                    } else {
                        ChatSearchUiState.Results(results)
                    }
                }
                .catch { e ->
                    _uiState.value = ChatSearchUiState.Error(e.message ?: "Error en la búsqueda")
                }
                .collect()
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        if (newQuery.isNotBlank()) {
            _uiState.value = ChatSearchUiState.Searching
        } else {
            _uiState.value = ChatSearchUiState.Idle
        }
    }
}
