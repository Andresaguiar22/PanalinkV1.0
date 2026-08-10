package com.example.media.playlist

import com.example.media.audio.AudioPlayerEngine
import com.example.media.audio.AudioTrackEntity
import kotlinx.coroutines.flow.first

/**
 * P6.7.2 - Playlist Playback Builder
 * Converts a playlist and its tracks into a queue compatible with AudioPlayerEngine.
 */
object PlaylistPlaybackBuilder {

    /**
     * Prepares the playback queue from a playlist's tracks.
     * Respects the exact order established by 'position'.
     */
    suspend fun playPlaylist(
        playlistId: String,
        playlistManager: PlaylistManager,
        playerEngine: AudioPlayerEngine,
        startTrackId: String? = null
    ) {
        val tracks = playlistManager.getValidTracks(playlistId).first()
        if (tracks.isEmpty()) return

        val startIndex = if (startTrackId != null) {
            val index = tracks.indexOfFirst { it.id == startTrackId }
            if (index != -1) index else 0
        } else {
            0
        }

        playerEngine.setQueueAndPlay(tracks, startIndex)
    }

    /**
     * Prepares a list of tracks for immediate playback.
     */
    fun playTracks(
        tracks: List<AudioTrackEntity>,
        playerEngine: AudioPlayerEngine,
        startIndex: Int = 0
    ) {
        playerEngine.setQueueAndPlay(tracks, startIndex)
    }
}
