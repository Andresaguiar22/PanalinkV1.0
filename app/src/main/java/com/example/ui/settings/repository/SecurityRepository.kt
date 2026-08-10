package com.example.ui.settings.repository

import android.content.Context
import com.example.data.supabase.SupabaseClient
import com.example.ui.settings.models.SecurityUiState
import com.example.ui.settings.models.SettingsKeys

class SecurityRepository(private val context: Context) {

    private fun getPrefs() = context.getSharedPreferences(SettingsKeys.PREFS_NAME, Context.MODE_PRIVATE)

    private fun getCurrentUid(): String {
        return SupabaseClient.currentUser?.id ?: "guest"
    }

    fun loadSecuritySettings(): SecurityUiState {
        val uid = getCurrentUid()
        val prefs = getPrefs()

        val pin = prefs.getString(SettingsKeys.securityPin(uid), "") ?: ""
        val is2Fa = prefs.getBoolean(SettingsKeys.security2Fa(uid), false)
        val isBiometrics = prefs.getBoolean(SettingsKeys.securityBiometrics(uid), false)

        // Generating a consistent 6-digit Pana PIN from UID if none stored remotely
        val PanaPinCode = if (uid != "guest") {
            val hashCode = kotlin.math.abs(uid.hashCode())
            (100000 + (hashCode % 900000)).toString()
        } else "123456"

        return SecurityUiState(
            hasPin = pin.isNotEmpty(),
            pin = pin,
            is2FaEnabled = is2Fa,
            isBiometricsEnabled = isBiometrics,
            userPinCode = PanaPinCode,
            userUid = uid,
            isLoading = false
        )
    }

    fun savePin(pin: String) {
        val uid = getCurrentUid()
        getPrefs().edit().putString(SettingsKeys.securityPin(uid), pin).apply()
    }

    fun removePin() {
        val uid = getCurrentUid()
        getPrefs().edit().remove(SettingsKeys.securityPin(uid)).apply()
    }

    fun save2Fa(enabled: Boolean) {
        val uid = getCurrentUid()
        getPrefs().edit().putBoolean(SettingsKeys.security2Fa(uid), enabled).apply()
    }

    fun saveBiometrics(enabled: Boolean) {
        val uid = getCurrentUid()
        getPrefs().edit().putBoolean(SettingsKeys.securityBiometrics(uid), enabled).apply()
    }
}
