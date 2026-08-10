package com.example.media.playlist

import kotlinx.serialization.Serializable

/**
 * P6.7.5 - Playlist Share Payload
 * Represents a secure abstraction of a playlist for internal sharing in PanaLink.
 */
@Serializable
data class PlaylistSharePayload(
    val playlistId: String,
    val title: String,
    val description: String?,
    val coverPath: String?,
    val trackCount: Int,
    val durationMs: Long,
    val trackIds: List<String>,
    val sharedBy: String,
    val sharedAt: Long = System.currentTimeMillis()
)
