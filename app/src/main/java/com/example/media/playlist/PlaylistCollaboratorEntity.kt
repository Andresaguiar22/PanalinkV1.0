package com.example.media.playlist

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * P6.7.9 - Local entity for playlist collaborators (Membership)
 */
@Entity(
    tableName = "playlist_collaborators",
    indices = [Index(value = ["playlistId", "userId"], unique = true)]
)
data class PlaylistCollaboratorEntity(
    @PrimaryKey val id: String, // Remote UUID
    val playlistId: String,
    val userId: String,
    val role: String, // OWNER, EDITOR, VIEWER
    val updatedAt: Long,
    val isDirty: Boolean = false
)
