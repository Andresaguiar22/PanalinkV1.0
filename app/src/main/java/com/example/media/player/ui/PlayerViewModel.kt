package com.example.media.player.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.media.audio.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * P6.7.3 - Player View Model
 * Centrally manages audio playback state for the professional player UI.
 */
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val playerEngine = AudioPlayerProvider.getPlayerEngine(application)
    private val effectsController = AudioPlayerProvider.getEffectsController()
    private val audioLibraryManager: AudioLibraryManager? by lazy {
        try {
            val db = com.example.data.database.PanalinkDatabase.getDatabase(application)
            AudioLibraryManager(db.audioDao())
        } catch (_: Exception) { null }
    }
    
    val playerState: StateFlow<AudioPlayerState> = playerEngine.state

    fun playTrack(track: AudioTrackEntity) {
        playerEngine.setQueueAndPlay(listOf(track), 0)
    }

    fun playTracks(tracks: List<AudioTrackEntity>, startIndex: Int = 0) {
        playerEngine.setQueueAndPlay(tracks, startIndex)
    }

    fun clearQueue() {
        playerEngine.setQueueAndPlay(emptyList(), 0)
    }

    fun toggleFavorite(track: AudioTrackEntity) {
        viewModelScope.launch {
            audioLibraryManager?.toggleFavorite(track.id, track.isFavorite)
        }
    }

    fun togglePlayPause() {
        playerEngine.togglePlayPause()
    }

    fun nextTrack() {
        playerEngine.nextTrack()
    }

    fun previousTrack() {
        playerEngine.previousTrack()
    }

    fun seekTo(positionMs: Long) {
        playerEngine.seekTo(positionMs)
    }

    fun toggleShuffle() {
        playerEngine.toggleShuffle()
    }

    fun toggleRepeat() {
        playerEngine.toggleRepeat()
    }

    fun setPlaybackSpeed(speed: Float) {
        playerEngine.setPlaybackSpeed(speed)
    }

    fun removeFromQueue(trackId: String) {
        // Implementation logic depends on engine supporting queue modification
        // For now, we'll need to update the engine to support this if it doesn't
    }

    fun playFromQueue(index: Int) {
        val tracks = playerState.value.queue
        if (index in tracks.indices) {
            playerEngine.setQueueAndPlay(tracks, index)
        }
    }
    
    fun setBassBoost(level: Int) {
        effectsController?.setBassBoost(level.toShort())
    }
    
    fun setBalance(balance: Float) {
        effectsController?.setBalance(balance)
    }
}
