package com.example.ui.settings.repository

import android.content.Context
import com.example.data.supabase.SupabaseClient
import com.example.ui.settings.models.PresenceUiState
import com.example.ui.settings.models.SettingsKeys

class PresenceRepository(private val context: Context) {

    private fun getPrefs() = context.getSharedPreferences(SettingsKeys.PREFS_NAME, Context.MODE_PRIVATE)

    private fun getCurrentUid(): String {
        return SupabaseClient.currentUser?.id ?: "guest"
    }

    fun loadPresenceSettings(): PresenceUiState {
        val uid = getCurrentUid()
        val prefs = getPrefs()

        val status = prefs.getString(SettingsKeys.profilePresence(uid), "online") ?: "online"
        val lastSeen = prefs.getString(SettingsKeys.privacyLastSeen(uid), "Mis Contactos") ?: "Mis Contactos"
        val invisible = prefs.getBoolean(SettingsKeys.profileInvisibility(uid), false)

        return PresenceUiState(
            status = status,
            lastSeenVisibility = lastSeen,
            isInvisibleMode = invisible,
            lastSeenTimestamp = "Ahora mismo",
            isSyncing = false,
            isLoading = false
        )
    }

    fun savePresenceStatus(status: String) {
        val uid = getCurrentUid()
        getPrefs().edit().putString(SettingsKeys.profilePresence(uid), status).apply()
        
        // Push the manual status into the real presence engine so the 30s
        // heartbeat and DB persistence honor it immediately.
        try {
            com.example.data.repository.PresenceRepository.applyManualStatusFromSettings(status)
        } catch (_: Exception) { }
        try {
            SupabaseClient.broadcastPresence(if (status == "invisible") "offline" else status)
        } catch (_: Exception) { }
    }

    fun saveLastSeenVisibility(visibility: String) {
        val uid = getCurrentUid()
        getPrefs().edit().putString(SettingsKeys.privacyLastSeen(uid), visibility).apply()
    }

    fun saveInvisibleMode(enabled: Boolean) {
        val uid = getCurrentUid()
        getPrefs().edit().putBoolean(SettingsKeys.profileInvisibility(uid), enabled).apply()
        if (enabled) {
            getPrefs().edit().putString(SettingsKeys.profilePresence(uid), "invisible").apply()
            try {
                com.example.data.repository.PresenceRepository.applyManualStatusFromSettings("invisible")
            } catch (_: Exception) { }
            try {
                SupabaseClient.broadcastPresence("offline")
            } catch (_: Exception) { }
        } else {
            getPrefs().edit().putString(SettingsKeys.profilePresence(uid), "online").apply()
            try {
                com.example.data.repository.PresenceRepository.applyManualStatusFromSettings("online")
            } catch (_: Exception) { }
            try {
                SupabaseClient.broadcastPresence("online")
            } catch (_: Exception) { }
        }
    }
}
