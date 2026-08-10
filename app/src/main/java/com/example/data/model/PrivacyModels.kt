package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserEntitlementDto(
    @Json(name = "user_id") val userId: String,
    @Json(name = "feature_code") val featureCode: String,
    @Json(name = "enabled") val enabled: Boolean,
    @Json(name = "expires_at") val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class UserPrivacySettingDto(
    @Json(name = "user_id") val userId: String,
    @Json(name = "feature_code") val featureCode: String,
    @Json(name = "value") val value: Map<String, Any>? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)
