package com.example.core.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

@OptIn(UnstableApi::class)
object ExoPlayerManager {
    private const val MAX_POOL_SIZE = 3
    private val playerPool = mutableListOf<ExoPlayer>()
    private val activePlayers = mutableSetOf<ExoPlayer>()

    @Synchronized
    fun getPlayer(context: Context): ExoPlayer {
        val appCtx = context.applicationContext
        val player = if (playerPool.isNotEmpty()) {
            playerPool.removeAt(0)
        } else {
            createExoPlayer(appCtx)
        }
        activePlayers.add(player)
        return player
    }

    @Synchronized
    fun releasePlayer(player: ExoPlayer?) {
        if (player == null) return
        player.stop()
        player.clearMediaItems()
        activePlayers.remove(player)

        if (playerPool.size < MAX_POOL_SIZE) {
            playerPool.add(player)
        } else {
            player.release()
        }
    }

    @Synchronized
    fun releaseAll() {
        activePlayers.forEach { it.release() }
        activePlayers.clear()
        playerPool.forEach { it.release() }
        playerPool.clear()
    }

    private fun createExoPlayer(context: Context): ExoPlayer {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                5000,   // minBufferMs
                15000,  // maxBufferMs
                500,    // bufferForPlaybackMs
                3000    // bufferForPlaybackAfterRebufferMs
            )
            .build()

        val mediaSourceFactory = DefaultMediaSourceFactory(
            com.example.data.video.CacheDataSourceFactory.getCacheDataSourceFactory(context)
        )

        return ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ONE
            }
    }
}
