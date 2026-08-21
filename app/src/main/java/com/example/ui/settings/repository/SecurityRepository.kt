package com.example.ui.settings.repository

import android.content.Context
import com.example.data.supabase.SupabaseClient
import com.example.security.AppLockManager
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

        val hasPin = AppLockManager.hasPin(context, uid)
        val hasPattern = AppLockManager.hasPattern(context, uid)
        val is2Fa = prefs.getBoolean(SettingsKeys.security2Fa(uid), false)
        val isBiometrics = prefs.getBoolean(SettingsKeys.securityBiometrics(uid), false)
        val autoLockMs = AppLockManager.autoLockDelayMs(context, uid)
        val biometricsAvailable = AppLockManager.canUseBiometrics(context)

        // Generating a consistent 6-digit Pana PIN from UID if none stored remotely
        val PanaPinCode = if (uid != "guest") {
            val hashCode = kotlin.math.abs(uid.hashCode())
            (100000 + (hashCode % 900000)).toString()
        } else "123456"

        return SecurityUiState(
            hasPin = hasPin,
            hasPattern = hasPattern,
            pin = "",
            is2FaEnabled = is2Fa,
            isBiometricsEnabled = isBiometrics,
            biometricsAvailable = biometricsAvailable,
            autoLockMs = autoLockMs,
            userPinCode = PanaPinCode,
            userUid = uid,
            isLoading = false
        )
    }

    fun savePin(pin: String) = AppLockManager.setPin(context, pin, getCurrentUid())

    fun removePin() = AppLockManager.removePin(context, getCurrentUid())

    fun savePattern(pattern: List<Int>) = AppLockManager.setPattern(context, pattern, getCurrentUid())

    fun removePattern() = AppLockManager.removePattern(context, getCurrentUid())

    fun saveAutoLock(delayMs: Long) = AppLockManager.setAutoLockDelayMs(context, delayMs, getCurrentUid())

    fun save2Fa(enabled: Boolean) {
        val uid = getCurrentUid()
        getPrefs().edit().putBoolean(SettingsKeys.security2Fa(uid), enabled).apply()
    }

    fun saveBiometrics(enabled: Boolean) {
        AppLockManager.setBiometricsEnabled(context, enabled, getCurrentUid())
    }
}
