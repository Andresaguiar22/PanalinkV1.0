package com.example.media.audio

import kotlinx.coroutines.flow.Flow

/**
 * P6.7.1 - Audio Repository
 * Repository providing clean offline-first CRUD operations for audio tracks.
 */
class AudioRepository(private val audioDao: AudioDao) {

    val allTracks: Flow<List<AudioTrackEntity>> = audioDao.getAllAudioTracks()
    val favoriteTracks: Flow<List<AudioTrackEntity>> = audioDao.getFavoriteTracks()

    suspend fun getTrackById(id: String): AudioTrackEntity? {
        return audioDao.getTrackById(id)
    }

    suspend fun getTrackByHash(hash: String): AudioTrackEntity? {
        return audioDao.getTrackByHash(hash)
    }

    suspend fun saveTrack(track: AudioTrackEntity) {
        audioDao.insertTrack(track)
    }

    suspend fun saveTracks(tracks: List<AudioTrackEntity>) {
        audioDao.insertTracks(tracks)
    }

    suspend fun updateTrackMetadata(id: String, title: String, artist: String, album: String) {
        val existing = audioDao.getTrackById(id) ?: return
        val updated = existing.copy(
            title = title,
            artist = artist,
            album = album
        )
        audioDao.updateTrack(updated)
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        audioDao.updateFavoriteState(id, isFavorite)
    }

    suspend fun deleteTrack(track: AudioTrackEntity) {
        audioDao.deleteTrack(track)
    }

    fun getTracksByPlaylist(playlistId: String): Flow<List<AudioTrackEntity>> {
        return audioDao.getTracksByPlaylist(playlistId)
    }
}
