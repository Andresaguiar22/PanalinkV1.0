package com.example.ui.session

import com.example.data.model.Profile

data class SessionUiState(
    val isAuthenticated: Boolean = false,
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val avatarUrl: String? = null,
    val isProfileComplete: Boolean = false,
    val profile: Profile? = null,
    val isOffline: Boolean = false,
    val isLoading: Boolean = true
)
