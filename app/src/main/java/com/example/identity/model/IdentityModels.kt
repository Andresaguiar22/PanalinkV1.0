package com.example.identity.model

import com.example.data.model.Profile
import androidx.compose.runtime.Immutable

@Immutable
data class IdentityUiState(
    val userId: String,
    val displayName: String,
    val username: String? = null,
    val avatarLocalPath: String? = null,
    val coverLocalPath: String? = null,
    val avatarUrl: String? = null,
    val coverUrl: String? = null,
    val bio: String? = null,
    val verified: Boolean = false,
    val onlineStatus: String? = null,
    val pin: String = ""
)

fun CachedProfile.toIdentityUiState(): IdentityUiState {
    return IdentityUiState(
        userId = profile.id,
        displayName = profile.displayName,
        username = profile.displayName.split(" ").firstOrNull()?.lowercase() ?: "usuario", // Fallback for missing username
        avatarLocalPath = avatarLocalPath,
        coverLocalPath = coverLocalPath,
        avatarUrl = profile.avatarUrl,
        coverUrl = null, // Fallback for missing coverUrl
        bio = null, // Fallback for missing bio
        verified = false, // Fallback for missing verified
        onlineStatus = null, // Fallback for missing onlineStatus
        pin = profile.pin ?: ""
    )
}

data class CachedProfile(
    val profile: Profile,
    val avatarLocalPath: String? = null,
    val coverLocalPath: String? = null,
    val isDirty: Boolean = false,
    val syncVersion: Int = 0,
    val lastSyncedAt: Long? = null
)

enum class IdentitySyncState {
    LOCAL,
    SYNCING,
    UPDATED,
    FAILED
}

enum class ProfileSyncState {
    PENDING,
    SYNCING,
    SUCCESS,
    FAILED
}

sealed class ProfileUpdateResult {
    object Success : ProfileUpdateResult()
    data class Error(val exception: Throwable) : ProfileUpdateResult()
}

sealed class AvatarDownloadResult {
    data class Success(val localPath: String) : AvatarDownloadResult()
    data class Error(val exception: Throwable) : AvatarDownloadResult()
}

sealed class CoverDownloadResult {
    data class Success(val localPath: String) : CoverDownloadResult()
    data class Error(val exception: Throwable) : CoverDownloadResult()
}
