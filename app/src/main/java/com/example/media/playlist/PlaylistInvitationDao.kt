package com.example.media.playlist

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * P6.7.9 - DAO for playlist invitations
 */
@Dao
interface PlaylistInvitationDao {

    @Query("SELECT * FROM playlist_invitations WHERE receiverId = :userId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun observeReceivedInvitations(userId: String): Flow<List<PlaylistInvitationEntity>>

    @Query("SELECT * FROM playlist_invitations WHERE playlistId = :playlistId ORDER BY createdAt DESC")
    fun observeInvitationsForPlaylist(playlistId: String): Flow<List<PlaylistInvitationEntity>>

    @Query("SELECT * FROM playlist_invitations WHERE id = :id LIMIT 1")
    suspend fun getInvitationById(id: String): PlaylistInvitationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInvitation(invitation: PlaylistInvitationEntity)

    @Query("UPDATE playlist_invitations SET status = :status, updatedAt = :updatedAt, isDirty = :isDirty WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long, isDirty: Boolean)

    @Query("SELECT * FROM playlist_invitations WHERE isDirty = 1")
    suspend fun getUnsyncedInvitations(): List<PlaylistInvitationEntity>

    @Query("UPDATE playlist_invitations SET isDirty = 0 WHERE id = :id")
    suspend fun clearDirty(id: String)

    @Delete
    suspend fun deleteInvitation(invitation: PlaylistInvitationEntity)

    @Query("SELECT * FROM playlist_invitations WHERE playlistId = :playlistId AND receiverId = :receiverId AND status = 'PENDING' LIMIT 1")
    suspend fun getPendingInvitation(playlistId: String, receiverId: String): PlaylistInvitationEntity?
}
