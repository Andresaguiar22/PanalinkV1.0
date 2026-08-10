package com.example.ui.settings.models

sealed class SettingsEvent {
    object OpenProfile : SettingsEvent()
    object OpenPresence : SettingsEvent()
    object OpenPrivacy : SettingsEvent()
    object OpenSecurity : SettingsEvent()
    object OpenChats : SettingsEvent()
    object OpenNotifications : SettingsEvent()
    object OpenCustomization : SettingsEvent()
    object OpenStorage : SettingsEvent()
    object OpenActivity : SettingsEvent()
    object OpenAbout : SettingsEvent()
    
    object Logout : SettingsEvent()
    object ClearCache : SettingsEvent()
    object ExportData : SettingsEvent()
    
    object NavigateBack : SettingsEvent()
}
