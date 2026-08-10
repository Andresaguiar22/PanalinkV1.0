package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Notification
import com.example.data.model.NotificationType
import com.example.data.model.Profile
import com.example.data.repository.NotificationsRepository
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    data class Success(val notifications: List<Notification>) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

class NotificationsViewModel(
    private val repository: NotificationsRepository = NotificationsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    val unreadCount = repository.unreadCount

    init {
        loadNotifications()
        viewModelScope.launch {
            repository.notifications.collect { list ->
                if (_uiState.value !is NotificationsUiState.Loading || list.isNotEmpty()) {
                    _uiState.value = NotificationsUiState.Success(list)
                }
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = NotificationsUiState.Loading
            val result = repository.fetchNotifications()
            if (result.isFailure) {
                _uiState.value = NotificationsUiState.Error(result.exceptionOrNull()?.message ?: "Error desconocido")
            } else {
                _uiState.value = NotificationsUiState.Success(result.getOrDefault(emptyList()))
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
            _uiState.value = NotificationsUiState.Success(emptyList())
        }
    }
    
    fun clearNotification(notificationId: String) {
        viewModelScope.launch {
            repository.clearNotification(notificationId)
        }
    }
}
