package com.example.media.playlist

import com.example.media.audio.AudioTrackEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

/**
 * P6.7.2 - Playlist Repository
 * Manages playlist data and track associations.
 */
class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val collaboratorDao: CollaboratorDao
) {

    fun getPlaylistsByUser(userId: String): Flow<List<PlaylistEntity>> = 
        playlistDao.getPlaylistsByUser(userId)

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = 
        playlistDao.getAllPlaylists()

    fun searchPlaylists(query: String, userId: String): Flow<List<PlaylistEntity>> = 
        playlistDao.searchPlaylists(query, userId)

    suspend fun getPlaylistById(id: String): PlaylistEntity? = 
        playlistDao.getPlaylistById(id)

    suspend fun createPlaylist(name: String, description: String, userId: String = "me") {
        val playlist = PlaylistEntity(
            id = UUID.randomUUID().toString(),
            ownerId = userId,
            name = name,
            description = description
        )
        playlistDao.insertPlaylist(playlist)
    }

    suspend fun createPlaylist(playlist: PlaylistEntity) {
        playlistDao.insertPlaylist(playlist)
    }

    suspend fun updatePlaylist(playlist: PlaylistEntity, updateTimestamp: Boolean = true) {
        val finalPlaylist = if (updateTimestamp) {
            playlist.copy(updatedAt = System.currentTimeMillis())
        } else {
            playlist
        }
        playlistDao.updatePlaylist(finalPlaylist)
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) {
        playlistDao.deletePlaylist(playlist)
    }

    fun getTracksForPlaylist(playlistId: String): Flow<List<AudioTrackEntity>> = 
        playlistDao.getTracksForPlaylist(playlistId)

    suspend fun getTracksForPlaylistSync(playlistId: String): List<AudioTrackEntity> = 
        playlistDao.getTracksForPlaylistSync(playlistId)

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) {
        val maxPos = playlistDao.getMaxPosition(playlistId) ?: -1
        val newTrack = PlaylistTrackEntity(
            id = UUID.randomUUID().toString(),
            playlistId = playlistId,
            trackId = trackId,
            position = maxPos + 1
        )
        playlistDao.insertPlaylistTrack(newTrack)
        
        // P6.7.8 Audit: Marking mother playlist as dirty for sync
        markPlaylistDirty(playlistId)
    }

    suspend fun upsertTrackWithPosition(playlistId: String, trackId: String, position: Int, markDirty: Boolean = false) {
        val existing = playlistDao.getPlaylistTrackRelation(playlistId, trackId)
        if (existing != null) {
            playlistDao.updateTrackPosition(playlistId, trackId, position)
        } else {
            val newTrack = PlaylistTrackEntity(
                id = UUID.randomUUID().toString(),
                playlistId = playlistId,
                trackId = trackId,
                position = position
            )
            playlistDao.insertPlaylistTrack(newTrack)
        }
        
        if (markDirty) markPlaylistDirty(playlistId)
    }

    suspend fun addTracksToPlaylist(playlistId: String, trackIds: List<String>) {
        val startPos = (playlistDao.getMaxPosition(playlistId) ?: -1) + 1
        val newTracks = trackIds.mapIndexed { index, trackId ->
            PlaylistTrackEntity(
                id = UUID.randomUUID().toString(),
                playlistId = playlistId,
                trackId = trackId,
                position = startPos + index
            )
        }
        playlistDao.insertPlaylistTracks(newTracks)
        
        // P6.7.8 Audit: Mark dirty
        markPlaylistDirty(playlistId)
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
        markPlaylistDirty(playlistId)
    }

    private suspend fun markPlaylistDirty(playlistId: String) {
        playlistDao.getPlaylistById(playlistId)?.let {
            playlistDao.updatePlaylist(it.copy(
                isDirty = true,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }

    suspend fun reorderTrack(playlistId: String, trackId: String, newPosition: Int) {
        playlistDao.updateTrackPosition(playlistId, trackId, newPosition)
    }

    // --- Collaboration Methods (P6.7.9) ---
    fun getCollaboratorsForPlaylist(playlistId: String): Flow<List<PlaylistCollaboratorEntity>> {
        return collaboratorDao.observeCollaborators(playlistId)
    }

    suspend fun getCollaborators(playlistId: String): List<PlaylistCollaboratorEntity> {
        return collaboratorDao.getCollaboratorsForPlaylist(playlistId)
    }

    suspend fun getCollaboratorRole(playlistId: String, userId: String): String? {
        return collaboratorDao.getCollaborator(playlistId, userId)?.role
    }

    suspend fun upsertCollaborator(collaborator: PlaylistCollaboratorEntity) {
        collaboratorDao.upsertCollaborator(collaborator)
    }

    suspend fun updateCollaboratorRole(id: String, role: String) {
        collaboratorDao.updateRole(id, role, System.currentTimeMillis(), isDirty = true)
    }

    suspend fun removeCollaborator(id: String) {
        collaboratorDao.getCollaboratorById(id)?.let {
            collaboratorDao.deleteCollaborator(it)
        }
    }

    suspend fun removeCollaborator(collaborator: PlaylistCollaboratorEntity) {
        collaboratorDao.deleteCollaborator(collaborator)
    }

    suspend fun getUnsyncedCollaborators(): List<PlaylistCollaboratorEntity> {
        return collaboratorDao.getUnsyncedCollaborators()
    }

    suspend fun markCollaboratorSynced(id: String) {
        collaboratorDao.clearDirty(id)
    }

    suspend fun getTrackCount(playlistId: String): Int = 
        playlistDao.getTrackCount(playlistId)

    suspend fun getUnsyncedPlaylistsSync(): List<PlaylistEntity> = 
        playlistDao.getUnsyncedPlaylists()

    suspend fun getUnsyncedTracksSync(): List<PlaylistTrackEntity> = 
        playlistDao.getUnsyncedTracks()
}
