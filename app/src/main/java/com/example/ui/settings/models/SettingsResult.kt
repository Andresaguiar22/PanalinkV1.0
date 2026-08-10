package com.example.ui.settings.models

sealed class SettingsResult<out T> {
    object Idle : SettingsResult<Nothing>()
    object Loading : SettingsResult<Nothing>()
    data class Success<out T>(val data: T) : SettingsResult<T>()
    data class Error(val message: String, val exception: Throwable? = null) : SettingsResult<Nothing>()
    data class ValidationError(val message: String) : SettingsResult<Nothing>()
    object Offline : SettingsResult<Nothing>()
    data class PermissionRequired(val permission: String) : SettingsResult<Nothing>()
}
