package com.example.identity.model

import androidx.annotation.Keep

@Keep
data class IdentityUiState(
    val userId: String,
    val displayName: String?,
    val avatarUrl: String?,
    val avatarLocalPath: String? = null,
    val verified: Boolean = false
)

sealed class AvatarDownloadResult {
    data class Success(val localPath: String) : AvatarDownloadResult()
    data class Error(val message: String) : AvatarDownloadResult()
}

fun IdentityUiState.toIdentityUiState() = this
