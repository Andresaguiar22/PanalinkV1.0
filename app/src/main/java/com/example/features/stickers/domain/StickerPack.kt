package com.example.features.stickers.domain

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StickerPack(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "cover_url") val coverUrl: String,
    @Json(name = "stickers") val stickers: List<Sticker> = emptyList()
)
