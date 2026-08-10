package com.example.call

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import org.webrtc.*
import java.util.concurrent.TimeUnit

/**
 * CallManager is the master coordinator for all signaling, peer connection management,
 * call states, audio devices (speaker, earpiece, mic), and durations.
 */
class CallManager private constructor(private val context: Context) : WebRTCClient.WebRTCListener {

    companion object {
        private const val TAG = "CallManager"
        @Volatile
        private var instance: CallManager? = null

        private const val INITIAL_CONNECTION_TIMEOUT = 20000L
        private const val ICE_RESTART_TIMEOUT = 30000L

        fun getInstance(context: Context): CallManager {
            return instance ?: synchronized(this) {
                instance ?: CallManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val mainScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var signalingClient: SignalingClient? = null
    private var webRtcClient: WebRTCClient? = null
    private val audioController = AudioController(context)

    // State flows representing call details
    private val _callState = MutableStateFlow<CallState>(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState

    private val _callType = MutableStateFlow(CallType.AUDIO)
    val callType: StateFlow<CallType> = _callType

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration


    private val _opponentName = MutableStateFlow<String?>(null)
    private val _opponentId = MutableStateFlow<String?>(null)
    val opponentId: StateFlow<String?> = _opponentId
    val opponentName: StateFlow<String?> = _opponentName

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn

    private val _isCameraOn = MutableStateFlow(true)
    val isCameraOn: StateFlow<Boolean> = _isCameraOn

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    // Local & Remote view renderers (referenced from Compose views)
    private var localVideoView: SurfaceViewRenderer? = null
    private var remoteVideoView: SurfaceViewRenderer? = null
    private var remoteVideoTrack: VideoTrack? = null

    // EglBase for video context sharing
    val eglBaseContext: EglBase.Context by lazy { EglBase.create().eglBaseContext }

    private var durationJob: Job? = null
    private var currentUserId: String? = null
    private var incomingSdpOffer: String? = null

    // WebRTC connection state hardening
    private var isInitiator = false
    private var remoteDescriptionSet = false
    private val pendingIceCandidates = mutableListOf<IceCandidate>()

    // Connection Guard Timer
    private var connectionGuardJob: Job? = null
    private var isCleaningCall = false

    /**
     * Initializes signaling for the logged-in user.
     */
    fun initialize(userId: String) {
        if (currentUserId == userId && signalingClient != null) {
            // If already initialized but signaling is down, try to reconnect
            if (!_isConnected.value) {
                Log.d(TAG, "Already initialized for $userId but disconnected. Reconnecting signaling client.")
                signalingClient?.connect()
            }
            return
        }
        currentUserId = userId
        
        Log.d(TAG, "Initializing Signaling Client for $userId")
        signalingClient?.disconnect()
        signalingClient = SignalingClient(userId)
        observeSignalingEvents()
    }

    /**
     * Force a full reconnection of the signaling engine.
     */
    fun forceReconnect() {
        val userId = currentUserId ?: return
        Log.i(TAG, "Forcing Signaling Reconnection for $userId")
        signalingClient?.disconnect()
        signalingClient = null
        initialize(userId)
    }

    fun handleFCMIncomingCall(callerId: String, callerName: String, typeStr: String, sdp: String? = null) {
        if (_callState.value == CallState.IDLE) {
            try {
                Log.d(TAG, "Handling incoming call from FCM directly: $callerId")
                _opponentId.value = callerId
                _opponentName.value = callerName
                _opponentId.value = callerId
                val isVideo = typeStr == "video"
                updateCallState(CallState.RINGING)
                
                if (!sdp.isNullOrEmpty()) {
                    incomingSdpOffer = sdp
                }

                audioController.setMode(AudioManager.MODE_RINGTONE)

                // Start foreground service for incoming call with FullScreenIntent
                CallForegroundService.startIncomingCall(context, callerName, isVideo)
            } catch (e: Exception) {
                Log.e(TAG, "Critical error in handleFCMIncomingCall", e)
                resetCall()
            }
        }
    }

    private fun observeSignalingEvents() {
        val client = signalingClient ?: return
        
        mainScope.launch {
            client.connectionStatusFlow.collect { connected ->
                _isConnected.value = connected
                Log.d(TAG, "Signaling connection changed: $connected")
            }
        }

        mainScope.launch {
            client.incomingCallFlow.collect { incoming ->
                Log.d(TAG, "Processing incoming call from: ${incoming.callerId}")
                if (_callState.value != CallState.IDLE) {
                    if ((_callState.value == CallState.RINGING || _callState.value is CallState.CONNECTING || _callState.value == CallState.CONNECTED || _callState.value == CallState.RECONNECTING) && _opponentId.value == incoming.callerId) {
                        Log.d(TAG, "Already ringing/connecting/connected for the same caller, updating incoming SDP offer")
                        incomingSdpOffer = incoming.sdp
                        if ((_callState.value is CallState.CONNECTING || _callState.value == CallState.CONNECTED || _callState.value == CallState.RECONNECTING) && !incoming.sdp.isNullOrEmpty()) {
                            Log.d(TAG, "Recipient in active/connecting/reconnecting call, applying newly received remote offer SDP")
                            remoteDescriptionSet = false
                            webRtcClient?.setRemoteDescription(
                                SessionDescription(SessionDescription.Type.OFFER, incoming.sdp),
                                object : SdpObserver {
                                    override fun onCreateSuccess(p0: SessionDescription?) {}
                                    override fun onSetSuccess() {
                                        Log.d(TAG, "Remote description set, creating WebRTC Answer")
                                        mainScope.launch {
                                            remoteDescriptionSet = true
                                            flushPendingIceCandidates()
                                        }
                                        webRtcClient?.createAnswer(object : SdpObserver {
                                            override fun onCreateSuccess(answerSdp: SessionDescription?) {
                                                answerSdp?.let {
                                                    signalingClient?.sendAnswer(incoming.callerId, it.description)
                                                }
                                            }
                                            override fun onSetSuccess() {}
                                            override fun onCreateFailure(p0: String?) {}
                                            override fun onSetFailure(p0: String?) {}
                                        })
                                    }
                                    override fun onCreateFailure(p0: String?) {}
                                    override fun onSetFailure(p0: String?) {}
                                }
                            )
                        }
                        return@collect
                    }
                    Log.d(TAG, "User busy, automatically rejecting call with call_busy")
                    client.sendBusy(incoming.callerId)
                    return@collect
                }
                
                try {
                    _opponentId.value = incoming.callerId
                    _opponentName.value = incoming.callerName
                    _opponentId.value = incoming.callerId
                    val isVideo = incoming.callType == "video"
                    updateCallState(CallState.RINGING)
                    
                    incomingSdpOffer = incoming.sdp
                    
                    // Initialize audio manager for ringtone path
                    audioController.setMode(AudioManager.MODE_RINGTONE)

                    // Start foreground service for incoming call
                    CallForegroundService.startIncomingCall(context, incoming.callerName, isVideo)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing incoming call flow", e)
                    resetCall()
                }
            }
        }

        mainScope.launch {
            client.callAnswerFlow.collect { data ->
                Log.d(TAG, "WebRTC Answer received from opponent")
                updateCallState(CallState.CONNECTING)
                outgoingCallJob?.cancel()
                outgoingCallJob = null
                val sdpStr = data.optString("sdp")
                webRtcClient?.setRemoteDescription(
                    SessionDescription(SessionDescription.Type.ANSWER, sdpStr),
                    object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "Remote description set successfully on initiator side")
                            mainScope.launch {
                                remoteDescriptionSet = true
                                flushPendingIceCandidates()
                            }
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(err: String?) {
                            Log.e(TAG, "Answer SDP setRemoteDescription failed: $err")
                        }
                    }
                )
            }
        }

        mainScope.launch {
            client.callRejectedFlow.collect { data ->
                Log.d(TAG, "Call rejected by peer")
                updateCallState(CallState.BUSY)
                delay(1500)
                resetCall()
            }
        }

        mainScope.launch {
            client.callEndedFlow.collect { data ->
                Log.d(TAG, "Call ended by peer")
                updateCallState(CallState.ENDED)
                delay(1000)
                resetCall()
            }
        }

        mainScope.launch {
            client.callBusyFlow.collect { data ->
                Log.d(TAG, "Peer is busy")
                updateCallState(CallState.BUSY)
                delay(1500)
                resetCall()
            }
        }

        mainScope.launch {
            client.iceCandidateReceivedFlow.collect { data ->
                val sdpMid = data.optString("sdpMid")
                val sdpMLineIndex = data.optInt("sdpMLineIndex")
                val sdp = data.optString("candidate")
                val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                if (remoteDescriptionSet) {
                    webRtcClient?.addIceCandidate(candidate)
                } else {
                    Log.d(TAG, "Queueing ICE candidate prior to remote description: $sdp")
                    pendingIceCandidates.add(candidate)
                }
            }
        }
    }

    private var outgoingCallJob: Job? = null

    /**
     * Start an outgoing call.
     */
    fun startCall(targetUserId: String, targetUserName: String, type: CallType) {
        if (_callState.value != CallState.IDLE) return

        val availability = com.example.data.repository.PresenceRepository.isUserAvailableForCall(targetUserId)
        if (!availability.first) {
            val failureState = if (availability.second.contains("llamada", ignoreCase = true)) CallState.BUSY else CallState.FAILED
            updateCallState(failureState)
            mainScope.launch {
                delay(3000)
                resetCall()
            }
            return
        }

        com.example.data.repository.PresenceRepository.updateMyStatus(com.example.data.repository.UserPresenceStatus.BUSY)

        _opponentId.value = targetUserId
        _opponentName.value = targetUserName
        _opponentId.value = targetUserId
        _callType.value = type
        
        outgoingCallJob?.cancel()
        outgoingCallJob = mainScope.launch {
            delay(30000)
            if (_callState.value is CallState.OUTGOING) {
                updateCallState(CallState.CANCELLED)
                endCall()
                delay(2000)
                resetCall()
            }
        }
        
        audioController.startCall()
        
        // Start foreground service for outgoing call
        CallForegroundService.startService(context, targetUserName, type == CallType.VIDEO)

        // Send metadata call_request first so recipient starts ringing/receiving immediately
        val callerName = SupabaseClient.currentProfile?.displayName 
            ?: SupabaseClient.currentUser?.email 
            ?: "Panalink User"
        signalingClient?.sendCallRequest(
            targetUserId = targetUserId,
            callerName = callerName,
            callType = if (type == CallType.VIDEO) "video" else "voice"
        )

        // Start WebRTC session as initiator right away to generate WebRTC offer
        startWebRTCSession(isInitiator = true)
    }

    /**
     * Accept incoming call.
     */
    fun acceptCall() {
        if (_callState.value !is CallState.RINGING) return
        updateCallState(CallState.CONNECTING)
        
        val target = _opponentId.value ?: ""
        signalingClient?.acceptCall(target)
        
        audioController.startCall()
        
        // Start WebRTC session as non-initiator to apply remote offer and create/send answer
        startWebRTCSession(isInitiator = false)
    }

    /**
     * Reject incoming call.
     */
    fun rejectCall() {
        if (_callState.value !is CallState.RINGING) return
        val target = _opponentId.value ?: ""
        signalingClient?.rejectCall(target)
        
        // Log missed/rejected call
        saveCallLog(com.example.data.model.CallLogStatus.REJECTED)
        
        resetCall()
    }

    /**
     * End ongoing call.
     */
    fun endCall() {
        val target = _opponentId.value
        if (target != null) {
            signalingClient?.endCall(target)
        }
        
        // Log completed call with duration
        if (_callState.value == CallState.CONNECTED) {
            saveCallLog(com.example.data.model.CallLogStatus.COMPLETED)
        } else if (_callState.value is CallState.OUTGOING) {
            saveCallLog(com.example.data.model.CallLogStatus.CANCELLED)
        }

        resetCall()
    }

    private fun startWebRTCSession(isInitiator: Boolean) {
        this.isInitiator = isInitiator
        Log.d(TAG, "Starting WebRTC Session, isInitiator=$isInitiator")
        
        webRtcClient?.close()
        webRtcClient = WebRTCClient(context, eglBaseContext, this)

        val hasAudio = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED

        // Standard setup: start audio capturer if permission granted
        if (hasAudio) {
            webRtcClient?.startLocalAudio()
        } else {
            Log.w(TAG, "Missing RECORD_AUDIO permission, starting in receive-only mode for audio")
        }

        // If video call, start local camera and render it if permission granted
        if (_callType.value == CallType.VIDEO) {
            if (hasCamera) {
                localVideoView?.let { view ->
                    webRtcClient?.startLocalVideo(view)
                }
            } else {
                Log.w(TAG, "Missing CAMERA permission, starting in receive-only mode for video")
            }
        }

        if (isInitiator) {
            webRtcClient?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(offerSdp: SessionDescription?) {
                    offerSdp?.let {
                        val callerName = SupabaseClient.currentProfile?.displayName 
                            ?: SupabaseClient.currentUser?.email 
                            ?: "Panalink User"
                        signalingClient?.sendOffer(
                            targetUserId = _opponentId.value ?: "",
                            offerSdp = it.description,
                            callerName = callerName,
                            callType = if (_callType.value == CallType.VIDEO) "video" else "voice"
                        )
                    }
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(p0: String?) {}
            })
        } else {
            val sdpStr = incomingSdpOffer
            if (!sdpStr.isNullOrEmpty()) {
                Log.d(TAG, "Applying remote offer SDP on receiver side")
                remoteDescriptionSet = false
                webRtcClient?.setRemoteDescription(
                    SessionDescription(SessionDescription.Type.OFFER, sdpStr),
                    object : SdpObserver {
                        override fun onCreateSuccess(p0: SessionDescription?) {}
                        override fun onSetSuccess() {
                            Log.d(TAG, "Remote description set, creating WebRTC Answer")
                            mainScope.launch {
                                remoteDescriptionSet = true
                                flushPendingIceCandidates()
                            }
                            webRtcClient?.createAnswer(object : SdpObserver {
                                override fun onCreateSuccess(answerSdp: SessionDescription?) {
                                    answerSdp?.let {
                                        signalingClient?.sendAnswer(_opponentId.value ?: "", it.description)
                                    }
                                }
                                override fun onSetSuccess() {}
                                override fun onCreateFailure(p0: String?) {}
                                override fun onSetFailure(p0: String?) {}
                            })
                        }
                        override fun onCreateFailure(p0: String?) {}
                        override fun onSetFailure(err: String?) {
                            Log.e(TAG, "Offer SDP setRemoteDescription failed: $err")
                        }
                    }
                )
            } else {
                Log.e(TAG, "Error: incomingSdpOffer is null or empty when starting receiver WebRTC session!")
            }
        }
    }

    /**
     * Set rendering views from Compose UI for local/remote video.
     */
    fun setVideoViews(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        localVideoView = local
        remoteVideoView = remote
        
        // Re-add remote sink if video track is already running
        remoteVideoTrack?.let { track ->
            remote?.let { view -> track.addSink(view) }
        }
    }

    fun toggleMute() {
        val newMute = !_isMuted.value
        _isMuted.value = newMute
        webRtcClient?.toggleMic(!newMute)
    }

    fun toggleSpeaker() {
        val newSpeaker = !_isSpeakerOn.value
        _isSpeakerOn.value = newSpeaker
        audioController.setAudioDevice(if (newSpeaker) AudioDevice.SPEAKER else AudioDevice.EARPIECE)
    }

    fun toggleCamera() {
        val newCamera = !_isCameraOn.value
        _isCameraOn.value = newCamera
        webRtcClient?.toggleVideo(newCamera)
    }

    fun switchCamera() {
        webRtcClient?.switchCamera()
    }

    private fun startDurationTimer() {
        durationJob?.cancel()
        _duration.value = 0L
        durationJob = mainScope.launch {
            while (isActive && _callState.value == CallState.CONNECTED) {
                delay(1000)
                _duration.value = _duration.value + 1
            }
        }
    }

    private fun updateCallState(newState: CallState) {
        if (_callState.value == newState) return
        _callState.value = newState
        evaluateConnectionGuard(newState)
    }

    private fun evaluateConnectionGuard(state: CallState) {
        when (state) {
            is CallState.CONNECTING -> {
                Log.d(TAG, "Connection Guard: Transitioned to CONNECTING. Starting $INITIAL_CONNECTION_TIMEOUT ms timer.")
                startConnectionGuard(INITIAL_CONNECTION_TIMEOUT)
            }
            is CallState.RECONNECTING -> {
                Log.d(TAG, "Connection Guard: Transitioned to RECONNECTING. Starting $ICE_RESTART_TIMEOUT ms timer.")
                startConnectionGuard(ICE_RESTART_TIMEOUT)
            }
            is CallState.OUTGOING -> {
                Log.d(TAG, "Connection Guard: Transitioned to OUTGOING. Starting $INITIAL_CONNECTION_TIMEOUT ms timer.")
                startConnectionGuard(INITIAL_CONNECTION_TIMEOUT)
            }
            CallState.CONNECTED -> {
                Log.d(TAG, "Connection Guard: Connected successfully. Stopping timer.")
                stopConnectionGuard()
            }
            CallState.IDLE, CallState.FAILED, CallState.ENDED, CallState.CANCELLED, CallState.BUSY -> {
                Log.d(TAG, "Connection Guard: State is inactive ($state). Stopping timer.")
                stopConnectionGuard()
            }
            else -> {
                // For other states (e.g., RINGING), stop if active or no-op
                stopConnectionGuard()
            }
        }
    }

    private fun startConnectionGuard(timeoutMs: Long) {
        connectionGuardJob?.cancel()
        connectionGuardJob = mainScope.launch {
            delay(timeoutMs)
            terminateCallWithFailure()
        }
    }

    private fun stopConnectionGuard() {
        connectionGuardJob?.cancel()
        connectionGuardJob = null
    }

    private fun terminateCallWithFailure() {
        if (_callState.value == CallState.CONNECTED) {
            Log.d(TAG, "Connection Guard: Call is already connected. Aborting timeout.")
            return
        }
        if (isCleaningCall) return
        isCleaningCall = true

        Log.e(TAG, "Connection Guard triggered! Call setup/reconnection timed out.")
        updateCallState(CallState.FAILED)
        saveCallLog(com.example.data.model.CallLogStatus.MISSED)
        
        mainScope.launch {
            delay(3000)
            resetCall()
        }
    }

    private fun resetCall() {
        Log.d(TAG, "Resetting Call Manager states")
        com.example.data.repository.PresenceRepository.updateMyStatus(com.example.data.repository.UserPresenceStatus.ONLINE)
        stopConnectionGuard()
        isCleaningCall = false
        
        // Stop foreground service
        CallForegroundService.stopService(context)

        durationJob?.cancel()
        durationJob = null
        _duration.value = 0L
        incomingSdpOffer = null
        
        isInitiator = false
        remoteDescriptionSet = false
        pendingIceCandidates.clear()
        
        webRtcClient?.close()
        webRtcClient = null
        
        updateCallState(CallState.IDLE)
        _opponentId.value = null
        _opponentName.value = null
        _opponentId.value = null
        _isMuted.value = false
        _isCameraOn.value = true
        
        localVideoView = null
        remoteVideoView = null
        remoteVideoTrack = null

        audioController.stopCall()
    }

    private fun flushPendingIceCandidates() {
        Log.d(TAG, "Flushing ${pendingIceCandidates.size} pending ICE candidates to WebRTCClient")
        pendingIceCandidates.forEach { candidate ->
            webRtcClient?.addIceCandidate(candidate)
        }
        pendingIceCandidates.clear()
    }

    private fun triggerIceRestart() {
        if (isInitiator) {
            Log.d(TAG, "We are the initiator. Executing restartIce and creating new offer...")
            webRtcClient?.restartIce()
            webRtcClient?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(offerSdp: SessionDescription?) {
                    offerSdp?.let {
                        val callerName = SupabaseClient.currentProfile?.displayName 
                            ?: SupabaseClient.currentUser?.email 
                            ?: "Panalink User"
                        signalingClient?.sendOffer(
                            targetUserId = _opponentId.value ?: "",
                            offerSdp = it.description,
                            callerName = callerName,
                            callType = if (_callType.value == CallType.VIDEO) "video" else "voice"
                        )
                    }
                }
                override fun onSetSuccess() {}
                override fun onCreateFailure(p0: String?) {}
                override fun onSetFailure(p0: String?) {}
            })
        } else {
            Log.d(TAG, "We are the receiver. Waiting for the initiator to restart ICE...")
        }
    }

    /**
     * Saves a call log event and optionally triggers a chat message.
     */
    private fun saveCallLog(status: com.example.data.model.CallLogStatus) {
        val opponentId = _opponentId.value ?: return
        val currentUid = SupabaseClient.currentUser?.id ?: return
        val duration = _duration.value
        val isVideo = _callType.value == CallType.VIDEO

        val log = com.example.data.model.CallLog(
            id = java.util.UUID.randomUUID().toString(),
            callerId = if (_callState.value is CallState.OUTGOING) currentUid else opponentId,
            receiverId = if (_callState.value is CallState.OUTGOING) opponentId else currentUid,
            type = if (isVideo) com.example.data.model.CallLogType.VIDEO else com.example.data.model.CallLogType.VOICE,
            status = status,
            durationSeconds = duration,
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date())
        )

        // In a real app, we would save this to Room and sync with Supabase.
        // For this task, we will simulate adding a message to the chat so it shows up in history.
        mainScope.launch(Dispatchers.IO) {
            try {
                val moshi = com.squareup.moshi.Moshi.Builder()
                    .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                    .build()
                val adapter = moshi.adapter(com.example.data.model.CallLog::class.java)
                val jsonLog = adapter.toJson(log)

                // Resolve chatId and save as a special "call" message
                val chatsRepo = com.example.data.repository.ChatsRepository()
                val chatId = chatsRepo.getChatIdByOtherUserId(opponentId)
                
                if (!chatId.isNullOrEmpty()) {
                    val messagesRepo = com.example.data.repository.MessagesRepository.getInstance()
                    messagesRepo.sendMessage(
                        chatId = chatId,
                        content = jsonLog,
                        messageType = "call",
                        receiverUid = opponentId
                    )
                    Log.d(TAG, "Call log saved successfully as a message in chat $chatId")
                } else {
                    Log.w(TAG, "Could not find chatId to save call log for opponent $opponentId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving call log", e)
            }
        }
    }

    // WebRTCListener implementations
    override fun onIceCandidateCreated(candidate: IceCandidate) {
        val target = _opponentId.value ?: return
        signalingClient?.sendIceCandidate(
            targetUserId = target,
            sdpMid = candidate.sdpMid ?: "",
            sdpMLineIndex = candidate.sdpMLineIndex,
            candidate = candidate.sdp ?: ""
        )
    }

    override fun onRemoteTrackAdded(transceiver: RtpTransceiver) {
        val track = transceiver.receiver.track()
        if (track is VideoTrack) {
            Log.d(TAG, "Remote VideoTrack received and bound")
            remoteVideoTrack = track
            remoteVideoView?.let { view ->
                track.addSink(view)
            }
        }
    }

    override fun onIceConnectionStateChanged(state: PeerConnection.IceConnectionState) {
        mainScope.launch {
            when (state) {
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED -> {
                    if (_callState.value != CallState.CONNECTED) {
                        updateCallState(CallState.CONNECTED)
                        startDurationTimer()
                    }
                }
                PeerConnection.IceConnectionState.DISCONNECTED -> {
                    Log.d(TAG, "ICE Connection DISCONNECTED. Transitioning to RECONNECTING state.")
                    updateCallState(CallState.RECONNECTING)
                    delay(4000)
                    if (_callState.value == CallState.RECONNECTING) {
                        Log.d(TAG, "Still disconnected after 4 seconds. Triggering ICE restart...")
                        triggerIceRestart()
                    }
                }
                PeerConnection.IceConnectionState.FAILED -> {
                    Log.d(TAG, "ICE Connection FAILED. Transitioning to FAILED state.")
                    updateCallState(CallState.FAILED)
                    delay(3000)
                    if (_callState.value == CallState.FAILED) {
                        resetCall()
                    }
                }
                PeerConnection.IceConnectionState.CLOSED -> {
                    resetCall()
                }
                else -> {}
            }
        }
    }

    fun formattedDuration(): String {
        val sec = _duration.value
        val minutes = TimeUnit.SECONDS.toMinutes(sec)
        val seconds = sec - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun release() {
        resetCall()
        audioController.release()
        signalingClient?.disconnect()
        signalingClient = null
        currentUserId = null
    }
}
