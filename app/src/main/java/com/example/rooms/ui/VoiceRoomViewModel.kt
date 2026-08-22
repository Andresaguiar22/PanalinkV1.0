package com.example.rooms.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.supabase.SupabaseClient
import com.example.rooms.model.VoiceRoomMessage
import com.example.rooms.model.VoiceRoomSeatReducer
import com.example.rooms.model.VoiceRoomUiState
import com.example.rooms.repository.VoiceRoomRepository
import com.example.rooms.signaling.SupabaseVoiceRoomSignaling
import com.example.rooms.signaling.VoiceRoomSignaling
import com.example.rooms.webrtc.VoiceRoomAudioManager
import com.example.rooms.webrtc.VoiceRoomWebRtcEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate

/**
 * ViewModel de la Sala de Voz. Unica fuente de verdad: [uiState].
 * Orquesta REST (repository), Realtime (signaling) y audio (engine).
 */
class VoiceRoomViewModel(app: Application) : AndroidViewModel(app) {

    companion object { private const val TAG = "VoiceRoomVM" }

    private val repository = VoiceRoomRepository.getInstance()
    private val myId: String get() = SupabaseClient.currentUser?.id ?: ""

    private val _uiState = MutableStateFlow(VoiceRoomUiState(myUserId = myId))
    val uiState: StateFlow<VoiceRoomUiState> = _uiState

    private var signaling: VoiceRoomSignaling? = null
    private var rtcEngine: VoiceRoomWebRtcEngine? = null
    private var audioManager: VoiceRoomAudioManager? = null
    private var roomId: String? = null
    private var hasAudioPermission = false

    // ------------------------------------------------------------------
    // Ciclo de vida de la sala
    // ------------------------------------------------------------------

