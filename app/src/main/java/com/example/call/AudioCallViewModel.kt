package com.example.call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

/**
 * AudioCallViewModel handles standard and MVVM-compliant voice call state exposures
 * and UI control handlers (accept, reject, mute, end call).
 */
class AudioCallViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CallRepository(application)

    val callState: StateFlow<CallState> = repository.callState
    val duration: StateFlow<Long> = repository.duration
    val opponentName: StateFlow<String?> = repository.opponentName
    val isMuted: StateFlow<Boolean> = repository.isMuted
    val isSpeakerOn: StateFlow<Boolean> = repository.isSpeakerOn

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

    fun getFormattedDuration(): String {
        return repository.getFormattedDuration()
    }
}
