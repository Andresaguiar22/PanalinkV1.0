package com.example.ui.settings.models

sealed interface PresenceAction {
    data class ChangePresenceStatus(val status: String) : PresenceAction
    data class ToggleInvisibleMode(val enabled: Boolean) : PresenceAction
    data class UpdateLastSeenVisibility(val visibility: String) : PresenceAction
    object RefreshPresence : PresenceAction
    object ClearMessages : PresenceAction
}