    fun enterRoom() {
        if (roomId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true, error = null) }
            repository.getOrCreateLobbyRoom()
                .onSuccess { room ->
                    roomId = room.id
                    repository.joinRoom(room.id)
                        .onFailure { e -> Log.e(TAG, "joinRoom fallo", e) }
                    _uiState.update { it.copy(room = room) }
                    startSignaling(room.id)
                    refreshSnapshot(room.id)
                    _uiState.update { it.copy(isJoining = false) }
                }
                .onFailure { e ->
                    Log.e(TAG, "No se pudo entrar a la sala", e)
                    _uiState.update { it.copy(isJoining = false, error = "No se pudo entrar a la sala") }
                }
        }
    }

    fun leaveRoom() {
        val id = roomId ?: return
        viewModelScope.launch {
            repository.leaveRoom(id)
        }
        teardown()
    }

    private fun teardown() {
        viewModelScope.launch { signaling?.leaveRoom() }
        rtcEngine?.release()
        rtcEngine = null
        audioManager?.exitRoomAudio()
        audioManager = null
        signaling = null
        roomId = null
        _uiState.value = VoiceRoomUiState(myUserId = myId)
    }

    override fun onCleared() {
        leaveRoom()
    }

    // ------------------------------------------------------------------
    // Sillones
    // ------------------------------------------------------------------

    fun onSeatClicked(seatIndex: Int, hasRecordPermission: Boolean) {
        val state = _uiState.value
        val id = roomId ?: return
        hasAudioPermission = hasRecordPermission

        val mySeat = state.mySeat
        if (mySeat != null) {
            // Estoy sentado: tocar mi sillon libera; tocar otro no hace nada.
            if (mySeat.index == seatIndex) {
                viewModelScope.launch {
                    repository.leaveSeat(id).onSuccess {
                        _uiState.update { s ->
                            s.copy(
                                seats = VoiceRoomSeatReducer.releaseSeat(s.seats, seatIndex),
                                isMicEnabled = false
                            )
                        }
                        rtcEngine?.setMicEnabled(false)
                        stopAudioIfAlone()
                    }
                }
            }
            return
        }

        if (state.seats.getOrNull(seatIndex)?.isOccupied == true) return

        viewModelScope.launch {
            repository.takeSeat(id, seatIndex)
                .onSuccess {
                    val profile = SupabaseClient.currentProfile
                    _uiState.update { s ->
                        s.copy(
                            seats = VoiceRoomSeatReducer.occupy(
                                s.seats, seatIndex, myId,
                                profile?.displayName, profile?.avatarUrl
                            ),
                            isMicEnabled = hasAudioPermission
                        )
                    }
                    startAudio()
                    // Aviso al engine de todos los demas ya sentados (yo ofrezco si mi id es menor).
                    state.seats.filter { it.isOccupied && it.userId != myId }.forEach {
                        rtcEngine?.onPeerJoined(it.userId!!)
                    }
                }
                .onFailure { e ->
                    Log.e(TAG, "takeSeat fallo", e)
                    _uiState.update { it.copy(error = "No se pudo ocupar el sillón") }
                }
        }
    }

    fun toggleMute() {
        val state = _uiState.value
        val seat = state.mySeat ?: return
        val id = roomId ?: return
        val newMuted = !seat.isMuted
        viewModelScope.launch {
            repository.setSeatMuted(id, newMuted)
        }
        _uiState.update { s -> s.copy(seats = VoiceRoomSeatReducer.setMuted(s.seats, myId, newMuted)) }
        rtcEngine?.setMicEnabled(!newMuted && hasAudioPermission)
    }

    // ------------------------------------------------------------------
    // Chat de la sala
    // ------------------------------------------------------------------

    fun sendMessage(text: String) {
        val id = roomId ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.sendMessage(id, trimmed)
                .onFailure { e ->
                    Log.e(TAG, "sendMessage fallo", e)
                    _uiState.update { it.copy(error = "No se pudo enviar el mensaje") }
                }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun startAudio() {
        if (rtcEngine == null) {
            val engine = VoiceRoomWebRtcEngine(getApplication(), myId, object : VoiceRoomWebRtcEngine.Listener {
                override fun onLocalIceCandidate(toUserId: String, candidate: IceCandidate) {
                    viewModelScope.launch {
                        signaling?.sendIceCandidate(
                            roomId ?: return@launch, toUserId,
                            candidate.sdpMid ?: "", candidate.sdpMLineIndex, candidate.sdp
                        )
                    }
                }
                override fun onPeerSpeaking(userId: String, speaking: Boolean) {
                    _uiState.update { s -> s.copy(seats = VoiceRoomSeatReducer.setSpeaking(s.seats, userId, speaking)) }
                }
                override fun onPeerConnectionStateChanged(userId: String, connected: Boolean) {
                    Log.d(TAG, "Peer $userId connected=$connected")
                }
                override suspend fun sendOfferTo(userId: String, sdp: String) {
                    signaling?.sendOffer(roomId ?: return, userId, sdp)
                }
                override suspend fun sendAnswerTo(userId: String, sdp: String) {
                    signaling?.sendAnswer(roomId ?: return, userId, sdp)
                }
            })
            engine.initialize()
            rtcEngine = engine
        }
        if (audioManager == null) {
            audioManager = VoiceRoomAudioManager(getApplication()).also { it.enterRoomAudio() }
        }
        rtcEngine?.setMicEnabled(hasAudioPermission)
    }

    private fun stopAudioIfAlone() {
        val others = _uiState.value.seats.count { it.isOccupied }
        if (others == 0) {
            rtcEngine?.release()
            rtcEngine = null
            audioManager?.exitRoomAudio()
            audioManager = null
        }
    }

    private fun startSignaling(roomId: String) {
        val sig = SupabaseVoiceRoomSignaling(myId)
        signaling = sig
        viewModelScope.launch { sig.joinRoom(roomId) }

        viewModelScope.launch {
            sig.tableEvents.collect { ev -> onTableEvent(ev) }
        }
        viewModelScope.launch {
            sig.signalEvents.collect { ev -> onSignalEvent(ev) }
        }
    }

    private suspend fun refreshSnapshot(roomId: String) {
        repository.getSeats(roomId).onSuccess { dtos ->
            var seats = VoiceRoomUiState.emptySeats()
            dtos.forEach { dto ->
                seats = VoiceRoomSeatReducer.occupy(seats, dto.seatIndex, dto.userId, null, null)
                if (dto.isMuted) seats = VoiceRoomSeatReducer.setMuted(seats, dto.userId, true)
            }
            _uiState.update { it.copy(seats = seats) }
            // Conectar con quienes ya estan sentados
            dtos.filter { it.userId != myId }.forEach { rtcEngine?.onPeerJoined(it.userId) }
        }
        repository.getMemberCount(roomId).onSuccess { count ->
            _uiState.update { it.copy(memberCount = count) }
        }
        repository.getMessages(roomId).onSuccess { dtos ->
            _uiState.update { s ->
                s.copy(messages = dtos.map { d -> VoiceRoomMessage(d.id, d.roomId, d.senderId, null, d.content, d.createdAt) })
            }
        }
    }

    private fun onTableEvent(ev: VoiceRoomSignaling.TableEvent) {
        when (ev.table) {
            "voice_room_seats" -> {
                when (ev.eventType) {
                    "INSERT" -> {
                        val seatIndex = ev.record.optInt("seat_index", -1)
                        val userId = ev.record.optString("user_id")
                        if (seatIndex >= 0 && userId.isNotEmpty()) {
                            _uiState.update { s ->
                                if (s.seats.any { it.userId == userId }) s
                                else s.copy(seats = VoiceRoomSeatReducer.occupy(s.seats, seatIndex, userId, null, null))
                            }
                            if (userId != myId) rtcEngine?.onPeerJoined(userId)
                        }
                    }
                    "UPDATE" -> {
                        val userId = ev.record.optString("user_id")
                        val muted = ev.record.optBoolean("is_muted", false)
                        _uiState.update { s -> s.copy(seats = VoiceRoomSeatReducer.setMuted(s.seats, userId, muted)) }
                    }
                    "DELETE" -> {
                        val userId = ev.record.optString("user_id")
                        _uiState.update { s -> s.copy(seats = VoiceRoomSeatReducer.release(s.seats, userId)) }
                        rtcEngine?.onPeerLeft(userId)
                    }
                }
            }
            "voice_room_members" -> {
                val id = roomId ?: return
                viewModelScope.launch {
                    repository.getMemberCount(id).onSuccess { count ->
                        _uiState.update { it.copy(memberCount = count) }
                    }
                }
            }
            "voice_room_messages" -> {
                if (ev.eventType != "INSERT") return
                val msg = VoiceRoomMessage(
                    id = ev.record.optString("id"),
                    roomId = ev.record.optString("room_id"),
                    senderId = ev.record.optString("sender_id"),
                    senderName = null,
                    content = ev.record.optString("content"),
                    createdAt = ev.record.optString("created_at")
                )
                if (msg.id.isNotEmpty()) {
                    _uiState.update { s ->
                        if (s.messages.any { it.id == msg.id }) s
                        else s.copy(messages = (s.messages + msg).takeLast(200))
                    }
                }
            }
        }
    }

    private fun onSignalEvent(ev: VoiceRoomSignaling.SignalEvent) {
        val engine = rtcEngine ?: return
        viewModelScope.launch {
            when (ev.type) {
                "offer" -> engine.onRemoteOffer(ev.fromUserId, ev.payload.optString("sdp"))
                "answer" -> engine.onRemoteAnswer(ev.fromUserId, ev.payload.optString("sdp"))
                "ice" -> engine.onRemoteIceCandidate(
                    ev.fromUserId,
                    ev.payload.optString("sdpMid"),
                    ev.payload.optInt("sdpMLineIndex"),
                    ev.payload.optString("candidate")
                )
            }
        }
    }
}
