package com.example.ui.reels.editor.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.ui.reels.editor.model.ReelLayer
import com.example.ui.reels.editor.model.ReelLayerContent
import com.example.ui.reels.editor.model.ReelProject
import com.example.ui.reels.editor.model.ReelTrackType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Builds a Media3 playlist from the project's media layers without loading files into RAM. */
class ReelTimelinePlayer(context: Context) {
    private val player = ExoPlayer.Builder(context.applicationContext).build()
    private val _state = MutableStateFlow(ReelTimelinePlaybackState())
    val state: StateFlow<ReelTimelinePlaybackState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            _state.value = _state.value.copy(isReady = playbackState == Player.STATE_READY)
        }
    }

    init { player.addListener(listener) }

    fun setProject(project: ReelProject) {
        val layers = project.timeline.tracks
            .filter { it.type == ReelTrackType.VIDEO || it.type == ReelTrackType.IMAGE }
            .flatMap { it.layers }
            .filter { it.visible && it.endTimeMs > it.startTimeMs }
            .sortedWith(compareBy<ReelLayer> { it.startTimeMs }.thenBy { it.zIndex })

        val items = layers.mapNotNull { layer ->
            val media = layer.content as? ReelLayerContent.Media ?: return@mapNotNull null
            MediaItem.fromUri(media.uri)
        }

        player.setMediaItems(items, true)
        if (items.isNotEmpty()) player.prepare()
        _state.value = ReelTimelinePlaybackState(itemCount = items.size)
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun seekTo(positionMs: Long) = player.seekTo(positionMs.coerceAtLeast(0L))
    fun currentPositionMs(): Long = player.currentPosition.coerceAtLeast(0L)
    fun durationMs(): Long = player.duration.takeIf { it >= 0L } ?: 0L
    fun playerForView(): ExoPlayer = player

    fun release() {
        player.removeListener(listener)
        player.release()
    }
}

data class ReelTimelinePlaybackState(
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val itemCount: Int = 0
)
