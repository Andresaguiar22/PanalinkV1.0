package com.example.ui.settings.repository

import android.content.Context
import com.example.data.supabase.SupabaseClient
import com.example.ui.settings.models.PrivacyUiState
import com.example.ui.settings.models.SettingsKeys

class PrivacyRepository(private val context: Context) {

    private fun getPrefs() = context.getSharedPreferences(SettingsKeys.PREFS_NAME, Context.MODE_PRIVATE)

    private fun getCurrentUid(): String {
        return SupabaseClient.currentUser?.id ?: "guest"
    }

    fun loadPrivacySettings(): PrivacyUiState {
        val uid = getCurrentUid()
        val prefs = getPrefs()

        val lastSeen = prefs.getString(SettingsKeys.privacyLastSeen(uid), "Mis Contactos") ?: "Mis Contactos"
        val readReceipts = prefs.getBoolean(SettingsKeys.privacyReadReceipts(uid), true)
        val invisibleMode = prefs.getBoolean(SettingsKeys.profileInvisibility(uid), false)
        val smartRead = prefs.getBoolean(SettingsKeys.profileSmartRead(uid), true)
        val presence = prefs.getString(SettingsKeys.profilePresence(uid), "online") ?: "online"

        return PrivacyUiState(
            lastSeenVisibility = lastSeen,
            readReceiptsEnabled = readReceipts,
            invisibleModeEnabled = invisibleMode,
            smartReadReceiptsEnabled = smartRead,
            profilePresence = presence,
            isLoading = false
        )
    }

    fun saveLastSeen(visibility: String) {
        val uid = getCurrentUid()
        getPrefs().edit().putString(SettingsKeys.privacyLastSeen(uid), visibility).apply()
    }

    fun saveReadReceipts(enabled: Boolean) {
        val uid = getCurrentUid()
        getPrefs().edit().putBoolean(SettingsKeys.privacyReadReceipts(uid), enabled).apply()
    }

    fun saveInvisibleMode(enabled: Boolean) {
        val uid = getCurrentUid()
        getPrefs().edit().putBoolean(SettingsKeys.profileInvisibility(uid), enabled).apply()
    }

    fun saveSmartReadReceipts(enabled: Boolean) {
        val uid = getCurrentUid()
        getPrefs().edit().putBoolean(SettingsKeys.profileSmartRead(uid), enabled).apply()
    }

    fun savePresence(status: String) {
        val uid = getCurrentUid()
        getPrefs().edit().putString(SettingsKeys.profilePresence(uid), status).apply()
    }
}
