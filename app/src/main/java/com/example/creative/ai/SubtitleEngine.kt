package com.example.creative.ai

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

data class SubtitleItem(
    val id: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val wordTimings: List<WordTiming> = emptyList()
)

data class WordTiming(
    val word: String,
    val startMs: Long,
    val endMs: Long
)

enum class SubtitleStylePreset {
    CLASSIC_WHITE,
    NEON_YELLOW,
    BACKGROUND_BLACK,
    GRADIENT_BOUNCE
}

data class SubtitleStyle(
    val preset: SubtitleStylePreset = SubtitleStylePreset.NEON_YELLOW,
    val fontSizeSp: Float = 24f,
    val textColorHex: String = "#FFFF00",
    val backgroundColorHex: String = "#80000000",
    val strokeColorHex: String = "#000000",
    val animatedHighlight: Boolean = true
)

object SubtitleEngine {

    suspend fun transcribeAudioTrack(
        videoPath: String,
        onProgress: (Float) -> Unit
    ): List<SubtitleItem> {
        // Simulates AI Speech-to-Text transcription with precise timing
        onProgress(0.2f)
        delay(150)
        onProgress(0.5f)
        delay(150)
        onProgress(0.9f)

        val sampleCaptions = listOf(
            SubtitleItem(
                id = "sub_1",
                startTimeMs = 0L,
                endTimeMs = 3000L,
                text = "¡Bienvenidos a PanaLink Creative Studio!",
                wordTimings = listOf(
                    WordTiming("¡Bienvenidos", 0L, 800L),
                    WordTiming("a", 850L, 1100L),
                    WordTiming("PanaLink!", 1150L, 3000L)
                )
            ),
            SubtitleItem(
                id = "sub_2",
                startTimeMs = 3200L,
                endTimeMs = 6500L,
                text = "Crea contenido increíble con Inteligencia Artificial.",
                wordTimings = listOf(
                    WordTiming("Crea", 3200L, 3800L),
                    WordTiming("contenido", 3850L, 4600L),
                    WordTiming("increíble", 4650L, 5500L),
                    WordTiming("con", 5550L, 5800L),
                    WordTiming("IA.", 5850L, 6500L)
                )
            )
        )
        onProgress(1.0f)
        return sampleCaptions
    }
}
