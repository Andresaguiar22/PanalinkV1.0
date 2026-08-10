package com.example.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.supabase.SupabaseClient
import com.example.ui.settings.models.ChatsSettingsAction
import com.example.ui.settings.models.ChatsSettingsUiState
import com.example.ui.settings.repository.ChatsSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatsSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatsSettingsRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ChatsSettingsUiState())
    val uiState: StateFlow<ChatsSettingsUiState> = _uiState.asStateFlow()

    private val currentUid: String
        get() = SupabaseClient.currentUser?.id ?: ""

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = repository.loadChatsSettings(currentUid)
            _uiState.value = loaded
        }
    }

    fun dispatch(action: ChatsSettingsAction) {
        when (action) {
            is ChatsSettingsAction.UpdateTextSize -> {
                _uiState.update { it.copy(textSize = action.size) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveTextSize(currentUid, action.size)
                }
            }
            is ChatsSettingsAction.SetEnterSends -> {
                _uiState.update { it.copy(enterSends = action.enabled) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveEnterSends(currentUid, action.enabled)
                }
            }
            is ChatsSettingsAction.SetWallpaper -> {
                _uiState.update { it.copy(wallpaper = action.wallpaper) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveWallpaper(currentUid, action.wallpaper)
                }
            }
        }
    }
}
