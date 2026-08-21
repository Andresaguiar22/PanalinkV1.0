package com.example.security

import android.content.Context
import android.content.SharedPreferences
import com.example.data.supabase.SupabaseClient
import com.example.ui.settings.models.SettingsKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/**
 * Real app-lock engine. Stores only salted SHA-256 hashes of the PIN/pattern
 * (never the raw values) and drives the global locked state consumed by
 * MainActivity's lock overlay.
 */
object AppLockManager {

    enum class LockMethod { NONE, PIN, PATTERN }

    private const val HASH_PREFIX = "panalink_lock_v1"

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var backgroundedAtMs: Long = 0L
    private var initialized = false

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(SettingsKeys.PREFS_NAME, Context.MODE_PRIVATE)

    private fun currentUid(): String = SupabaseClient.currentUser?.id ?: "guest"

    private fun hash(uid: String, secret: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$HASH_PREFIX|$uid|$secret".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ---------------- Setup / state ----------------

    fun hasPin(context: Context, uid: String = currentUid()): Boolean =
        !prefs(context).getString(SettingsKeys.securityPin(uid), null).isNullOrEmpty()

    fun hasPattern(context: Context, uid: String = currentUid()): Boolean =
        !prefs(context).getString(SettingsKeys.securityPattern(uid), null).isNullOrEmpty()

    fun lockMethod(context: Context, uid: String = currentUid()): LockMethod = when {
        hasPattern(context, uid) -> LockMethod.PATTERN
        hasPin(context, uid) -> LockMethod.PIN
        else -> LockMethod.NONE
    }

    fun isBiometricsEnabled(context: Context, uid: String = currentUid()): Boolean =
        prefs(context).getBoolean(SettingsKeys.securityBiometrics(uid), false)

    /** True only when the device actually has enrolled biometric hardware. */
    fun canUseBiometrics(context: Context): Boolean {
        val bm = androidx.biometric.BiometricManager.from(context)
        return bm.canAuthenticate(androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
    }

    /** Auto-lock delay in milliseconds. 0 = lock immediately on background. */
    fun autoLockDelayMs(context: Context, uid: String = currentUid()): Long =
        prefs(context).getLong(SettingsKeys.securityAutoLock(uid), 0L)

    fun isProtectionConfigured(context: Context): Boolean =
        lockMethod(context) != LockMethod.NONE

    // ---------------- PIN / Pattern management ----------------

    fun setPin(context: Context, pin: String, uid: String = currentUid()) {
        prefs(context).edit().putString(SettingsKeys.securityPin(uid), hash(uid, pin)).apply()
    }

    fun removePin(context: Context, uid: String = currentUid()) {
        prefs(context).edit().remove(SettingsKeys.securityPin(uid)).apply()
    }

    fun setPattern(context: Context, pattern: List<Int>, uid: String = currentUid()) {
        val serialized = pattern.joinToString("-")
        prefs(context).edit().putString(SettingsKeys.securityPattern(uid), hash(uid, serialized)).apply()
    }

    fun removePattern(context: Context, uid: String = currentUid()) {
        prefs(context).edit().remove(SettingsKeys.securityPattern(uid)).apply()
    }

    fun setBiometricsEnabled(context: Context, enabled: Boolean, uid: String = currentUid()) {
        prefs(context).edit().putBoolean(SettingsKeys.securityBiometrics(uid), enabled).apply()
    }

    fun setAutoLockDelayMs(context: Context, delayMs: Long, uid: String = currentUid()) {
        prefs(context).edit().putLong(SettingsKeys.securityAutoLock(uid), delayMs).apply()
    }

    // ---------------- Verification ----------------

    fun verifyPin(context: Context, pin: String, uid: String = currentUid()): Boolean {
        val stored = prefs(context).getString(SettingsKeys.securityPin(uid), null) ?: return false
        // Legacy migration: builds before hashing stored the PIN in plain text.
        if (stored.length != 64 || stored.any { it !in '0'..'9' && it !in 'a'..'f' }) {
            val legacyMatch = stored == pin
            if (legacyMatch) setPin(context, pin, uid)
            return legacyMatch
        }
        return stored == hash(uid, pin)
    }

    fun verifyPattern(context: Context, pattern: List<Int>, uid: String = currentUid()): Boolean {
        val stored = prefs(context).getString(SettingsKeys.securityPattern(uid), null) ?: return false
        return stored == hash(uid, pattern.joinToString("-"))
    }

    // ---------------- Lock lifecycle ----------------

    /** Called once from MainActivity.onCreate: cold start always locks if protected. */
    fun onAppLaunched(context: Context) {
        initialized = true
        _isLocked.value = isProtectionConfigured(context)
    }

    fun onAppBackgrounded() {
        backgroundedAtMs = System.currentTimeMillis()
    }

    fun onAppForegrounded(context: Context) {
        if (!initialized) onAppLaunched(context)
        if (!isProtectionConfigured(context)) {
            _isLocked.value = false
            return
        }
        val elapsed = if (backgroundedAtMs == 0L) Long.MAX_VALUE else System.currentTimeMillis() - backgroundedAtMs
        if (elapsed >= autoLockDelayMs(context)) {
            _isLocked.value = true
        }
    }

    fun unlock() {
        _isLocked.value = false
        backgroundedAtMs = 0L
    }

    fun lockNow(context: Context) {
        if (isProtectionConfigured(context)) _isLocked.value = true
    }

    fun onSessionChanged(context: Context) {
        // After login/logout the protection set may differ per uid.
        _isLocked.value = isProtectionConfigured(context)
        backgroundedAtMs = 0L
    }
}
