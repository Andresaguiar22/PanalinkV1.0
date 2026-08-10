package com.example.data.model

import com.squareup.moshi.JsonClass

/**
 * Modern public profile model for PanaLink V2.0.
 * Represents public identity details of any user.
 *
 * Excludes private attributes (e.g., pin, phone, email, device fingerprint, presence).
 */
@JsonClass(generateAdapter = true)
data class PublicProfile(
    val id: String,
    val displayName: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null,
    val updatedAt: String? = null
)
