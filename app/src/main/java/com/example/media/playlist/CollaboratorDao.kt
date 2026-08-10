package com.example.media.playlist

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * P6.7.9 - DAO for playlist collaborators
 */
@Dao
interface CollaboratorDao {

    @Query("SELECT * FROM playlist_collaborators WHERE playlistId = :playlistId")
    fun getCollaboratorsForPlaylist(playlistId: String): List<PlaylistCollaboratorEntity>

    @Query("SELECT * FROM playlist_collaborators WHERE playlistId = :playlistId")
    fun observeCollaborators(playlistId: String): Flow<List<PlaylistCollaboratorEntity>>

    @Query("SELECT * FROM playlist_collaborators WHERE id = :id LIMIT 1")
    suspend fun getCollaboratorById(id: String): PlaylistCollaboratorEntity?

    @Query("SELECT * FROM playlist_collaborators WHERE playlistId = :playlistId AND userId = :userId LIMIT 1")
    suspend fun getCollaborator(playlistId: String, userId: String): PlaylistCollaboratorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCollaborator(collaborator: PlaylistCollaboratorEntity)

    @Query("UPDATE playlist_collaborators SET role = :newRole, updatedAt = :updatedAt, isDirty = :isDirty WHERE id = :id")
    suspend fun updateRole(id: String, newRole: String, updatedAt: Long, isDirty: Boolean)

    @Delete
    suspend fun deleteCollaborator(collaborator: PlaylistCollaboratorEntity)

    @Query("SELECT * FROM playlist_collaborators WHERE isDirty = 1")
    suspend fun getUnsyncedCollaborators(): List<PlaylistCollaboratorEntity>

    @Query("UPDATE playlist_collaborators SET isDirty = 0 WHERE id = :id")
    suspend fun clearDirty(id: String)
}
