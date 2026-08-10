package com.example.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.settings.models.PresenceAction
import com.example.ui.settings.models.PresenceUiState
import com.example.ui.settings.repository.PresenceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PresenceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PresenceRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(PresenceUiState(isLoading = true))
    val uiState: StateFlow<PresenceUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val loadedState = repository.loadPresenceSettings()
            _uiState.value = loadedState
        }
    }

    fun dispatch(action: PresenceAction) {
        when (action) {
            is PresenceAction.ChangePresenceStatus -> {
                repository.savePresenceStatus(action.status)
                _uiState.update {
                    it.copy(
                        status = action.status,
                        isInvisibleMode = (action.status == "invisible"),
                        successMessage = "Estado de presencia actualizado a ${getPresenceLabel(action.status)}"
                    )
                }
            }
            is PresenceAction.ToggleInvisibleMode -> {
                repository.saveInvisibleMode(action.enabled)
                val newStatus = if (action.enabled) "invisible" else "online"
                _uiState.update {
                    it.copy(
                        isInvisibleMode = action.enabled,
                        status = newStatus,
                        successMessage = if (action.enabled) "Modo invisible activado 👻" else "Modo invisible desactivado 🟢"
                    )
                }
            }
            is PresenceAction.UpdateLastSeenVisibility -> {
                repository.saveLastSeenVisibility(action.visibility)
                _uiState.update {
                    it.copy(
                        lastSeenVisibility = action.visibility,
                        successMessage = "Visibilidad de última vez: ${action.visibility}"
                    )
                }
            }
            is PresenceAction.RefreshPresence -> {
                loadSettings()
            }
            is PresenceAction.ClearMessages -> {
                _uiState.update { it.copy(successMessage = null, errorMessage = null) }
            }
        }
    }

    private fun getPresenceLabel(status: String): String {
        return when (status) {
            "online" -> "Disponible 🟢"
            "busy" -> "Ocupado 🔴"
            "invisible" -> "Invisible ⚪"
            else -> status
        }
    }
}
