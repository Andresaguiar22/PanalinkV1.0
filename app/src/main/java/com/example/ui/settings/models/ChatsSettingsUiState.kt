package com.example.ui.settings.models

data class ChatsSettingsUiState(
    val textSize: Float = 15f,
    val enterSends: Boolean = false,
    val wallpaper: String = "dark_slate",
    val isLoading: Boolean = false
)
