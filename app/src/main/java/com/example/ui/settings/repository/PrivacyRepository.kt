package com.example.ui.settings.repository

import android.content.Context
import android.util.Log
import com.example.data.supabase.SupabaseClient
import com.example.ui.settings.models.PrivacyUiState
import com.example.ui.settings.models.SettingsKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PrivacyRepository(private val context: Context) {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val backendRepo = com.example.data.repository.PrivacyRepository()

    /** Best-effort sync of the setting to the user_privacy_settings backend table. */
    private fun syncToBackend(featureCode: String, value: Map<String, Any>) {
        syncScope.launch {
            try {
                backendRepo.updatePrivacySetting(featureCode, value)
                com.example.data.repository.PrivacyManager.refresh()
            } catch (e: Exception) {
                Log.w("SettingsPrivacyRepository", "Backend sync skipped for " + featureCode + ": " + e.message)
            }
        }
    }

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
        syncToBackend("last_seen_visibility", mapOf("value" to visibility))
    }

    fun saveReadReceipts(enabled: Boolean) {
        val uid = getCurrentUid()
        getPrefs().edit().putBoolean(SettingsKeys.privacyReadReceipts(uid), enabled).apply()
        syncToBackend("read_receipts", mapOf("enabled" to enabled))
    }

    fun saveInvisibleMode(enabled: Boolean) {
        val uid = getCurrentUid()
        getPrefs().edit().putBoolean(SettingsKeys.profileInvisibility(uid), enabled).apply()
        try {
            com.example.data.repository.PresenceRepository.applyManualStatusFromSettings(if (enabled) "invisible" else "online")
        } catch (_: Exception) { }
        syncToBackend("invisible_mode", mapOf("enabled" to enabled))
    }

    fun saveSmartReadReceipts(enabled: Boolean) {
        val uid = getCurrentUid()
        getPrefs().edit().putBoolean(SettingsKeys.profileSmartRead(uid), enabled).apply()
        syncToBackend("smart_read_receipts", mapOf("enabled" to enabled))
    }

    fun savePresence(status: String) {
        val uid = getCurrentUid()
        getPrefs().edit().putString(SettingsKeys.profilePresence(uid), status).apply()
    }
}
