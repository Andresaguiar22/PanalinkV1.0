package com.example.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson

@JsonClass(generateAdapter = true)
data class ProfileSurrogate(
    @Json(name = "id") val id: String,
    @Json(name = "display_name") val displayName: String?,
    @Json(name = "avatar_url") val avatarUrl: String?,
    @Json(name = "pin_hash") val pinHash: String? = null,
    @Json(name = "pin") val pin: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "profile_theme") val profileTheme: String? = "dark_teal",
    @Json(name = "profile_badges") val profileBadges: List<String>? = emptyList(),
    @Json(name = "last_profile_edit") val lastProfileEdit: String? = null,
    @Json(name = "device_fingerprint") val deviceFingerprint: String? = null,
    @Json(name = "public_key") val publicKey: String? = null,
    @Json(name = "is_profile_complete") val isProfileComplete: Boolean? = false,
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "status") val status: String? = "active",
    @Json(name = "birth_date") val birthDate: String? = null,
    @Json(name = "sex") val sex: String? = null,
    @Json(name = "interests") val interests: List<String>? = emptyList(),
    @Json(name = "qr_payload") val qrPayload: String? = null,
    @Json(name = "cover_url") val coverUrl: String? = null,
    @Json(name = "profile_edit_count") val profileEditCount: Int? = 0
)

class ProfileSurrogateAdapter {
    @FromJson
    fun fromJson(surrogate: ProfileSurrogate): Profile {
        return Profile(
            id = surrogate.id,
            displayName = surrogate.displayName ?: "",
            avatarUrl = surrogate.avatarUrl,
            pinHash = surrogate.pinHash,
            pin = surrogate.pin,
            createdAt = surrogate.createdAt,
            profileTheme = surrogate.profileTheme,
            profileBadges = surrogate.profileBadges,
            lastProfileEdit = surrogate.lastProfileEdit,
            deviceFingerprint = surrogate.deviceFingerprint,
            publicKey = surrogate.publicKey,
            isProfileComplete = surrogate.isProfileComplete ?: false,
            firstName = surrogate.firstName,
            lastName = surrogate.lastName,
            status = surrogate.status,
            birthDate = surrogate.birthDate,
            sex = surrogate.sex,
            interests = surrogate.interests,
            qrPayload = surrogate.qrPayload,
            coverUrl = surrogate.coverUrl,
            profileEditCount = surrogate.profileEditCount
        )
    }

    @ToJson
    fun toJson(profile: Profile): ProfileSurrogate {
        return ProfileSurrogate(
            id = profile.id,
            displayName = profile.displayName,
            avatarUrl = profile.avatarUrl,
            pinHash = profile.pinHash,
            pin = profile.pin,
            createdAt = profile.createdAt,
            profileTheme = profile.profileTheme,
            profileBadges = profile.profileBadges,
            lastProfileEdit = profile.lastProfileEdit,
            deviceFingerprint = profile.deviceFingerprint,
            publicKey = profile.publicKey,
            isProfileComplete = profile.isProfileComplete,
            firstName = profile.firstName,
            lastName = profile.lastName,
            status = profile.status,
            birthDate = profile.birthDate,
            sex = profile.sex,
            interests = profile.interests,
            qrPayload = profile.qrPayload,
            coverUrl = profile.coverUrl,
            profileEditCount = profile.profileEditCount
        )
    }
}
