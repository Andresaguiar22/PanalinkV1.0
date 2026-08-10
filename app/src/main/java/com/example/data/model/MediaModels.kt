package com.example.data.model

data class MediaPreview(
    val provider: String,
    val videoId: String,
    val title: String,
    val thumbnailUrl: String,
    val duration: String? = null,
    val embedUrl: String
)
