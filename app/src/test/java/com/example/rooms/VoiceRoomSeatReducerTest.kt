package com.example.rooms

import com.example.rooms.model.VoiceRoom
import com.example.rooms.model.VoiceRoomSeatReducer
import com.example.rooms.model.VoiceRoomUiState
import org.junit.Assert.*
import org.junit.Test

class VoiceRoomSeatReducerTest {

    private fun empty() = VoiceRoomUiState.emptySeats()

    @Test
    fun `exactly 7 seats are created and all start free`() {
        val seats = empty()
        assertEquals(7, seats.size)
        assertTrue(seats.all { !it.isOccupied })
        assertEquals((0..6).toList(), seats.map { it.index })
    }

    @Test
    fun `occupy fills a free seat`() {
        val seats = VoiceRoomSeatReducer.occupy(empty(), 0, "user_a", "Ana", null)
        assertEquals("user_a", seats[0].userId)
        assertTrue(seats[0].isOccupied)
        assertEquals("Ana", seats[0].displayName)
    }

    @Test
    fun `occupy does not overwrite an occupied seat`() {
        var seats = VoiceRoomSeatReducer.occupy(empty(), 0, "user_a", "Ana", null)
        seats = VoiceRoomSeatReducer.occupy(seats, 0, "user_b", "Beto", null)
        assertEquals("user_a", seats[0].userId)
    }

    @Test
    fun `same user cannot occupy two seats`() {
        var seats = VoiceRoomSeatReducer.occupy(empty(), 0, "user_a", "Ana", null)
        seats = VoiceRoomSeatReducer.occupy(seats, 3, "user_a", "Ana", null)
        assertEquals(1, seats.count { it.userId == "user_a" })
        assertFalse(seats[3].isOccupied)
    }

    @Test
    fun `occupy rejects out of range index`() {
        val seats = VoiceRoomSeatReducer.occupy(empty(), 7, "user_a", null, null)
        assertTrue(seats.none { it.isOccupied })
        val seatsNeg = VoiceRoomSeatReducer.occupy(empty(), -1, "user_a", null, null)
        assertTrue(seatsNeg.none { it.isOccupied })
    }

    @Test
    fun `release frees the user seat`() {
        var seats = VoiceRoomSeatReducer.occupy(empty(), 2, "user_a", null, null)
        seats = VoiceRoomSeatReducer.release(seats, "user_a")
        assertFalse(seats[2].isOccupied)
        assertNull(seats[2].userId)
    }

    @Test
    fun `releaseSeat frees by index`() {
        var seats = VoiceRoomSeatReducer.occupy(empty(), 5, "user_a", null, null)
        seats = VoiceRoomSeatReducer.releaseSeat(seats, 5)
        assertFalse(seats[5].isOccupied)
    }

    @Test
    fun `setMuted marks muted and clears speaking`() {
        var seats = VoiceRoomSeatReducer.occupy(empty(), 1, "user_a", null, null)
        seats = VoiceRoomSeatReducer.setSpeaking(seats, "user_a", true)
        seats = VoiceRoomSeatReducer.setMuted(seats, "user_a", true)
        assertTrue(seats[1].isMuted)
        assertFalse(seats[1].isSpeaking)
    }

    @Test
    fun `setSpeaking is ignored while muted`() {
        var seats = VoiceRoomSeatReducer.occupy(empty(), 1, "user_a", null, null)
        seats = VoiceRoomSeatReducer.setMuted(seats, "user_a", true)
        seats = VoiceRoomSeatReducer.setSpeaking(seats, "user_a", true)
        assertFalse(seats[1].isSpeaking)
    }

    @Test
    fun `firstFreeSeatIndex returns lowest free seat and null when full`() {
        var seats = empty()
        assertEquals(0, VoiceRoomSeatReducer.firstFreeSeatIndex(seats))
        seats = VoiceRoomSeatReducer.occupy(seats, 0, "u0", null, null)
        assertEquals(1, VoiceRoomSeatReducer.firstFreeSeatIndex(seats))
        // Llenar los 7 sillones
        (1..6).forEach { seats = VoiceRoomSeatReducer.occupy(seats, it, "u$it", null, null) }
        assertNull(VoiceRoomSeatReducer.firstFreeSeatIndex(seats))
        assertEquals(7, seats.count { it.isOccupied })
    }

    @Test
    fun `full room keeps MAX_SEATS at 7`() {
        assertEquals(7, VoiceRoom.MAX_SEATS)
    }
}
