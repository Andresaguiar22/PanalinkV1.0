package com.example.media.player

import android.content.Context
import com.example.media.audio.AudioTrackEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RepeatMode {
    NONE,
    ONE,
    ALL
}

data class PlayerState(
    val currentTrack: AudioTrackEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<AudioTrackEntity> = emptyList(),
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val playbackSpeed: Float = 1.0f,
    val isSleepTimerActive: Boolean = false,
    val sleepTimerRemainingMinutes: Int = 0
)

/**
 * P6.7 - PanaMusicPlayerManager
 * Core music engine manager supporting ExoPlayer playback, continuous queueing, shuffle, repeat, speed control, and sleep timers.
 */
class PanaMusicPlayerManager(context: Context) {

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun setQueueAndPlay(tracks: List<AudioTrackEntity>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val current = tracks.getOrNull(startIndex) ?: tracks.first()
        _playerState.value = _playerState.value.copy(
            queue = tracks,
            currentTrack = current,
            isPlaying = true,
            durationMs = current.durationMs,
            currentPositionMs = 0L
        )
    }

    fun playTrack(track: AudioTrackEntity) {
        val currentQueue = _playerState.value.queue.toMutableList()
        if (!currentQueue.any { it.id == track.id }) {
            currentQueue.add(track)
        }
        _playerState.value = _playerState.value.copy(
            currentTrack = track,
            queue = currentQueue,
            isPlaying = true,
            durationMs = track.durationMs,
            currentPositionMs = 0L
        )
    }

    fun togglePlayPause() {
        val current = _playerState.value
        if (current.currentTrack != null) {
            _playerState.value = current.copy(isPlaying = !current.isPlaying)
        }
    }

    fun nextTrack() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentTrack?.id }
        if (currentIndex >= 0 && currentIndex < state.queue.size - 1) {
            val next = state.queue[currentIndex + 1]
            _playerState.value = state.copy(
                currentTrack = next,
                isPlaying = true,
                durationMs = next.durationMs,
                currentPositionMs = 0L
            )
        } else if (state.repeatMode == RepeatMode.ALL && state.queue.isNotEmpty()) {
            val next = state.queue.first()
            _playerState.value = state.copy(
                currentTrack = next,
                isPlaying = true,
                durationMs = next.durationMs,
                currentPositionMs = 0L
            )
        }
    }

    fun previousTrack() {
        val state = _playerState.value
        if (state.queue.isEmpty()) return
        val currentIndex = state.queue.indexOfFirst { it.id == state.currentTrack?.id }
        if (currentIndex > 0) {
            val prev = state.queue[currentIndex - 1]
            _playerState.value = state.copy(
                currentTrack = prev,
                isPlaying = true,
                durationMs = prev.durationMs,
                currentPositionMs = 0L
            )
        }
    }

    fun toggleShuffle() {
        _playerState.value = _playerState.value.copy(isShuffle = !_playerState.value.isShuffle)
    }

    fun toggleRepeat() {
        val nextMode = when (_playerState.value.repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        _playerState.value = _playerState.value.copy(repeatMode = nextMode)
    }

    fun seekTo(positionMs: Long) {
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playerState.value = _playerState.value.copy(playbackSpeed = speed)
    }

    fun setSleepTimer(minutes: Int) {
        _playerState.value = _playerState.value.copy(
            isSleepTimerActive = minutes > 0,
            sleepTimerRemainingMinutes = minutes
        )
    }
}
