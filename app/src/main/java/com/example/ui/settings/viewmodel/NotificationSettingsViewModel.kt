package com.example.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.settings.models.NotificationAction
import com.example.ui.settings.models.NotificationUiState
import com.example.ui.settings.repository.NotificationSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotificationSettingsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = repository.loadSettings()
            _uiState.value = loaded
        }
    }

    fun dispatch(action: NotificationAction) {
        when (action) {
            is NotificationAction.SetGlobalEnabled -> {
                _uiState.update { it.copy(globalEnabled = action.enabled) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveGlobalEnabled(action.enabled)
                }
            }
            is NotificationAction.SetSoundEnabled -> {
                _uiState.update { it.copy(soundEnabled = action.enabled) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveSoundEnabled(action.enabled)
                }
            }
            is NotificationAction.SetSoundTone -> {
                _uiState.update { it.copy(soundTone = action.tone) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveSoundTone(action.tone)
                }
            }
            is NotificationAction.SetVibrationEnabled -> {
                _uiState.update { it.copy(vibrationEnabled = action.enabled) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveVibrationEnabled(action.enabled)
                }
            }
            is NotificationAction.SetVibrationPattern -> {
                _uiState.update { it.copy(vibrationPattern = action.pattern) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveVibrationPattern(action.pattern)
                }
            }
            is NotificationAction.SetChatSoundEnabled -> {
                _uiState.update { it.copy(chatSoundEnabled = action.enabled) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveChatSoundEnabled(action.enabled)
                }
            }
            is NotificationAction.SetChatSoundTone -> {
                _uiState.update { it.copy(chatSoundTone = action.tone) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveChatSoundTone(action.tone)
                }
            }
            is NotificationAction.SetOutgoingSoundEnabled -> {
                _uiState.update { it.copy(outgoingSoundEnabled = action.enabled) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveOutgoingSoundEnabled(action.enabled)
                }
            }
        }
    }
}
