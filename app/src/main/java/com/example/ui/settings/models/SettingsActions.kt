package com.example.ui.settings.models

import android.content.Context
import java.io.File

sealed class SettingsAction {
    data class SaveProfile(val profileModel: ProfileUiModel) : SettingsAction()
    data class UploadAvatar(val context: Context, val file: File, val mimeType: String) : SettingsAction()
    data class UploadCover(val context: Context, val file: File, val mimeType: String) : SettingsAction()
    
    data class ChangeWallpaper(val wallpaperId: String) : SettingsAction()
    data class UpdatePrivacy(val level: String) : SettingsAction()
    data class UpdateInvisibility(val mode: String) : SettingsAction()
    
    data class EnableNotifications(val enabled: Boolean) : SettingsAction()
    data class DisableNotifications(val disabled: Boolean) : SettingsAction()
    
    data class GenerateQr(val pin: String) : SettingsAction()
    object ClearStorage : SettingsAction()
    object SyncNow : SettingsAction()
    object Logout : SettingsAction()
}
