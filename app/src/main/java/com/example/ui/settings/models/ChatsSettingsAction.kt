package com.example.ui.settings.models

sealed interface ChatsSettingsAction {
    data class UpdateTextSize(val size: Float) : ChatsSettingsAction
    data class SetEnterSends(val enabled: Boolean) : ChatsSettingsAction
    data class SetWallpaper(val wallpaper: String) : ChatsSettingsAction
}
