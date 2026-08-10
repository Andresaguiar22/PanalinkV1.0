package com.example.notification.ui

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.notification.engine.storage.NotificationLocalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface NotificationUiState {
    object Loading : NotificationUiState
    data class Success(
        val notifications: List<NotificationUiModel>,
        val unreadCount: Int
    ) : NotificationUiState
    data class Error(val message: String) : NotificationUiState
}

@Keep
class NotificationCenterViewModelV2(
    private val repository: NotificationLocalRepository
) : ViewModel() {

    val uiState: StateFlow<NotificationUiState> = repository.observeAllNotifications()
        .map { entities ->
            val uiModels = entities.map { NotificationUiMapper.mapToUiModel(it) }
            val unread = entities.count { !it.isRead }
            if (uiModels.isEmpty()) {
                NotificationUiState.Success(emptyList(), 0)
            } else {
                NotificationUiState.Success(uiModels, unread)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = NotificationUiState.Loading
        )

    fun markAsRead(id: String) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    class Factory(private val repository: NotificationLocalRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotificationCenterViewModelV2(repository) as T
        }
    }
}
