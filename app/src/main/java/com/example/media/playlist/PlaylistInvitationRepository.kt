package com.example.media.playlist

import android.util.Log
import com.example.data.supabase.SupabaseApiService
import com.example.media.sync.MusicSocialMapper
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * P6.7.9 - Repository for playlist invitations
 */
class PlaylistInvitationRepository(
    private val invitationDao: PlaylistInvitationDao,
    private val api: SupabaseApiService,
    private val apiKey: String
) {
    private val TAG = "PlaylistInviteRepo"

    fun observeReceivedInvitations(userId: String): Flow<List<PlaylistInvitationEntity>> {
        return invitationDao.observeReceivedInvitations(userId)
    }

    fun observeInvitationsForPlaylist(playlistId: String): Flow<List<PlaylistInvitationEntity>> {
        return invitationDao.observeInvitationsForPlaylist(playlistId)
    }

    suspend fun getInvitationById(id: String): PlaylistInvitationEntity? {
        return invitationDao.getInvitationById(id)
    }

    suspend fun upsertInvitation(invitation: PlaylistInvitationEntity) {
        invitationDao.upsertInvitation(invitation)
    }

    /**
     * Create invitation locally (Offline-First)
     */
    suspend fun createInvitationLocally(
        playlistId: String,
        senderId: String,
        receiverId: String,
        role: String,
        expiresAt: Long? = null
    ): PlaylistInvitationEntity {
        val existing = invitationDao.getPendingInvitation(playlistId, receiverId)
        if (existing != null) return existing

        val invitation = PlaylistInvitationEntity(
            id = UUID.randomUUID().toString(),
            playlistId = playlistId,
            senderId = senderId,
            receiverId = receiverId,
            role = role,
            status = "PENDING",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            expiresAt = expiresAt,
            isDirty = true
        )
        invitationDao.upsertInvitation(invitation)
        return invitation
    }

    /**
     * Accept invitation (Atomic via RPC)
     */
    suspend fun acceptInvitation(invitationId: String, authHeader: String): Result<Unit> {
        return try {
            val response = api.acceptMusicPlaylistInvitation(
                apiKey,
                authHeader,
                mapOf("invitation_id" to invitationId)
            )
            if (response.isSuccessful) {
                // We don't update local status immediately to wait for Realtime, 
                // but we can for better UX
                invitationDao.updateStatus(invitationId, "ACCEPTED", System.currentTimeMillis(), isDirty = false)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to accept invitation: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reject invitation
     */
    suspend fun rejectInvitation(invitationId: String, authHeader: String): Result<Unit> {
        return try {
            val response = api.rejectMusicPlaylistInvitation(
                apiKey,
                authHeader,
                mapOf("invitation_id" to invitationId)
            )
            if (response.isSuccessful) {
                invitationDao.updateStatus(invitationId, "REJECTED", System.currentTimeMillis(), isDirty = false)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to reject invitation"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Revoke invitation
     */
    suspend fun revokeInvitation(invitationId: String, authHeader: String): Result<Unit> {
        return try {
            val response = api.revokeMusicPlaylistInvitation(
                apiKey,
                authHeader,
                mapOf("invitation_id" to invitationId)
            )
            if (response.isSuccessful) {
                invitationDao.updateStatus(invitationId, "REVOKED", System.currentTimeMillis(), isDirty = false)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to revoke invitation"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync local dirty invitations to remote
     */
    suspend fun syncLocalToRemote(authHeader: String) {
        val unsynced = invitationDao.getUnsyncedInvitations()
        for (local in unsynced) {
            try {
                if (local.status == "PENDING") {
                    val dto = MusicSocialMapper.toRemote(local)
                    val response = api.createMusicPlaylistInvitation(apiKey, authHeader, dto)
                    if (response.isSuccessful) {
                        val remote = response.body()?.firstOrNull()
                        if (remote != null) {
                            // Replace temporary local ID with remote ID if needed, 
                            // or just clear dirty if ID was already stable.
                            // Since we use UUIDs from client, it's stable.
                            invitationDao.clearDirty(local.id)
                        }
                    }
                } else {
                    // Sync status updates (REJECTED, etc.) if not using RPCs
                    val updates = mapOf("status" to local.status)
                    val response = api.updateMusicPlaylistInvitation(apiKey, authHeader, "eq.${local.id}", updates)
                    if (response.isSuccessful) {
                        invitationDao.clearDirty(local.id)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync invitation ${local.id}", e)
            }
        }
    }

    /**
     * Fetch remote invitations to local
     */
    suspend fun syncRemoteToLocal(userId: String, authHeader: String) {
        try {
            val response = api.getMusicPlaylistInvitations(apiKey, authHeader, receiverId = userId)
            if (response.isSuccessful) {
                response.body()?.forEach { remote ->
                    invitationDao.upsertInvitation(MusicSocialMapper.toLocal(remote))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch invitations", e)
        }
    }
}
