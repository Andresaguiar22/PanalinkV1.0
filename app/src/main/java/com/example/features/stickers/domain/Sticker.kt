package com.example.features.stickers.domain

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Sticker(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "image_url") val imageUrl: String,
    @Json(name = "emoji") val emoji: String? = null,
    @Json(name = "pack_id") val packId: String? = null
)
