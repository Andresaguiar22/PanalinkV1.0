package com.example.call

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CallViewModel(application: Application) : AndroidViewModel(application) {
    private val callManager = CallManager.getInstance(application)
    
    private val _callState = MutableStateFlow<CallState>(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState

    private var outgoingCallJob: Job? = null

    init {
        viewModelScope.launch {
            callManager.callState.collect { managerState ->
                // Map low-level CallManager states to UI states
                // This is where you would map managerState to our new sealed class CallState
                // This will require mapping.
            }
        }
    }

    fun initiateCall(remoteUserId: String, userName: String, type: CallType) {
        _callState.value = CallState.OUTGOING(remoteUserId)
        callManager.startCall(remoteUserId, userName, type)
        
        // Start 30s timeout
        outgoingCallJob?.cancel()
        outgoingCallJob = viewModelScope.launch {
            delay(30000)
            if (_callState.value is CallState.OUTGOING) {
                _callState.value = CallState.CANCELLED
                callManager.endCall()
                delay(2000)
                _callState.value = CallState.IDLE
            }
        }
    }

    fun acceptCall() {
        callManager.acceptCall()
    }
    
    fun rejectCall() {
        callManager.rejectCall()
        _callState.value = CallState.REJECTED
    }
    
    fun endCall() {
        callManager.endCall()
        _callState.value = CallState.ENDED
    }
}
