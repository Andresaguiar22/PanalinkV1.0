package com.example.media.feed

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.core.media.ExoPlayerManager

@OptIn(UnstableApi::class)
class ReelsPreloadManager(
    private val context: Context
) {
    private var preloadPlayer: ExoPlayer? = null
    private var preloadedUrl: String? = null

    fun preloadNext(url: String) {
        if (url.isBlank()) return
        if (preloadedUrl == url) {
            return
        }

        releasePreload()

        val player = ExoPlayerManager.getPlayer(context)
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()

        // Importante: preparar pero no reproducir
        player.playWhenReady = false

        preloadPlayer = player
        preloadedUrl = url
    }

    fun consumePreloaded(url: String?): ExoPlayer? {
        if (url == null || preloadedUrl != url) {
            return null
        }
        val player = preloadPlayer
        preloadPlayer = null
        preloadedUrl = null
        return player
    }

    fun releasePreload() {
        preloadPlayer?.let {
            ExoPlayerManager.releasePlayer(it)
        }
        preloadPlayer = null
        preloadedUrl = null
    }
}
