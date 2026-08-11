package com.example.call

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.net.URISyntaxException

/**
 * SignalingClient implements signaling using Socket.IO, transmitting offers, answers,
 * and ICE candidates between callers. It runs completely offline/peer-to-peer over the network
 * for the media stream.
 */
class SignalingClient(private val userId: String) {
    companion object {
        private const val TAG = "SignalingClient"
    }

    private var socket: Socket? = null

    // Shared Flows to communicate socket events reactively
    private val _incomingCallFlow = MutableSharedFlow<IncomingCallData>(extraBufferCapacity = 64)
    val incomingCallFlow: SharedFlow<IncomingCallData> = _incomingCallFlow

    private val _callAnswerFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
    val callAnswerFlow: SharedFlow<JSONObject> = _callAnswerFlow

    private val _callRejectedFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
    val callRejectedFlow: SharedFlow<JSONObject> = _callRejectedFlow

    private val _callEndedFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
    val callEndedFlow: SharedFlow<JSONObject> = _callEndedFlow

    private val _callBusyFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
    val callBusyFlow: SharedFlow<JSONObject> = _callBusyFlow

    private val _iceCandidateReceivedFlow = MutableSharedFlow<JSONObject>(extraBufferCapacity = 64)
    val iceCandidateReceivedFlow: SharedFlow<JSONObject> = _iceCandidateReceivedFlow

    private val _connectionStatusFlow = MutableStateFlow<Boolean>(false)
    val connectionStatusFlow: StateFlow<Boolean> = _connectionStatusFlow

    init {
        connect()
    }

