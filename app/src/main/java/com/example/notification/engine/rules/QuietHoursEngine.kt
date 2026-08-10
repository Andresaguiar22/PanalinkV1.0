package com.example.notification.engine.rules

import androidx.annotation.Keep
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2
import java.util.Calendar

@Keep
class QuietHoursEngine(
    val isEnabled: Boolean = true,
    val startHour: Int = 22,
    val startMinute: Int = 0,
    val endHour: Int = 7,
    val endMinute: Int = 0
) {

    fun isInQuietHours(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!isEnabled) return false

        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinute = cal.get(Calendar.MINUTE)
        val currentTotalMinutes = currentHour * 60 + currentMinute

        val startTotalMinutes = startHour * 60 + startMinute
        val endTotalMinutes = endHour * 60 + endMinute

        return if (startTotalMinutes > endTotalMinutes) {
            // Overnight quiet hours (e.g. 22:00 to 07:00)
            currentTotalMinutes >= startTotalMinutes || currentTotalMinutes < endTotalMinutes
        } else {
            // Same day quiet hours (e.g. 13:00 to 15:00)
            currentTotalMinutes in startTotalMinutes until endTotalMinutes
        }
    }

    fun applyQuietHoursPolicy(
        event: NotificationEvent,
        currentPriority: NotificationPriority,
        currentInterruptiveness: InterruptivenessLevel,
        nowMillis: Long = System.currentTimeMillis()
    ): Pair<NotificationPriority, InterruptivenessLevel> {
        if (!isInQuietHours(nowMillis)) {
            return Pair(currentPriority, currentInterruptiveness)
        }

        // CRITICAL priority / Incoming calls bypass Quiet Hours
        if (currentPriority == NotificationPriority.CRITICAL || event.type == NotificationTypeV2.CALL_INCOMING) {
            return Pair(currentPriority, currentInterruptiveness)
        }

        // Downscale disturbance during Quiet Hours
        val adjustedInterruptiveness = when (currentInterruptiveness) {
            InterruptivenessLevel.FULLSCREEN, InterruptivenessLevel.HEADS_UP, InterruptivenessLevel.SOUND_ONLY -> {
                InterruptivenessLevel.STATUS_BAR_ONLY
            }
            else -> InterruptivenessLevel.IN_APP_ONLY
        }

        val adjustedPriority = if (currentPriority == NotificationPriority.HIGH) NotificationPriority.NORMAL else currentPriority

        return Pair(adjustedPriority, adjustedInterruptiveness)
    }
}
