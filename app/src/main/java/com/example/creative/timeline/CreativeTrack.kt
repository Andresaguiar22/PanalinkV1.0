package com.example.creative.timeline

import com.example.creative.core.CreativeLayer

/**
 * P6.5B - Creative Engine Multi-Track Architecture
 * Extended track controls: Mute, Solo, Lock, Visibility, Expanded mode.
 */

sealed class CreativeTrack {
    abstract val id: String
    abstract val name: String
    abstract val isMuted: Boolean
    abstract val isSolo: Boolean
    abstract val isLocked: Boolean
    abstract val isVisible: Boolean
    abstract val isExpanded: Boolean
    abstract val startOffsetMs: Long
    abstract val durationMs: Long

    data class VideoTrack(
        override val id: String = java.util.UUID.randomUUID().toString(),
        override val name: String = "Pista de Video",
        override val isMuted: Boolean = false,
        override val isSolo: Boolean = false,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val isExpanded: Boolean = true,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        val videoUri: String = "",
        val speed: Float = 1.0f,
        val volume: Float = 1.0f,
        val trimStartMs: Long = 0L,
        val trimEndMs: Long = 15000L
    ) : CreativeTrack()

    data class AudioTrack(
        override val id: String = java.util.UUID.randomUUID().toString(),
        override val name: String = "Pista de Música",
        override val isMuted: Boolean = false,
        override val isSolo: Boolean = false,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val isExpanded: Boolean = true,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        val audioUri: String = "",
        val volume: Float = 1.0f,
        val fadeInMs: Long = 0L,
        val fadeOutMs: Long = 0L
    ) : CreativeTrack()

    data class VoiceTrack(
        override val id: String = java.util.UUID.randomUUID().toString(),
        override val name: String = "Voz en Off",
        override val isMuted: Boolean = false,
        override val isSolo: Boolean = false,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val isExpanded: Boolean = true,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        val voiceUri: String = "",
        val volume: Float = 1.0f,
        val noiseReduction: Boolean = true
    ) : CreativeTrack()

    data class StickerTrack(
        override val id: String = java.util.UUID.randomUUID().toString(),
        override val name: String = "Stickers & Overlays",
        override val isMuted: Boolean = false,
        override val isSolo: Boolean = false,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val isExpanded: Boolean = true,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        val layers: List<CreativeLayer.Sticker> = emptyList()
    ) : CreativeTrack()

    data class TextTrack(
        override val id: String = java.util.UUID.randomUUID().toString(),
        override val name: String = "Pista de Texto",
        override val isMuted: Boolean = false,
        override val isSolo: Boolean = false,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val isExpanded: Boolean = true,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        val layers: List<CreativeLayer.Text> = emptyList()
    ) : CreativeTrack()

    data class FXTrack(
        override val id: String = java.util.UUID.randomUUID().toString(),
        override val name: String = "Filtros & FX",
        override val isMuted: Boolean = false,
        override val isSolo: Boolean = false,
        override val isLocked: Boolean = false,
        override val isVisible: Boolean = true,
        override val isExpanded: Boolean = true,
        override val startOffsetMs: Long = 0L,
        override val durationMs: Long = 15000L,
        val filterName: String = "Normal",
        val intensity: Float = 1.0f
    ) : CreativeTrack()
}

data class SubtitleEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val startMs: Long,
    val endMs: Long,
    val text: String
)

class TimelineEngine(
    val tracks: MutableList<CreativeTrack> = mutableListOf(),
    var totalDurationMs: Long = 15000L,
    var currentTimeMs: Long = 0L,
    var isPlaying: Boolean = false,
    var snapSensitivityMs: Long = 100L
) {
    fun addTrack(track: CreativeTrack) {
        tracks.add(track)
    }

    fun removeTrack(trackId: String) {
        tracks.removeAll { it.id == trackId }
    }

    fun seekTo(timeMs: Long) {
        currentTimeMs = timeMs.coerceIn(0L, totalDurationMs)
    }

    fun findSnapTime(timeMs: Long): Long {
        val candidates = mutableListOf(0L, totalDurationMs)
        tracks.forEach { track ->
            candidates.add(track.startOffsetMs)
            candidates.add(track.startOffsetMs + track.durationMs)
        }
        val closest = candidates.minByOrNull { Math.abs(it - timeMs) } ?: timeMs
        return if (Math.abs(closest - timeMs) <= snapSensitivityMs) closest else timeMs
    }

    fun getActiveLayers(currentTimeMs: Long, allLayers: List<CreativeLayer>): List<CreativeLayer> {
        return allLayers.filter { layer ->
            layer.isVisible && currentTimeMs >= layer.startOffsetMs && currentTimeMs <= (layer.startOffsetMs + layer.durationMs)
        }
    }
}
