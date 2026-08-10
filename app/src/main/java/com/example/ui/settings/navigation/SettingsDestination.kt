package com.example.ui.settings.navigation

sealed class SettingsDestination(val route: String) {
    object Dashboard : SettingsDestination("settings_dashboard")
    object ProfileEdit : SettingsDestination("settings_profile_edit")
    object PresenceCenter : SettingsDestination("settings_presence_center")
    object PrivacyCenter : SettingsDestination("settings_privacy_center")
    object SecurityCenter : SettingsDestination("settings_security_center")
    object ChatsCenter : SettingsDestination("settings_chats_center")
    object NotificationCenter : SettingsDestination("settings_notification_center")
    object CustomizationCenter : SettingsDestination("settings_customization_center")
    object StorageCenter : SettingsDestination("settings_storage_center")
    object ActivityCenter : SettingsDestination("settings_activity_center")
    object About : SettingsDestination("settings_about")
}
