package com.example.rooms.repository

import android.util.Log
import com.example.data.supabase.SupabaseClient
import com.example.rooms.data.VoiceRoomApi
import com.example.rooms.data.VoiceRoomDto
import com.example.rooms.data.VoiceRoomMessageDto
import com.example.rooms.data.VoiceRoomSeatDto
import com.example.rooms.model.VoiceRoom
import com.example.rooms.model.VoiceRoomMessage
import com.example.rooms.model.VoiceRoomSeat
import com.example.util.Resilience
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Repositorio REST de Salas de Voz. Usa la URL/anon key/token de SupabaseClient
 * (solo lectura) pero con cliente e interfaz Retrofit propios.
 */
class VoiceRoomRepository private constructor() {

    companion object {
        private const val TAG = "VoiceRoomRepository"
        private const val LOBBY_NAME = "Sala Principal"
        @Volatile private var instance: VoiceRoomRepository? = null
        fun getInstance(): VoiceRoomRepository =
            instance ?: synchronized(this) {
                instance ?: VoiceRoomRepository().also { instance = it }
            }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: VoiceRoomApi by lazy {
        VoiceRoomApi.create(SupabaseClient.supabaseUrl, SupabaseClient.moshi, client)
    }

    private val apiKey get() = SupabaseClient.supabaseAnonKey
    private val auth get() = "Bearer ${SupabaseClient.currentToken ?: SupabaseClient.supabaseAnonKey}"
    private val myId get() = SupabaseClient.currentUser?.id ?: ""

    /** Devuelve la sala lobby (la crea si no existe). Fase 1: una sola sala. */
    suspend fun getOrCreateLobbyRoom(): Result<VoiceRoom> = runCatching {
        val res = Resilience.retry { api.listLiveRooms(apiKey, auth) }
        if (res.isSuccessful) {
            val existing = res.body()?.firstOrNull()
            if (existing != null) return@runCatching existing.toModel()
        }
        val created = Resilience.retry {
            api.createRoom(apiKey, auth, mapOf("name" to LOBBY_NAME, "owner_id" to myId))
        }
        if (!created.isSuccessful) error("createRoom HTTP ${created.code()}: ${created.errorBody()?.string()}")
        created.body()!!.first().toModel()
    }

    suspend fun joinRoom(roomId: String): Result<Unit> = runCatching {
        // Re-entrar: si quedo una membresia activa previa, la cerramos primero (best effort).
        try { leaveRoom(roomId).getOrNull() } catch (_: Exception) {}
        val res = Resilience.retry {
            api.joinRoom(apiKey, auth, mapOf("room_id" to roomId, "user_id" to myId, "role" to "listener"))
        }
        if (!res.isSuccessful && res.code() != 409) {
            error("joinRoom HTTP ${res.code()}: ${res.errorBody()?.string()}")
        }
        Unit
    }

    suspend fun leaveRoom(roomId: String): Result<Unit> = runCatching {
        try { api.leaveSeat(apiKey, auth, "eq.$roomId", "eq.$myId") } catch (_: Exception) {}
        val res = api.leaveRoom(
            apiKey, auth, "eq.$roomId", "eq.$myId",
            body = mapOf("left_at" to SupabaseClient.getNowIsoString())
        )
        if (!res.isSuccessful) error("leaveRoom HTTP ${res.code()}")
        Unit
    }

    suspend fun takeSeat(roomId: String, seatIndex: Int): Result<VoiceRoomSeatDto> = runCatching {
        val res = api.takeSeat(
            apiKey, auth,
            mapOf("room_id" to roomId, "seat_index" to seatIndex, "user_id" to myId, "is_muted" to false)
        )
        if (!res.isSuccessful) error("takeSeat HTTP ${res.code()}: ${res.errorBody()?.string()}")
        res.body()!!.first()
    }

    suspend fun leaveSeat(roomId: String): Result<Unit> = runCatching {
        val res = api.leaveSeat(apiKey, auth, "eq.$roomId", "eq.$myId")
        if (!res.isSuccessful) error("leaveSeat HTTP ${res.code()}")
        Unit
    }

    suspend fun setSeatMuted(roomId: String, muted: Boolean): Result<Unit> = runCatching {
        val res = api.updateSeat(apiKey, auth, "eq.$roomId", "eq.$myId", mapOf("is_muted" to muted))
        if (!res.isSuccessful) error("updateSeat HTTP ${res.code()}")
        Unit
    }

    suspend fun getSeats(roomId: String): Result<List<VoiceRoomSeatDto>> = runCatching {
        val res = api.getSeats(apiKey, auth, "eq.$roomId")
        if (!res.isSuccessful) error("getSeats HTTP ${res.code()}")
        res.body() ?: emptyList()
    }

    suspend fun getMemberCount(roomId: String): Result<Int> = runCatching {
        val res = api.getMembers(apiKey, auth, "eq.$roomId")
        if (!res.isSuccessful) error("getMembers HTTP ${res.code()}")
        res.body()?.size ?: 0
    }

    suspend fun getMessages(roomId: String): Result<List<VoiceRoomMessageDto>> = runCatching {
        val res = api.getMessages(apiKey, auth, "eq.$roomId")
        if (!res.isSuccessful) error("getMessages HTTP ${res.code()}")
        res.body() ?: emptyList()
    }

    suspend fun sendMessage(roomId: String, content: String): Result<VoiceRoomMessageDto> = runCatching {
        val res = api.sendMessage(
            apiKey, auth,
            mapOf("room_id" to roomId, "sender_id" to myId, "content" to content)
        )
        if (!res.isSuccessful) error("sendMessage HTTP ${res.code()}: ${res.errorBody()?.string()}")
        res.body()!!.first()
    }

    // --- Mappers ---

    private fun VoiceRoomDto.toModel() = VoiceRoom(id, name, ownerId, status, maxSeats)

    fun VoiceRoomMessageDto.toModel(senderName: String? = null) =
        VoiceRoomMessage(id, roomId, senderId, senderName, content, createdAt)
}
