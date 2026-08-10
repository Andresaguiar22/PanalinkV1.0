package com.example.media.playlist

import com.example.media.audio.AudioRepository
import com.example.media.audio.AudioTrackEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.util.UUID

/**
 * P6.7.2 - Playlist Manager
 * High-level business logic for playlist management.
 */
class PlaylistManager(
    private val repository: PlaylistRepository,
    private val audioRepository: AudioRepository
) {

    suspend fun createNewPlaylist(userId: String, name: String, description: String? = null): PlaylistEntity {
        val playlist = PlaylistEntity(
            id = "playlist_${UUID.randomUUID()}",
            ownerId = userId,
            name = name,
            description = description,
            isDirty = true
        )
        repository.createPlaylist(playlist)
        return playlist
    }

    suspend fun duplicatePlaylist(playlistId: String, newName: String? = null): PlaylistEntity? {
        val original = repository.getPlaylistById(playlistId) ?: return null
        val tracks = repository.getTracksForPlaylist(playlistId).first()

        val duplicated = PlaylistEntity(
            id = "playlist_${UUID.randomUUID()}",
            ownerId = original.ownerId,
            name = newName ?: "Copia de ${original.name}",
            description = original.description,
            coverPath = original.coverPath,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isDirty = true
        )
        repository.createPlaylist(duplicated)
        repository.addTracksToPlaylist(duplicated.id, tracks.map { it.id })

        return duplicated
    }

    suspend fun reorderTracks(playlistId: String, trackIds: List<String>) {
        trackIds.forEachIndexed { index, trackId ->
            repository.reorderTrack(playlistId, trackId, index)
        }
        repository.getPlaylistById(playlistId)?.let {
            repository.updatePlaylist(it.copy(isDirty = true))
        }
    }

    suspend fun getSharePayload(playlistId: String, sharedBy: String): PlaylistSharePayload? {
        val playlist = repository.getPlaylistById(playlistId) ?: return null
        val tracks = repository.getTracksForPlaylist(playlistId).first()
        
        return PlaylistSharePayload(
            playlistId = playlist.id,
            title = playlist.name,
            description = playlist.description,
            coverPath = playlist.coverPath,
            trackCount = tracks.size,
            durationMs = tracks.sumOf { it.durationMs },
            trackIds = tracks.map { it.id },
            sharedBy = sharedBy
        )
    }

    suspend fun getPlaylistDurationMs(playlistId: String): Long {
        val tracks = repository.getTracksForPlaylist(playlistId).first()
        return tracks.sumOf { it.durationMs }
    }

    /**
     * P6.7.2 Requirement: Detect physically deleted tracks and automatically resolve them.
     */
    fun getValidTracks(playlistId: String): Flow<List<AudioTrackEntity>> {
        return repository.getTracksForPlaylist(playlistId).map { tracks ->
            tracks.filter { track ->
                val file = File(track.filePath)
                file.exists() && file.length() > 0
            }
        }
    }

    suspend fun renamePlaylist(playlistId: String, newName: String) {
        val playlist = repository.getPlaylistById(playlistId) ?: return
        repository.updatePlaylist(playlist.copy(name = newName, isDirty = true))
    }

    suspend fun updateDescription(playlistId: String, description: String?) {
        val playlist = repository.getPlaylistById(playlistId) ?: return
        repository.updatePlaylist(playlist.copy(description = description, isDirty = true))
    }

    suspend fun setPlaylistPublic(playlistId: String, isPublic: Boolean) {
        val playlist = repository.getPlaylistById(playlistId) ?: return
        repository.updatePlaylist(playlist.copy(isPublic = isPublic, isDirty = true))
    }

    suspend fun deletePlaylist(playlistId: String) {
        val playlist = repository.getPlaylistById(playlistId) ?: return
        repository.deletePlaylist(playlist)
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        repository.removeTrackFromPlaylist(playlistId, trackId)
        repository.getPlaylistById(playlistId)?.let {
            repository.updatePlaylist(it.copy(isDirty = true))
        }
    }

    suspend fun updateCoverPath(playlistId: String, coverPath: String?) {
        val playlist = repository.getPlaylistById(playlistId) ?: return
        repository.updatePlaylist(playlist.copy(coverPath = coverPath, isDirty = true))
    }
}
