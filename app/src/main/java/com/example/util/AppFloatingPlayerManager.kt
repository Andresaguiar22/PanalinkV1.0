package com.example.util

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.example.data.video.CacheDataSourceFactory
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

@UnstableApi
object AppFloatingPlayerManager {
    var activeId by mutableStateOf<String?>(null)
    var activeUrl by mutableStateOf<String?>(null)
    var activeTitle by mutableStateOf<String?>(null)
    var activeType by mutableStateOf<String?>(null) // "reel" or "panatv"
    
    var exoPlayer by mutableStateOf<ExoPlayer?>(null)
    var isFloating by mutableStateOf(false)
    var isMuted by mutableStateOf(false)
    
    // Track position in screen
    var bubbleOffsetX by mutableStateOf(0f)
    var bubbleOffsetY by mutableStateOf(0f)
    
    // Native PiP State for the Activity
    var isInNativePip by mutableStateOf(false)

    fun acquirePlayer(context: Context, id: String, url: String, title: String?, type: String): ExoPlayer {
        // If we already have a player with the same video playing, reuse it!
        val currentPlayer = exoPlayer
        if (currentPlayer != null && activeUrl == url) {
            isFloating = false // Bring it out of floating mode
            activeId = id
            activeTitle = title
            activeType = type
            return currentPlayer
        }

        // Otherwise, release existing player and create a new one
        currentPlayer?.release()
        
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10000, // minBufferMs: 10s
                20000, // maxBufferMs: 20s
                200,   // bufferForPlaybackMs: 200ms for instant start
                500    // bufferForPlaybackAfterRebufferMs: 500ms
            )
            .setBackBuffer(5000, true) // Cache 5s of already played video for instant seek-back
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
            
        // Setup adaptive track selection
        val trackSelector = DefaultTrackSelector(context, androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection.Factory()).apply {
            setParameters(buildUponParameters().clearVideoSizeConstraints()) // Allow high quality
        }

        val dataSourceFactory = if (url.startsWith("http")) {
            CacheDataSourceFactory.getCacheDataSourceFactory(context)
        } else {
            androidx.media3.datasource.DefaultDataSource.Factory(context)
        }

        val player = ExoPlayer.Builder(context, DefaultRenderersFactory(context))
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(dataSourceFactory)
            )
            .setLoadControl(loadControl)
            .build()
            .apply {
                val mediaItem = if (url.startsWith("http")) {
                    MediaItem.fromUri(url)
                } else {
                    // Ensure local path is correctly formatted as file://
                    val uri = if (url.startsWith("/")) android.net.Uri.fromFile(java.io.File(url)) else android.net.Uri.parse(url)
                    MediaItem.fromUri(uri)
                }
                setMediaItem(mediaItem)
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = false // DO NOT play by default to prevent audio overlap during preloading
                volume = if (isMuted) 0f else 1f
                prepare()
            }
        
        exoPlayer = player
        activeId = id
        activeUrl = url
        activeTitle = title
        activeType = type
        isFloating = false
        
        return player
    }

    fun releasePlayer() {
        exoPlayer?.stop()
        exoPlayer?.release()
        exoPlayer = null
        activeId = null
        activeUrl = null
        activeTitle = null
        activeType = null
        isFloating = false
    }
}
