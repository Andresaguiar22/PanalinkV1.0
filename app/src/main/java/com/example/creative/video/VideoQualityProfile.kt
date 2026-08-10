package com.example.creative.video

enum class VideoQualityProfile(
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val frameRate: Int
) {
    LOW_720P(720, 1280, 2_500_000, 30),
    STANDARD_1080P(1080, 1920, 6_000_000, 30),
    HIGH_1080P_60FPS(1080, 1920, 10_000_000, 60),
    STORY_PRO(1080, 1920, 8_000_000, 30),
    REEL_PRO(1080, 1920, 12_000_000, 30)
}
