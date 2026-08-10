package com.example.call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer

/**
 * VideoCallViewModel handles state distribution, UI event routing, EGL contexts, and
 * camera feeds for video call screens.
 */
class VideoCallViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CallRepository(application)

    val callState: StateFlow<CallState> = repository.callState
    val duration: StateFlow<Long> = repository.duration
    val opponentName: StateFlow<String?> = repository.opponentName
    val isMuted: StateFlow<Boolean> = repository.isMuted
    val isSpeakerOn: StateFlow<Boolean> = repository.isSpeakerOn
    val isCameraOn: StateFlow<Boolean> = repository.isCameraOn

    fun acceptCall() {
        repository.acceptCall()
    }

    fun rejectCall() {
        repository.rejectCall()
    }

    fun endCall() {
        repository.endCall()
    }

    fun toggleMute() {
        repository.toggleMute()
    }

    fun toggleSpeaker() {
        repository.toggleSpeaker()
    }

    fun toggleCamera() {
        repository.toggleCamera()
    }

    fun switchCamera() {
        repository.switchCamera()
    }

    fun getEglContext(): EglBase.Context {
        return repository.getEglContext()
    }

    fun setVideoViews(local: SurfaceViewRenderer?, remote: SurfaceViewRenderer?) {
        repository.setVideoViews(local, remote)
    }

    fun getFormattedDuration(): String {
        return repository.getFormattedDuration()
    }
}
