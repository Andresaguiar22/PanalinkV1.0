package com.example.media.externalvideo

enum class PlatformType(val displayName: String, val brandColorHex: String) {
    YOUTUBE("YouTube", "#FF0000"),
    INSTAGRAM("Instagram", "#E4405F"),
    FACEBOOK("Facebook", "#1877F2"),
    TIKTOK("TikTok", "#000000"),
    TWITTER("X (Twitter)", "#1DA1F2"),
    OTHER("Video Enlace", "#38BDF8")
}

object PlatformDetector {
    fun detectPlatform(url: String): PlatformType {
        val lower = url.lowercase()
        return when {
            lower.contains("youtube.com") || lower.contains("youtu.be") -> PlatformType.YOUTUBE
            lower.contains("instagram.com") -> PlatformType.INSTAGRAM
            lower.contains("facebook.com") || lower.contains("fb.watch") -> PlatformType.FACEBOOK
            lower.contains("tiktok.com") -> PlatformType.TIKTOK
            lower.contains("twitter.com") || lower.contains("x.com") -> PlatformType.TWITTER
            else -> PlatformType.OTHER
        }
    }
}
