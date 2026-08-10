package com.example.data.model

import kotlinx.serialization.Serializable

/**
 * P6.7.6B - Supabase Remote DTOs for Music Social Metadata
 */

@Serializable
data class RemoteMusicPlaylist(
    val id: String? = null, // UUID in Supabase
    val owner_id: String,
    val title: String,
    val description: String? = null,
    val cover_cdn_url: String? = null,
    val cover_hash: String? = null,
    val privacy: String = "PRIVATE", // "PUBLIC", "PRIVATE", "CONTACTS"
    val is_collaborative: Boolean = false,
    val total_duration_ms: Long = 0,
    val track_count: Int = 0,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class RemoteMusicPlaylistTrack(
    val id: String? = null,
    val playlist_id: String,
    val media_hash: String,
    val cdn_url: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val cover_cdn_url: String? = null,
    val duration_ms: Long,
    val mime_type: String,
    val order_index: Int,
    val created_at: String? = null
)

@Serializable
data class RemoteMusicPlaylistCollaborator(
    val id: String? = null,
    val playlist_id: String,
    val user_id: String,
    val role: String, // "OWNER", "EDITOR", "VIEWER"
    val created_at: String? = null
)

@Serializable
data class RemoteMusicPlaylistInvitation(
    val id: String? = null,
    val playlist_id: String,
    val sender_id: String,
    val receiver_id: String,
    val role: String, // EDITOR, VIEWER
    val status: String, // PENDING, ACCEPTED, REJECTED, REVOKED, EXPIRED
    val created_at: String? = null,
    val updated_at: String? = null,
    val expires_at: String? = null
)

@Serializable
data class RemoteMusicPlaylistShare(
    val id: String? = null,
    val playlist_id: String,
    val shared_by: String,
    val shared_to_chat_id: String? = null,
    val shared_to_user_id: String? = null,
    val created_at: String? = null
)
