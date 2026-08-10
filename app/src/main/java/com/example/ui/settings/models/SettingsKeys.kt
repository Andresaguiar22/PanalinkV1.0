package com.example.ui.settings.models

object SettingsKeys {
    const val PREFS_NAME = "panalink_prefs"
    
    // Privacy
    fun privacyLastSeen(uid: String) = "privacy_last_seen_$uid"
    fun privacyReadReceipts(uid: String) = "privacy_read_receipts_$uid"
    fun profilePresence(uid: String) = "profile_presence_$uid"
    fun profileInvisibility(uid: String) = "profile_invisibility_$uid"
    fun profileSmartRead(uid: String) = "profile_smart_read_$uid"
    
    // Chats & Appearance
    fun chatTextSize(uid: String) = "chat_text_size_$uid"
    fun chatEnterSends(uid: String) = "chat_enter_sends_$uid"
    fun chatWallpaper(uid: String) = "chat_wallpaper_$uid"
    
    // Theme & Customization
    fun profileTheme(uid: String) = "profile_theme_$uid"
    const val BOTTOM_BAR_COLOR_PRESET = "bottom_bar_color_preset"
    const val BOTTOM_BAR_SHAPE_PRESET = "bottom_bar_shape_preset"
    const val CUSTOM_PRIMARY = "custom_primary"
    const val CUSTOM_SECONDARY = "custom_secondary"
    const val MINIMALIST_MODE_GLOBAL = "minimalist_mode_global"
    
    // Notifications
    const val NOTIFICATIONS_GLOBAL_ENABLED = "notifications_global_enabled"
    const val NOTIFICATIONS_SOUND_ENABLED = "notifications_sound_enabled"
    const val NOTIFICATIONS_VIBRATION_ENABLED = "notifications_vibration_enabled"
    const val NOTIFICATIONS_SOUND_TONE = "notifications_sound_tone"
    const val NOTIFICATIONS_VIBRATION_PATTERN = "notifications_vibration_pattern"
    const val NOTIFICATIONS_CHAT_SOUND_ENABLED = "notifications_chat_sound_enabled"
    const val NOTIFICATIONS_CHAT_SOUND_TONE = "notifications_chat_sound_tone"
    const val NOTIFICATIONS_OUTGOING_SOUND_ENABLED = "notifications_outgoing_sound_enabled"

    // Security
    fun securityPin(uid: String) = "security_pin_$uid"
    fun security2Fa(uid: String) = "security_2fa_$uid"
    fun securityBiometrics(uid: String) = "security_biometrics_$uid"
}
