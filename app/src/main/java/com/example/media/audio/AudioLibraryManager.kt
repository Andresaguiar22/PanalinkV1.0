package com.example.media.audio

import kotlinx.coroutines.flow.Flow

/**
 * P6.7.1 - Audio Library Manager
 * Manages local music library, chat audios, voice notes, and imported audio files with metadata editing and favorites support.
 */
class AudioLibraryManager(private val repository: AudioRepository) {

    constructor(audioDao: AudioDao) : this(AudioRepository(audioDao))

    val allTracks: Flow<List<AudioTrackEntity>> = repository.allTracks
    val favoriteTracks: Flow<List<AudioTrackEntity>> = repository.favoriteTracks

    suspend fun addTrack(track: AudioTrackEntity) {
        repository.saveTrack(track)
    }

    suspend fun getTrackById(id: String): AudioTrackEntity? {
        return repository.getTrackById(id)
    }

    suspend fun getTrackByHash(hash: String): AudioTrackEntity? {
        return repository.getTrackByHash(hash)
    }

    suspend fun editTrackMetadata(id: String, title: String, artist: String, album: String) {
        repository.updateTrackMetadata(id, title, artist, album)
    }

    suspend fun toggleFavorite(trackId: String, currentFavorite: Boolean) {
        repository.toggleFavorite(trackId, !currentFavorite)
    }

    suspend fun deleteTrack(track: AudioTrackEntity) {
        repository.deleteTrack(track)
    }

    fun getPlaylistTracks(playlistId: String): Flow<List<AudioTrackEntity>> {
        return repository.getTracksByPlaylist(playlistId)
    }
}
