package com.example.call

sealed class CallState {
    object IDLE : CallState()
    data class OUTGOING(val opponentId: String) : CallState()
    object RINGING : CallState() // INCOMING_RINGING
    object CONNECTING : CallState()
    object CONNECTED : CallState()
    object ENDED : CallState()
    object REJECTED : CallState()
    object BUSY : CallState()
    object CANCELLED : CallState()
    object MISSED : CallState()
    object FAILED : CallState()
    object RECONNECTING : CallState()
    object DISCONNECTED : CallState()
}

enum class CallType {
    AUDIO,
    VIDEO
}
