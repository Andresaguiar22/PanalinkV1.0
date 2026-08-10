package com.example.ui.settings.models

data class ProfileUiModel(
    val displayName: String = "",
    val avatarUrl: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val statusText: String = "",
    val birthDate: String = "",
    val sex: String = "",
    val interests: List<String> = emptyList(),
    val coverUrl: String = ""
)

data class PresenceUiModel(
    val status: String = "online"
)

data class PrivacyUiModel(
    val lastSeenVisibility: String = "Mis Contactos",
    val readReceiptsEnabled: Boolean = true,
    val advancedInvisibility: String = "contacts",
    val smartReadReceipt: String = "contacts"
)

data class NotificationUiModel(
    val globalEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val soundTone: String = "default",
    val vibrationPattern: String = "default",
    val chatSoundEnabled: Boolean = true,
    val chatSoundTone: String = "water_drop",
    val outgoingSoundEnabled: Boolean = true
)

data class ChatsUiModel(
    val textSize: Float = 15f,
    val enterSends: Boolean = false,
    val wallpaper: String = "dark_slate"
)

data class CustomizationUiModel(
    val profileTheme: String = "dark_teal",
    val bottomBarColorPreset: String = "tropical",
    val bottomBarShapePreset: String = "pill",
    val minimalistMode: Boolean = false,
    val customPrimaryColor: Int? = null,
    val customSecondaryColor: Int? = null
)

data class StorageUiModel(
    val totalUsedBytes: Long = 0L,
    val mediaUsedBytes: Long = 0L,
    val cacheUsedBytes: Long = 0L,
    val autoDownloadMedia: Boolean = true
)

data class ActivityUiModel(
    val activeDevicesCount: Int = 1,
    val currentDeviceName: String = "Este dispositivo",
    val totalMessagesSent: Int = 0,
    val totalCallsMade: Int = 0
)

data class AboutUiModel(
    val version: String = "2.0.0",
    val supportEmail: String = "soporte@panalink.com"
)
