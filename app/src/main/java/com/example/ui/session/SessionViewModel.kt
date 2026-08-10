package com.example.ui.session

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SessionRepository(application.applicationContext)

    val sessionUiState: StateFlow<SessionUiState> = repository.sessionUiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SessionUiState(
            isAuthenticated = repository.getCurrentUserId().isNotEmpty(),
            userId = repository.getCurrentUserId(),
            email = repository.getCurrentEmail(),
            profile = repository.getCurrentProfile(),
            isLoading = false
        )
    )

    fun refreshProfile() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.refreshProfile()
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.logout()
        }
    }
}
