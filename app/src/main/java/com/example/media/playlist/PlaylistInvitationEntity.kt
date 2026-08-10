package com.example.media.playlist

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * P6.7.9 - Local entity for playlist invitations
 */
@Entity(
    tableName = "playlist_invitations",
    indices = [
        Index(value = ["receiverId", "status"]),
        Index(value = ["playlistId"]),
        Index(value = ["isDirty"])
    ]
)
data class PlaylistInvitationEntity(
    @PrimaryKey val id: String, // Remote UUID
    val playlistId: String,
    val senderId: String,
    val receiverId: String,
    val role: String, // EDITOR, VIEWER
    val status: String, // PENDING, ACCEPTED, REJECTED, REVOKED, EXPIRED
    val createdAt: Long,
    val updatedAt: Long,
    val expiresAt: Long? = null,
    val isDirty: Boolean = false
)
