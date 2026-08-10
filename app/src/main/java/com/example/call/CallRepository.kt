package com.example.call

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer

/**
 * CallRepository serves as the standard MVVM repository pattern, decoupling
 * the ViewModel UI state layers from direct CallManager interactions.
 */
class CallRepository(private val context: Context) {
    private val callManager = CallManager.getInstance(context)

    val callState: StateFlow<CallState> = callManager.callState
    val callType: StateFlow<CallType> = callManager.callType
    val duration: StateFlow<Long> = callManager.duration
    val opponentId: StateFlow<String?> = callManager.opponentId
    val opponentName: StateFlow<String?> = callManager.opponentName
    val isMuted: StateFlow<Boolean> = callManager.isMuted
    val isSpeakerOn: StateFlow<Boolean> = callManager.isSpeakerOn
    val isCameraOn: StateFlow<Boolean> = callManager.isCameraOn

    fun initialize(userId: String) {
        callManager.initialize(userId)
    }

    fun startCall(targetUserId: String, targetUserName: String, type: CallType) {
        callManager.startCall(targetUserId, targetUserName, type)
    }

    fun acceptCall() {
        callManager.acceptCall()
    }

    fun rejectCall() {
        callManager.rejectCall()
    }

    fun endCall() {
        callManager.endCall()
    }

    fun toggleMute() {
        callManager.toggleMute()
    }

    fun toggleSpeaker() {
        callManager.toggleSpeaker()
    }

    fun toggleCamera() {
        callManager.toggleCamera()
    }

    fun switchCamera() {
        callManager.switchCamera()
    }

    fun getFormattedDuration(): String {
        return callManager.formattedDuration()
    }

    fun getEglContext(): EglBase.Context {
        return callManager.eglBaseContext
    }
    
    fun setVideoViews(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        callManager.setVideoViews(local, remote)
    }
}
