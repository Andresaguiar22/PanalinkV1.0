package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Retrofit DTO for consuming the public profiles endpoint/view from Supabase PostgREST.
 */
@JsonClass(generateAdapter = true)
data class PublicProfileDto(
    @Json(name = "id") val id: String,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)
