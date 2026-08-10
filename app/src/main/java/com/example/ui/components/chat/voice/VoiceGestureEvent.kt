package com.example.ui.components.chat.voice

sealed interface VoiceGestureEvent {
    object StartRecording : VoiceGestureEvent
    object LockRecording : VoiceGestureEvent
    object CancelRecording : VoiceGestureEvent
    object FinishRecording : VoiceGestureEvent
    object PauseRecording : VoiceGestureEvent
    object ResumeRecording : VoiceGestureEvent
    object StopAndPreviewRecording : VoiceGestureEvent
}

