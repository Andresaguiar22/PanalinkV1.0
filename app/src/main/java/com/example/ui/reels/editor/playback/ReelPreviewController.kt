package com.example.ui.reels.editor.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Owns preview playback. The Compose UI does not own the player lifecycle. */
class ReelPreviewController(context: Context) {
    private val player = ExoPlayer.Builder(context.applicationContext).build()
    private val _state = MutableStateFlow(ReelPreviewState())
    val state: StateFlow<ReelPreviewState> = _state.asStateFlow()

    fun setMedia(uri: String?) {
        if (uri.isNullOrBlank()) {
            player.clearMediaItems()
            _state.value = ReelPreviewState()
            return
        }
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        _state.value = _state.value.copy(mediaUri = uri, isReady = false)
    }

    fun play() { player.play(); _state.value = _state.value.copy(isPlaying = true) }
    fun pause() { player.pause(); _state.value = _state.value.copy(isPlaying = false) }
    fun seekTo(positionMs: Long) {
        val safe = positionMs.coerceAtLeast(0L)
        player.seekTo(safe)
        _state.value = _state.value.copy(positionMs = safe)
    }
    fun positionMs(): Long = player.currentPosition.coerceAtLeast(0L)
    fun durationMs(): Long = player.duration.takeIf { it >= 0L } ?: 0L
    fun release() = player.release()

    internal fun playerForView(): ExoPlayer = player
}

data class ReelPreviewState(
    val mediaUri: String? = null,
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L
)
