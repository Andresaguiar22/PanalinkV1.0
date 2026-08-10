package com.example.ui.settings.models

sealed interface NotificationAction {
    data class SetGlobalEnabled(val enabled: Boolean) : NotificationAction
    data class SetSoundEnabled(val enabled: Boolean) : NotificationAction
    data class SetSoundTone(val tone: String) : NotificationAction
    data class SetVibrationEnabled(val enabled: Boolean) : NotificationAction
    data class SetVibrationPattern(val pattern: String) : NotificationAction
    data class SetChatSoundEnabled(val enabled: Boolean) : NotificationAction
    data class SetChatSoundTone(val tone: String) : NotificationAction
    data class SetOutgoingSoundEnabled(val enabled: Boolean) : NotificationAction
}
