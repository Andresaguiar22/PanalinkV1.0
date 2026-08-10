package com.example.ui.settings.repository

import android.content.Context
import com.example.ui.settings.models.NotificationUiState
import com.example.ui.settings.models.SettingsKeys

class NotificationSettingsRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(SettingsKeys.PREFS_NAME, Context.MODE_PRIVATE)

    fun loadSettings(): NotificationUiState {
        return NotificationUiState(
            globalEnabled = prefs.getBoolean(SettingsKeys.NOTIFICATIONS_GLOBAL_ENABLED, true),
            soundEnabled = prefs.getBoolean(SettingsKeys.NOTIFICATIONS_SOUND_ENABLED, true),
            vibrationEnabled = prefs.getBoolean(SettingsKeys.NOTIFICATIONS_VIBRATION_ENABLED, true),
            soundTone = prefs.getString(SettingsKeys.NOTIFICATIONS_SOUND_TONE, "default") ?: "default",
            vibrationPattern = prefs.getString(SettingsKeys.NOTIFICATIONS_VIBRATION_PATTERN, "default") ?: "default",
            chatSoundEnabled = prefs.getBoolean(SettingsKeys.NOTIFICATIONS_CHAT_SOUND_ENABLED, true),
            chatSoundTone = prefs.getString(SettingsKeys.NOTIFICATIONS_CHAT_SOUND_TONE, "water_drop") ?: "water_drop",
            outgoingSoundEnabled = prefs.getBoolean(SettingsKeys.NOTIFICATIONS_OUTGOING_SOUND_ENABLED, true)
        )
    }

    fun saveGlobalEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(SettingsKeys.NOTIFICATIONS_GLOBAL_ENABLED, enabled).apply()
    }

    fun saveSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(SettingsKeys.NOTIFICATIONS_SOUND_ENABLED, enabled).apply()
    }

    fun saveSoundTone(tone: String) {
        prefs.edit().putString(SettingsKeys.NOTIFICATIONS_SOUND_TONE, tone).apply()
    }

    fun saveVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(SettingsKeys.NOTIFICATIONS_VIBRATION_ENABLED, enabled).apply()
    }

    fun saveVibrationPattern(pattern: String) {
        prefs.edit().putString(SettingsKeys.NOTIFICATIONS_VIBRATION_PATTERN, pattern).apply()
    }

    fun saveChatSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(SettingsKeys.NOTIFICATIONS_CHAT_SOUND_ENABLED, enabled).apply()
    }

    fun saveChatSoundTone(tone: String) {
        prefs.edit().putString(SettingsKeys.NOTIFICATIONS_CHAT_SOUND_TONE, tone).apply()
    }

    fun saveOutgoingSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(SettingsKeys.NOTIFICATIONS_OUTGOING_SOUND_ENABLED, enabled).apply()
    }
}
