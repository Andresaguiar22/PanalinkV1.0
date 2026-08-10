package com.example.media.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * P6.7.3 - Audio Effects Controller
 * Abstraction for audio enhancements (Bass, Treble, EQ).
 */
class AudioEffectsController(private val exoPlayer: ExoPlayer?) {
    fun setBassBoost(level: Short) { /* Preparado para futura integración con BassBoost */ }
    fun setEqualizer(preset: String) { /* Preparado para presets */ }
    fun setBalance(balance: Float) { exoPlayer?.setVolume(balance) }
}

enum class RepeatMode { NONE, ONE, ALL }

data class AudioPlayerState(
    val currentTrack: AudioTrackEntity? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val queue: List<AudioTrackEntity> = emptyList(),
    val currentIndex: Int = -1,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val playbackSpeed: Float = 1.0f
)

/**
 * P6.7.1 - Audio Player Engine
 * Professional ExoPlayer & MediaSession engine for background playback, queue, shuffle, repeat, and StateFlow updates.
 */
class AudioPlayerEngine(private val context: Context) {

    private val _state = MutableStateFlow(AudioPlayerState())
    val state: StateFlow<AudioPlayerState> = _state.asStateFlow()

    private var exoPlayer: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    init {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build()

            val player = ExoPlayer.Builder(context)
                .setAudioAttributes(audioAttributes, true) // Managed by system
                .setHandleAudioBecomingNoisy(true)
                .build()
            
            exoPlayer = player

            try {
                mediaSession = MediaSession.Builder(context, player).build()
            } catch (_: Exception) {
                // MediaSession fallback for testing environments
            }

            player.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(isPlaying = isPlaying)
                    if (isPlaying) {
                        startProgressTicker()
                    } else {
                        stopProgressTicker()
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val currentIdx = player.currentMediaItemIndex
                    val currentTrack = _state.value.queue.getOrNull(currentIdx)
                    _state.value = _state.value.copy(
                        currentTrack = currentTrack,
                        currentIndex = currentIdx,
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        handlePlaybackEnded()
                    }
                    _state.value = _state.value.copy(
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
            })
        } catch (_: Exception) {
            // Safe fallback for non-GUI unit tests
        }
    }

    fun getExoPlayer(): ExoPlayer? = exoPlayer

    fun setQueueAndPlay(tracks: List<AudioTrackEntity>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val validIndex = startIndex.coerceIn(0, tracks.lastIndex)
        val selectedTrack = tracks[validIndex]

        _state.value = _state.value.copy(
            queue = tracks,
            currentIndex = validIndex,
            currentTrack = selectedTrack,
            isPlaying = true
        )

        exoPlayer?.let { player ->
            player.stop()
            player.clearMediaItems()
            val mediaItems = tracks.map { track ->
                MediaItem.Builder()
                    .setUri(Uri.fromFile(File(track.filePath)))
                    .setMediaId(track.id)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(track.title)
                            .setArtist(track.artist)
                            .setAlbumTitle(track.album)
                            .build()
                    )
                    .build()
            }
            player.setMediaItems(mediaItems, validIndex, 0L)
            player.prepare()
            player.playWhenReady = true
        }
    }

    fun playTrack(track: AudioTrackEntity) {
        setQueueAndPlay(listOf(track), 0)
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                if (player.playbackState == Player.STATE_ENDED) {
                    player.seekTo(0)
                }
                player.play()
            }
        } ?: run {
            val playing = !_state.value.isPlaying
            _state.value = _state.value.copy(isPlaying = playing)
        }
    }

    fun pause() {
        exoPlayer?.pause()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _state.value = _state.value.copy(currentPositionMs = positionMs)
    }

    fun nextTrack() {
        val currentState = _state.value
        if (currentState.queue.isEmpty()) return

        var nextIndex = currentState.currentIndex + 1
        if (nextIndex >= currentState.queue.size) {
            nextIndex = if (currentState.repeatMode == RepeatMode.ALL) 0 else currentState.currentIndex
        }

        if (nextIndex in currentState.queue.indices) {
            val track = currentState.queue[nextIndex]
            _state.value = currentState.copy(
                currentIndex = nextIndex,
                currentTrack = track
            )
            exoPlayer?.seekToDefaultPosition(nextIndex)
        }
    }

    fun previousTrack() {
        val currentState = _state.value
        if (currentState.queue.isEmpty()) return

        var prevIndex = currentState.currentIndex - 1
        if (prevIndex < 0) {
            prevIndex = if (currentState.repeatMode == RepeatMode.ALL) currentState.queue.lastIndex else 0
        }

        if (prevIndex in currentState.queue.indices) {
            val track = currentState.queue[prevIndex]
            _state.value = currentState.copy(
                currentIndex = prevIndex,
                currentTrack = track
            )
            exoPlayer?.seekToDefaultPosition(prevIndex)
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_state.value.isShuffle
        _state.value = _state.value.copy(isShuffle = newShuffle)
        exoPlayer?.shuffleModeEnabled = newShuffle
    }

    fun toggleRepeat() {
        val nextRepeat = when (_state.value.repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        _state.value = _state.value.copy(repeatMode = nextRepeat)
        exoPlayer?.repeatMode = when (nextRepeat) {
            RepeatMode.NONE -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        _state.value = _state.value.copy(playbackSpeed = speed)
        exoPlayer?.setPlaybackSpeed(speed)
    }

    private fun handlePlaybackEnded() {
        val currentState = _state.value
        when (currentState.repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
            }
            RepeatMode.ALL -> {
                nextTrack()
            }
            RepeatMode.NONE -> {
                if (currentState.currentIndex < currentState.queue.lastIndex) {
                    nextTrack()
                } else {
                    _state.value = currentState.copy(isPlaying = false)
                }
            }
        }
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = scope.launch {
            while (isActive) {
                exoPlayer?.let { player ->
                    _state.value = _state.value.copy(
                        currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                        durationMs = player.duration.coerceAtLeast(0L)
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTicker()
        try {
            mediaSession?.release()
            exoPlayer?.release()
        } catch (_: Exception) {}
        exoPlayer = null
        mediaSession = null
        scope.cancel()
    }
}
