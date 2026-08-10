package com.example

import com.example.data.repository.CallAvailability
import com.example.data.repository.PresenceRepository
import com.example.data.repository.UserPresenceStatus
import com.example.util.PresenceHistoryTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = PanaApplication::class)
class PresenceRepositoryTest {

    @Test
    fun testPresenceRepositoryDefaultStatus() {
        val status = PresenceRepository.currentUserStatus.value
        assertEquals(UserPresenceStatus.ONLINE, status)
    }

    @Test
    fun testUpdateMyStatus() {
        PresenceRepository.updateMyStatus(UserPresenceStatus.BUSY)
        assertEquals(UserPresenceStatus.BUSY, PresenceRepository.currentUserStatus.value)

        PresenceRepository.updateMyStatus(UserPresenceStatus.AWAY)
        assertEquals(UserPresenceStatus.AWAY, PresenceRepository.currentUserStatus.value)

        PresenceRepository.updateMyStatus(UserPresenceStatus.ONLINE)
        assertEquals(UserPresenceStatus.ONLINE, PresenceRepository.currentUserStatus.value)
    }

    @Test
    fun testCallAvailabilityUpdates() {
        PresenceRepository.updateMyCallAvailability(CallAvailability.MESSAGES_ONLY)
        assertEquals(CallAvailability.MESSAGES_ONLY, PresenceRepository.currentUserCallAvailability.value)

        PresenceRepository.updateMyCallAvailability(CallAvailability.AVAILABLE)
        assertEquals(CallAvailability.AVAILABLE, PresenceRepository.currentUserCallAvailability.value)
    }

    @Test
    fun testIsUserAvailableForCallDefault() {
        val unknownUserId = "user_test_999"
        val availability = PresenceRepository.isUserAvailableForCall(unknownUserId)
        assertFalse(availability.first)
        assertEquals("El usuario está desconectado", availability.second)
    }

    @Test
    fun testPresenceHistoryTracker() {
        val testUser = "user_history_123"
        PresenceHistoryTracker.recordEvent(testUser, UserPresenceStatus.ONLINE)
        PresenceHistoryTracker.recordEvent(testUser, UserPresenceStatus.AWAY)

        val history = PresenceHistoryTracker.getHistoryForUser(testUser)
        assertTrue(history.size >= 2)
        assertEquals(UserPresenceStatus.ONLINE, history[0].status)
        assertEquals(UserPresenceStatus.AWAY, history[1].status)
    }
}
