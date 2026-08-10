package com.example.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.settings.models.PrivacyAction
import com.example.ui.settings.models.PrivacyUiState
import com.example.ui.settings.repository.PrivacyRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PrivacyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PrivacyRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(PrivacyUiState(isLoading = true))
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val loadedState = repository.loadPrivacySettings()
            _uiState.value = loadedState
        }
    }

    fun dispatch(action: PrivacyAction) {
        when (action) {
            is PrivacyAction.UpdateLastSeen -> {
                repository.saveLastSeen(action.visibility)
                _uiState.update {
                    it.copy(
                        lastSeenVisibility = action.visibility,
                        successMessage = "Última vez actualizada a ${action.visibility}"
                    )
                }
            }
            is PrivacyAction.ToggleReadReceipts -> {
                repository.saveReadReceipts(action.enabled)
                _uiState.update {
                    it.copy(
                        readReceiptsEnabled = action.enabled,
                        successMessage = if (action.enabled) "Confirmaciones de lectura activadas" else "Confirmaciones de lectura desactivadas"
                    )
                }
            }
            is PrivacyAction.ToggleInvisibleMode -> {
                repository.saveInvisibleMode(action.enabled)
                _uiState.update {
                    it.copy(
                        invisibleModeEnabled = action.enabled,
                        successMessage = if (action.enabled) "Modo invisible activado 👻" else "Modo invisible desactivado"
                    )
                }
            }
            is PrivacyAction.ToggleSmartReadReceipts -> {
                repository.saveSmartReadReceipts(action.enabled)
                _uiState.update {
                    it.copy(
                        smartReadReceiptsEnabled = action.enabled,
                        successMessage = if (action.enabled) "Lectura inteligente activada" else "Lectura inteligente desactivada"
                    )
                }
            }
            is PrivacyAction.UpdatePresence -> {
                repository.savePresence(action.status)
                _uiState.update {
                    it.copy(
                        profilePresence = action.status,
                        successMessage = "Estado de presencia actualizado"
                    )
                }
            }
            is PrivacyAction.ClearMessages -> {
                _uiState.update { it.copy(successMessage = null, errorMessage = null) }
            }
        }
    }
}
