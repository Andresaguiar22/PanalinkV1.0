package com.example.notification.engine.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationDomain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

@Keep
data class NotificationPreferences(
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22, // 10 PM
    val quietHoursEndHour: Int = 7,    // 7 AM
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val mutedDomains: Set<String> = emptySet()
)

@Keep
class NotificationPreferencesRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("notification_v2_prefs", Context.MODE_PRIVATE)

    private val _preferencesFlow = MutableStateFlow(loadPreferences())
    val preferencesFlow: StateFlow<NotificationPreferences> = _preferencesFlow.asStateFlow()

    private fun loadPreferences(): NotificationPreferences {
        return NotificationPreferences(
            quietHoursEnabled = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, false),
            quietHoursStartHour = prefs.getInt(KEY_QUIET_HOURS_START, 22),
            quietHoursEndHour = prefs.getInt(KEY_QUIET_HOURS_END, 7),
            soundEnabled = prefs.getBoolean(KEY_SOUND_ENABLED, true),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true),
            mutedDomains = prefs.getStringSet(KEY_MUTED_DOMAINS, emptySet()) ?: emptySet()
        )
    }

    fun setQuietHours(enabled: Boolean, startHour: Int = 22, endHour: Int = 7) {
        prefs.edit()
            .putBoolean(KEY_QUIET_HOURS_ENABLED, enabled)
            .putInt(KEY_QUIET_HOURS_START, startHour)
            .putInt(KEY_QUIET_HOURS_END, endHour)
            .apply()
        _preferencesFlow.value = loadPreferences()
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
        _preferencesFlow.value = loadPreferences()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
        _preferencesFlow.value = loadPreferences()
    }

    fun muteDomain(domain: NotificationDomain) {
        val current = _preferencesFlow.value.mutedDomains.toMutableSet()
        current.add(domain.name)
        prefs.edit().putStringSet(KEY_MUTED_DOMAINS, current).apply()
        _preferencesFlow.value = loadPreferences()
    }

    fun unmuteDomain(domain: NotificationDomain) {
        val current = _preferencesFlow.value.mutedDomains.toMutableSet()
        current.remove(domain.name)
        prefs.edit().putStringSet(KEY_MUTED_DOMAINS, current).apply()
        _preferencesFlow.value = loadPreferences()
    }

    fun isInQuietHours(): Boolean {
        val prefs = _preferencesFlow.value
        if (!prefs.quietHoursEnabled) return false
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (prefs.quietHoursStartHour > prefs.quietHoursEndHour) {
            // Overnight (e.g. 22:00 to 07:00)
            currentHour >= prefs.quietHoursStartHour || currentHour < prefs.quietHoursEndHour
        } else {
            // Same day (e.g. 13:00 to 15:00)
            currentHour >= prefs.quietHoursStartHour && currentHour < prefs.quietHoursEndHour
        }
    }

    companion object {
        private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
        private const val KEY_QUIET_HOURS_START = "quiet_hours_start"
        private const val KEY_QUIET_HOURS_END = "quiet_hours_end"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        private const val KEY_MUTED_DOMAINS = "muted_domains"
    }
}
