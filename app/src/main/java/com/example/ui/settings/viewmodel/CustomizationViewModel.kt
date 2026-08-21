package com.example.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.supabase.SupabaseClient
import com.example.ui.settings.models.CustomizationAction
import com.example.ui.settings.models.CustomizationUiState
import com.example.ui.settings.repository.CustomizationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomizationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CustomizationRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(CustomizationUiState())
    val uiState: StateFlow<CustomizationUiState> = _uiState.asStateFlow()

    private val currentUid: String
        get() = SupabaseClient.currentUser?.id ?: ""

    init {
        loadCustomization()
    }

    fun loadCustomization() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = repository.loadCustomization(currentUid)
            _uiState.value = loaded
        }
    }

    fun dispatch(action: CustomizationAction) {
        when (action) {
            is CustomizationAction.SetThemeMode -> {
                _uiState.update { it.copy(themeMode = action.mode) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveThemeMode(action.mode)
                }
            }
            is CustomizationAction.SetProfileTheme -> {
                _uiState.update { it.copy(profileThemeChoice = action.theme) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveProfileTheme(currentUid, action.theme)
                }
            }
            is CustomizationAction.SetBottomBarColor -> {
                _uiState.update { it.copy(bottomBarColorChoice = action.preset) }
                // Live-apply so the bottom bar reacts instantly, not only after restart.
                com.example.ui.theme.ThemeManager.bottomBarColorPreset.value = action.preset
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveBottomBarPreset(action.preset, _uiState.value.bottomBarShapeChoice)
                }
            }
            is CustomizationAction.SetBottomBarShape -> {
                _uiState.update { it.copy(bottomBarShapeChoice = action.preset) }
                com.example.ui.theme.ThemeManager.bottomBarShapePreset.value = action.preset
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveBottomBarPreset(_uiState.value.bottomBarColorChoice, action.preset)
                }
            }
            is CustomizationAction.UpdateCustomPrimary -> {
                _uiState.update { it.copy(customR = action.r, customG = action.g, customB = action.b) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveCustomPrimary(action.r, action.g, action.b)
                }
            }
            is CustomizationAction.UpdateCustomSecondary -> {
                _uiState.update { it.copy(customSecR = action.r, customSecG = action.g, customSecB = action.b) }
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveCustomSecondary(action.r, action.g, action.b)
                }
            }
            is CustomizationAction.SetMinimalistMode -> {
                _uiState.update { it.copy(isMinimalistMode = action.enabled) }
                com.example.ui.theme.ThemeManager.isMinimalistMode.value = action.enabled
                viewModelScope.launch(Dispatchers.IO) {
                    repository.saveMinimalistMode(action.enabled)
                }
            }
        }
    }
}
