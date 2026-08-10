package com.example.util

import com.example.data.repository.UserPresenceStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class PresenceTimeFormatterTest {

    @Test
    fun testOnlineStatusFormat() {
        val result = PresenceTimeFormatter.formatLastSeen(
            status = UserPresenceStatus.ONLINE,
            lastSeenMs = System.currentTimeMillis()
        )
        assertEquals("En línea", result)
    }

    @Test
    fun testBusyStatusFormat() {
        val result = PresenceTimeFormatter.formatLastSeen(
            status = UserPresenceStatus.BUSY,
            lastSeenMs = System.currentTimeMillis()
        )
        assertEquals("En llamada", result)
    }

    @Test
    fun testActiveJustNowFormat() {
        val now = System.currentTimeMillis()
        val thirtySecondsAgo = now - 30_000L
        val result = PresenceTimeFormatter.formatLastSeen(
            status = UserPresenceStatus.OFFLINE,
            lastSeenMs = thirtySecondsAgo,
            currentTimeMs = now
        )
        assertEquals("Activo ahora", result)
    }

    @Test
    fun testActiveMinutesAgoFormat() {
        val now = System.currentTimeMillis()
        val fifteenMinutesAgo = now - (15 * 60 * 1000L)
        val result = PresenceTimeFormatter.formatLastSeen(
            status = UserPresenceStatus.AWAY,
            lastSeenMs = fifteenMinutesAgo,
            currentTimeMs = now
        )
        assertEquals("Activo hace 15 minutos", result)
    }

    @Test
    fun testSameDayLastSeenFormat() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 5, 14, 30, 0)
        }
        val lastSeenCal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 5, 10, 15, 0)
        }

        val result = PresenceTimeFormatter.formatLastSeen(
            status = UserPresenceStatus.OFFLINE,
            lastSeenMs = lastSeenCal.timeInMillis,
            currentTimeMs = nowCal.timeInMillis
        )
        assert(result.startsWith("Última vez hoy a las"))
    }

    @Test
    fun testYesterdayLastSeenFormat() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 5, 14, 30, 0)
        }
        val lastSeenCal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 4, 18, 45, 0)
        }

        val result = PresenceTimeFormatter.formatLastSeen(
            status = UserPresenceStatus.OFFLINE,
            lastSeenMs = lastSeenCal.timeInMillis,
            currentTimeMs = nowCal.timeInMillis
        )
        assert(result.startsWith("Última vez ayer a las"))
    }
}
