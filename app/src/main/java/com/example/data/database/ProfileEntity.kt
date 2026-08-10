package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Profile

@Entity(tableName = "local_profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val avatarUrl: String?,
    val profileTheme: String? = "dark_teal",
    val createdAt: String? = null,
    val publicKey: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val status: String? = "active",
    val birthDate: String? = null,
    val sex: String? = null,
    val interests: String? = null, // Stored as JSON string
    val qrPayload: String? = null,
    val coverUrl: String? = null,
    val isProfileComplete: Boolean = false,
    val pinHash: String? = null,
    val pin: String? = null,
    val pinUpdatedAt: String? = null,
    val profileBadges: String? = null, // Stored as JSON string
    val lastProfileEdit: String? = null,
    val profileEditCount: Int? = 0,
    val deviceFingerprint: String? = null,
    
    // IMCE Fields
    val avatarLocalPath: String? = null,
    val coverLocalPath: String? = null,
    val updatedAt: String? = null,
    val lastSyncedAt: Long? = null,
    val syncVersion: Int = 0,
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false
) {
    fun toProfile(): Profile {
        val pubKey = com.example.util.CryptoManager.cleanPublicKey(publicKey)
        if (pubKey.isNotEmpty()) {
            com.example.util.CryptoManager.publicKeyCache[id] = pubKey
        }
        val moshi = com.example.data.supabase.SupabaseClient.moshi
        val listAdapter = moshi.adapter<List<String>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java))
        
        val interestsList = try {
            if (!interests.isNullOrEmpty()) {
                listAdapter.fromJson(interests)
            } else null
        } catch (e: Exception) { null }
        
        val badgesList = try {
            if (!profileBadges.isNullOrEmpty()) {
                listAdapter.fromJson(profileBadges)
            } else null
        } catch (e: Exception) { null }

        return Profile(
            id = id,
            displayName = displayName,
            avatarUrl = avatarUrl,
            profileTheme = profileTheme,
            createdAt = createdAt,
            publicKey = if (pubKey.isNotEmpty()) pubKey else null,
            firstName = firstName,
            lastName = lastName,
            status = status,
            birthDate = birthDate,
            sex = sex,
            interests = interestsList ?: emptyList(),
            qrPayload = qrPayload,
            coverUrl = coverUrl,
            isProfileComplete = isProfileComplete,
            pinHash = pinHash,
            pin = pin,
            pinUpdatedAt = pinUpdatedAt,
            profileBadges = badgesList ?: emptyList(),
            lastProfileEdit = lastProfileEdit,
            profileEditCount = profileEditCount,
            deviceFingerprint = deviceFingerprint
        )
    }

    companion object {
        fun fromProfile(profile: Profile): ProfileEntity {
            val pubKey = com.example.util.CryptoManager.cleanPublicKey(profile.publicKey)
            if (pubKey.isNotEmpty()) {
                com.example.util.CryptoManager.publicKeyCache[profile.id] = pubKey
            }
            val moshi = com.example.data.supabase.SupabaseClient.moshi
            val listAdapter = moshi.adapter<List<String>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java))
            
            val interestsJson = try {
                if (profile.interests != null) {
                    listAdapter.toJson(profile.interests)
                } else null
            } catch (e: Exception) { null }
            
            val badgesJson = try {
                if (profile.profileBadges != null) {
                    listAdapter.toJson(profile.profileBadges)
                } else null
            } catch (e: Exception) { null }

            return ProfileEntity(
                id = profile.id,
                displayName = profile.displayName,
                avatarUrl = profile.avatarUrl,
                profileTheme = profile.profileTheme,
                createdAt = profile.createdAt,
                publicKey = if (pubKey.isNotEmpty()) pubKey else null,
                firstName = profile.firstName,
                lastName = profile.lastName,
                status = profile.status,
                birthDate = profile.birthDate,
                sex = profile.sex,
                interests = interestsJson,
                qrPayload = profile.qrPayload,
                coverUrl = profile.coverUrl,
                isProfileComplete = profile.isProfileComplete,
                pinHash = profile.pinHash,
                pin = profile.pin,
                pinUpdatedAt = profile.pinUpdatedAt,
                profileBadges = badgesJson,
                lastProfileEdit = profile.lastProfileEdit,
                profileEditCount = profile.profileEditCount,
                deviceFingerprint = profile.deviceFingerprint
            )
        }
    }
}
