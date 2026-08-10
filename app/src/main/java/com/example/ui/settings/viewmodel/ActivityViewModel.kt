package com.example.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.settings.models.ActivityAction
import com.example.ui.settings.models.ActivityUiState
import com.example.ui.settings.repository.ActivityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ActivityRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ActivityUiState(isLoading = true))
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        loadActivitySummary()
    }

    fun loadActivitySummary() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val summary = repository.loadActivitySummary()
                _uiState.value = summary
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Error al cargar diagnósticos"
                    )
                }
            }
        }
    }

    fun dispatch(action: ActivityAction) {
        when (action) {
            is ActivityAction.RefreshSummary,
            is ActivityAction.LoadDiagnostics,
            is ActivityAction.RefreshStorage,
            is ActivityAction.RefreshDevices -> {
                loadActivitySummary()
            }
            is ActivityAction.ClearError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
        }
    }
}
