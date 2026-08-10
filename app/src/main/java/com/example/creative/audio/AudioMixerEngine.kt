package com.example.creative.audio

import com.example.creative.timeline.CreativeTrack

/**
 * P6.5B - DAW-like Audio Mixer Engine
 * Advanced multi-channel audio controls (Volume, Mute, Solo, Balance, Pitch, Speed, Noise Reduction, Fade Engine).
 */

data class AudioChannelState(
    val trackId: String,
    val name: String,
    val volume: Float = 1.0f,
    val isMuted: Boolean = false,
    val isSolo: Boolean = false,
    val fadeInMs: Long = 0L,
    val fadeOutMs: Long = 0L,
    val balance: Float = 0.0f, // -1.0 (Left) to +1.0 (Right)
    val pitch: Float = 1.0f,
    val speed: Float = 1.0f,
    val noiseReduction: Boolean = false,
    val equalizerLow: Float = 0.0f, // -12dB to +12dB
    val equalizerMid: Float = 0.0f,
    val equalizerHigh: Float = 0.0f
)

object AudioMixerEngine {

    fun computeEffectiveVolume(
        channel: AudioChannelState,
        anyChannelSolo: Boolean
    ): Float {
        if (channel.isMuted) return 0f
        if (anyChannelSolo && !channel.isSolo) return 0f
        return channel.volume.coerceIn(0f, 2.5f)
    }

    fun applyFadeEnvelope(
        currentTimeMs: Long,
        totalDurationMs: Long,
        fadeInMs: Long,
        fadeOutMs: Long,
        rawVolume: Float
    ): Float {
        if (rawVolume <= 0f) return 0f
        
        var fadeMultiplier = 1.0f

        if (fadeInMs > 0 && currentTimeMs < fadeInMs) {
            fadeMultiplier = (currentTimeMs.toFloat() / fadeInMs.toFloat()).coerceIn(0f, 1f)
        } else if (fadeOutMs > 0 && currentTimeMs > (totalDurationMs - fadeOutMs)) {
            val remainingMs = totalDurationMs - currentTimeMs
            fadeMultiplier = (remainingMs.toFloat() / fadeOutMs.toFloat()).coerceIn(0f, 1f)
        }

        return rawVolume * fadeMultiplier
    }

    fun createChannelsFromTracks(tracks: List<CreativeTrack>): List<AudioChannelState> {
        val channels = mutableListOf<AudioChannelState>()
        val anySolo = tracks.any { it.isSolo }

        tracks.forEach { track ->
            when (track) {
                is CreativeTrack.AudioTrack -> {
                    channels.add(
                        AudioChannelState(
                            trackId = track.id,
                            name = track.name,
                            volume = track.volume,
                            isMuted = track.isMuted,
                            isSolo = track.isSolo,
                            fadeInMs = track.fadeInMs,
                            fadeOutMs = track.fadeOutMs
                        )
                    )
                }
                is CreativeTrack.VoiceTrack -> {
                    channels.add(
                        AudioChannelState(
                            trackId = track.id,
                            name = track.name,
                            volume = track.volume,
                            isMuted = track.isMuted,
                            isSolo = track.isSolo,
                            noiseReduction = track.noiseReduction
                        )
                    )
                }
                is CreativeTrack.VideoTrack -> {
                    channels.add(
                        AudioChannelState(
                            trackId = track.id,
                            name = "Audio Video Original",
                            volume = track.volume,
                            isMuted = track.isMuted,
                            isSolo = track.isSolo
                        )
                    )
                }
                else -> {}
            }
        }
        return channels
    }
}
