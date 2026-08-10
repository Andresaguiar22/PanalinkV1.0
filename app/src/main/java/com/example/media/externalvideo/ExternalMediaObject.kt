package com.example.media.externalvideo

/**
 * P6.7 - External Media Object
 * Metadata object representing imported or embedded external videos (YouTube, Instagram, TikTok, Facebook, Twitter).
 */
data class ExternalMediaObject(
    val url: String,
    val platform: PlatformType,
    val title: String,
    val thumbnail: String? = null,
    val author: String? = null,
    val durationSeconds: Long = 0L,
    val embedSupported: Boolean = true
)
