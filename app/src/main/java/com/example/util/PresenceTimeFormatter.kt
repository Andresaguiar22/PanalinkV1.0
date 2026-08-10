package com.example.util

import com.example.data.repository.UserPresenceStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PresenceTimeFormatter {

    fun formatLastSeen(
        status: UserPresenceStatus,
        lastSeenMs: Long,
        currentTimeMs: Long = System.currentTimeMillis()
    ): String {
        if (status == UserPresenceStatus.ONLINE) {
            return "En línea"
        }
        if (status == UserPresenceStatus.BUSY) {
            return "En llamada"
        }

        val diffMs = (currentTimeMs - lastSeenMs).coerceAtLeast(0)
        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60

        if (diffSec < 60) {
            return "Activo ahora"
        }

        if (diffMin < 60) {
            return "Activo hace $diffMin ${if (diffMin == 1L) "minuto" else "minutos"}"
        }

        val nowCal = Calendar.getInstance().apply { timeInMillis = currentTimeMs }
        val lastSeenCal = Calendar.getInstance().apply { timeInMillis = lastSeenMs }

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(lastSeenMs))

        val isSameDay = nowCal.get(Calendar.YEAR) == lastSeenCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == lastSeenCal.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) {
            return "Última vez hoy a las $formattedTime"
        }

        val yesterdayCal = (nowCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val isYesterday = yesterdayCal.get(Calendar.YEAR) == lastSeenCal.get(Calendar.YEAR) &&
                yesterdayCal.get(Calendar.DAY_OF_YEAR) == lastSeenCal.get(Calendar.DAY_OF_YEAR)

        if (isYesterday) {
            return "Última vez ayer a las $formattedTime"
        }

        val diffDays = (diffMin / (60 * 24)).coerceAtLeast(1)
        return "Última vez hace $diffDays ${if (diffDays == 1L) "día" else "días"}"
    }
}