    fun connect() {
        if (socket?.connected() == true) return

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val backendUrl = com.example.data.repository.CdnManager.getCDNUrl()
                if (backendUrl.isNotEmpty()) {
                    Log.d(TAG, "Initializing Socket.IO with dynamic URL: $backendUrl")
                    val options = IO.Options().apply {
                        reconnection = true
                        reconnectionDelay = 1000
                        reconnectionDelayMax = 5000
                        reconnectionAttempts = Int.MAX_VALUE
                        transports = arrayOf("websocket")
                    }
                    if (socket == null) {
                        socket = IO.socket(backendUrl, options)
                        setupListeners()
                    }
                    socket?.connect()
                } else {
                    Log.e(TAG, "Dynamic backend URL obtained from Supabase is empty! Signaling will not work.")
                    _connectionStatusFlow.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch dynamic backend URL and initialize Socket.IO", e)
                _connectionStatusFlow.value = false
            }
        }
    }

    private fun setupListeners() {
        socket?.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Socket connected, registering user: $userId")
            val registerData = JSONObject().apply {
                put("userId", userId)
            }
            socket?.emit("register_user", registerData)
            _connectionStatusFlow.value = true
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Socket disconnected")
            _connectionStatusFlow.value = false
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "Socket connect error: ${args.getOrNull(0)}")
            _connectionStatusFlow.value = false
        }

        socket?.on("incoming_call") { args ->
            Log.d(TAG, "Received incoming_call")
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            val callerId = data.optString("callerId").ifEmpty { data.optString("senderId") }
            val callerName = data.optString("callerName")
            val callType = data.optString("callType", "voice")
            val sdp = data.optString("sdp")
            _incomingCallFlow.tryEmit(IncomingCallData(callerId, callerName, callType, sdp))
        }

        socket?.on("webrtc_offer") { args ->
            Log.d(TAG, "Received webrtc_offer")
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            val callerId = data.optString("callerId").ifEmpty { data.optString("senderId") }
            val callerName = data.optString("callerName", "")
            val callType = data.optString("callType", "voice")
            val sdp = data.optString("sdp")
            _incomingCallFlow.tryEmit(IncomingCallData(callerId, callerName, callType, sdp))
        }

        socket?.on("webrtc_answer") { args ->
            Log.d(TAG, "Received webrtc_answer")
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            _callAnswerFlow.tryEmit(data)
        }

        socket?.on("call_answer") { args ->
            Log.d(TAG, "Received call_answer")
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            _callAnswerFlow.tryEmit(data)
        }

        socket?.on("call_accepted") { args ->
            Log.d(TAG, "Received call_accepted")
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            _callAnswerFlow.tryEmit(data)
        }

        socket?.on("call_rejected") { args ->
            Log.d(TAG, "Received call_rejected")
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            _callRejectedFlow.tryEmit(data)
        }

        socket?.on("call_reject") { args ->
            Log.d(TAG, "Received call_reject")
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            _callRejectedFlow.tryEmit(data)
        }

        socket?.on("call_ended") { args ->
            Log.d(TAG, "Received call_ended")
            val data = args.getOrNull(0) as? JSONObject ?: JSONObject()
            _callEndedFlow.tryEmit(data)
        }

        socket?.on("call_end") { args ->
            Log.d(TAG, "Received call_end")
            val data = args.getOrNull(0) as? JSONObject ?: JSONObject()
            _callEndedFlow.tryEmit(data)
        }

        socket?.on("call_busy") { args ->
            Log.d(TAG, "Received call_busy")
            val data = args.getOrNull(0) as? JSONObject ?: JSONObject()
            _callBusyFlow.tryEmit(data)
        }

        socket?.on("ice_candidate") { args ->
            Log.d(TAG, "Received ice_candidate")
            val data = args.getOrNull(0) as? JSONObject ?: return@on
            _iceCandidateReceivedFlow.tryEmit(data)
        }
    }

    fun sendCallRequest(targetUserId: String, callerName: String, callType: String) {
        val payload = JSONObject().apply {
            put("targetUserId", targetUserId)
            put("senderId", userId)
            put("callerId", userId)
            put("callerName", callerName)
            put("callType", callType)
        }
        Log.d(TAG, "Emitting call_request payload (metadata without SDP)")
        socket?.emit("call_request", payload)
    }

    fun sendOffer(targetUserId: String, offerSdp: String, callerName: String, callType: String) {
        val payload = JSONObject().apply {
            put("targetUserId", targetUserId)
            put("senderId", userId)
            put("sdp", offerSdp)
            put("callerId", userId)
            put("callerName", callerName)
            put("callType", callType)
        }
        Log.d(TAG, "Emitting webrtc_offer payload with SDP")
        socket?.emit("webrtc_offer", payload)
    }

    fun sendAnswer(targetUserId: String, answerSdp: String) {
        val payload = JSONObject().apply {
            put("targetUserId", targetUserId)
            put("senderId", userId)
            put("callerId", userId)
            put("sdp", answerSdp)
        }
        Log.d(TAG, "Emitting webrtc_answer")
        socket?.emit("webrtc_answer", payload)
    }

    fun acceptCall(callerId: String) {
        val payload = JSONObject().apply {
            put("targetUserId", callerId)
            put("callerId", callerId)
            put("senderId", userId)
        }
        Log.d(TAG, "Emitting call_accept payload")
        socket?.emit("call_accept", payload)
    }

    fun sendIceCandidate(targetUserId: String, sdpMid: String, sdpMLineIndex: Int, candidate: String) {
        val payload = JSONObject().apply {
            put("targetUserId", targetUserId)
            put("senderId", userId)
            put("sdpMid", sdpMid)
            put("sdpMLineIndex", sdpMLineIndex)
            put("candidate", candidate)
        }
        Log.d(TAG, "Emitting ice_candidate")
        socket?.emit("ice_candidate", payload)
    }

    fun rejectCall(callerId: String) {
        val payload = JSONObject().apply {
            put("targetUserId", callerId)
            put("senderId", userId)
        }
        Log.d(TAG, "Emitting call_reject payload")
        socket?.emit("call_reject", payload)
    }

    fun endCall(targetUserId: String) {
        val payload = JSONObject().apply {
            put("targetUserId", targetUserId)
            put("senderId", userId)
        }
        Log.d(TAG, "Emitting call_end payload")
        socket?.emit("call_end", payload)
    }

    fun sendBusy(targetUserId: String) {
        val payload = JSONObject().apply {
            put("targetUserId", targetUserId)
            put("senderId", userId)
        }
        Log.d(TAG, "Emitting call_busy payload")
        socket?.emit("call_busy", payload)
    }

    fun disconnect() {
        Log.d(TAG, "Disconnecting signaling socket")
        socket?.disconnect()
    }
}

data class IncomingCallData(
    val callerId: String,
    val callerName: String,
    val callType: String,
    val sdp: String?
)
