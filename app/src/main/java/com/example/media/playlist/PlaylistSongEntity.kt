package com.example.media.playlist

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * P6.7 - Playlist Song Relationship Entity
 * Junction table associating audio tracks to playlists with orderIndex.
 */
@Entity(tableName = "playlist_songs")
data class PlaylistSongEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val trackId: String,
    val orderIndex: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)
