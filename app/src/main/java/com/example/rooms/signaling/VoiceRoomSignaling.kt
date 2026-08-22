package com.example.rooms.signaling

import kotlinx.coroutines.flow.SharedFlow
import org.json.JSONObject

/**
 * Contrato de senalizacion y realtime de Salas de Voz.
 * La UI y el engine WebRTC no saben si esto usa Supabase Realtime, Socket.IO o un SFU:
 * solo ven eventos de dominio y envian SDP/ICE opacos.
 */
interface VoiceRoomSignaling {

    /** Evento de base de datos (postgres_changes) de una tabla de la sala. */
    data class TableEvent(
        val table: String,      // voice_room_seats | voice_room_members | voice_room_messages | voice_rooms
        val eventType: String,  // INSERT | UPDATE | DELETE
        val record: JSONObject
    )

    /** Mensaje de senalizacion WebRTC (offer/answer/ice) dirigido. */
    data class SignalEvent(
        val type: String,       // offer | answer | ice
        val fromUserId: String,
        val toUserId: String,
        val payload: JSONObject
    )

    val tableEvents: SharedFlow<TableEvent>
    val signalEvents: SharedFlow<SignalEvent>
    val connectionState: kotlinx.coroutines.flow.StateFlow<Boolean>

    suspend fun joinRoom(roomId: String)
    suspend fun leaveRoom()

    suspend fun sendOffer(roomId: String, toUserId: String, sdp: String)
    suspend fun sendAnswer(roomId: String, toUserId: String, sdp: String)
    suspend fun sendIceCandidate(roomId: String, toUserId: String, sdpMid: String, sdpMLineIndex: Int, candidate: String)
}
