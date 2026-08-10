package com.example.creative.video

data class SpeedOption(
    val speed: Float,
    val label: String
)

object VideoSpeedController {
    val defaultOptions = listOf(
        SpeedOption(0.5f, "0.5x"),
        SpeedOption(1.0f, "1.0x"),
        SpeedOption(1.5f, "1.5x"),
        SpeedOption(2.0f, "2.0x"),
        SpeedOption(3.0f, "3.0x")
    )

    fun calculateDurationWithSpeed(originalDurationMs: Long, speed: Float): Long {
        if (speed <= 0f) return originalDurationMs
        return (originalDurationMs / speed).toLong()
    }
}
