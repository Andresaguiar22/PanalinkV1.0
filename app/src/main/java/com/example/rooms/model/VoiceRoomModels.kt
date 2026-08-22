package com.example.rooms.model

/**
 * Modelos del modulo Salas de Voz. Independientes del chat privado.
 *
 * seat_index: 0 = sillon superior central (anfitrion), 1..6 = sillones inferiores.
 * La UI no guarda estado propio: la unica fuente de verdad es [VoiceRoomUiState].
 */

data class VoiceRoom(
    val id: String,
    val name: String,
    val ownerId: String,
    val status: String,
    val maxSeats: Int = MAX_SEATS
) {
    companion object { const val MAX_SEATS = 7 }
}

data class VoiceRoomMember(
    val id: String,
    val roomId: String,
    val userId: String,
    val role: String,
    val joinedAt: String
)

data class VoiceRoomSeat(
    val index: Int,
    val userId: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val isMuted: Boolean = false,
    val isSpeaking: Boolean = false
) {
    val isOccupied: Boolean get() = userId != null
}

data class VoiceRoomMessage(
    val id: String,
    val roomId: String,
    val senderId: String,
    val senderName: String? = null,
    val content: String,
    val createdAt: String
)

data class VoiceRoomUiState(
    val room: VoiceRoom? = null,
    val seats: List<VoiceRoomSeat> = emptySeats(),
    val messages: List<VoiceRoomMessage> = emptyList(),
    val memberCount: Int = 0,
    val myUserId: String = "",
    val isJoining: Boolean = true,
    val isMicEnabled: Boolean = false,
    val error: String? = null
) {
    val mySeat: VoiceRoomSeat? get() = seats.firstOrNull { it.userId != null && it.userId == myUserId }
    val isSeated: Boolean get() = mySeat != null

    companion object {
        fun emptySeats(): List<VoiceRoomSeat> = (0 until VoiceRoom.MAX_SEATS).map { VoiceRoomSeat(index = it) }
    }
}

/**
 * Reductor puro de sillones: concentra TODAS las transiciones de estado de los
 * 7 puestos para que la UI y los eventos realtime pasen por un solo camino.
 */
object VoiceRoomSeatReducer {

    fun occupy(
        seats: List<VoiceRoomSeat>,
        seatIndex: Int,
        userId: String,
        displayName: String?,
        avatarUrl: String?
    ): List<VoiceRoomSeat> {
        if (seatIndex !in 0 until VoiceRoom.MAX_SEATS) return seats
        if (seats.any { it.userId == userId }) return seats // un usuario, un sillon
        return seats.map {
            if (it.index == seatIndex && it.userId == null) {
                it.copy(userId = userId, displayName = displayName, avatarUrl = avatarUrl, isMuted = false, isSpeaking = false)
            } else it
        }
    }

    fun release(seats: List<VoiceRoomSeat>, userId: String): List<VoiceRoomSeat> =
        seats.map { if (it.userId == userId) VoiceRoomSeat(index = it.index) else it }

    fun releaseSeat(seats: List<VoiceRoomSeat>, seatIndex: Int): List<VoiceRoomSeat> =
        seats.map { if (it.index == seatIndex) VoiceRoomSeat(index = it.index) else it }

    fun setMuted(seats: List<VoiceRoomSeat>, userId: String, muted: Boolean): List<VoiceRoomSeat> =
        seats.map { if (it.userId == userId) it.copy(isMuted = muted, isSpeaking = if (muted) false else it.isSpeaking) else it }

    fun setSpeaking(seats: List<VoiceRoomSeat>, userId: String, speaking: Boolean): List<VoiceRoomSeat> =
        seats.map { if (it.userId == userId && !it.isMuted) it.copy(isSpeaking = speaking) else it }

    fun firstFreeSeatIndex(seats: List<VoiceRoomSeat>): Int? =
        seats.firstOrNull { it.userId == null }?.index
}
