package com.example.ui.settings.repository

import android.content.Context
import com.example.ui.settings.models.ChatsSettingsUiState
import com.example.ui.settings.models.SettingsKeys

class ChatsSettingsRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(SettingsKeys.PREFS_NAME, Context.MODE_PRIVATE)

    fun loadChatsSettings(uid: String): ChatsSettingsUiState {
        val size = prefs.getFloat(SettingsKeys.chatTextSize(uid), 15f)
        val enterSends = prefs.getBoolean(SettingsKeys.chatEnterSends(uid), false)
        val wallpaper = prefs.getString(SettingsKeys.chatWallpaper(uid), "dark_slate") ?: "dark_slate"
        return ChatsSettingsUiState(
            textSize = size,
            enterSends = enterSends,
            wallpaper = wallpaper
        )
    }

    fun saveTextSize(uid: String, size: Float) {
        prefs.edit().putFloat(SettingsKeys.chatTextSize(uid), size).apply()
    }

    fun saveEnterSends(uid: String, enabled: Boolean) {
        prefs.edit().putBoolean(SettingsKeys.chatEnterSends(uid), enabled).apply()
    }

    fun saveWallpaper(uid: String, wallpaper: String) {
        prefs.edit().putString(SettingsKeys.chatWallpaper(uid), wallpaper).apply()
    }
}
