package com.example.media.playlist

import com.example.media.audio.AudioTrackEntity

/**
 * P6.7.2 - Playlist UI State
 * Representa el estado de una playlist en la interfaz.
 */
data class PlaylistState(
    val playlist: PlaylistEntity? = null,
    val tracks: List<AudioTrackEntity> = emptyList(),
    val collaborators: List<PlaylistCollaboratorEntity> = emptyList(),
    val invitations: List<PlaylistInvitationEntity> = emptyList(),
    val userRole: PlaylistMemberRole = PlaylistMemberRole.VIEWER,
    val isLoading: Boolean = false,
    val totalDurationMs: Long = 0,
    val error: String? = null
)

/**
 * P6.7.2 - Playlist Summary State
 * Para listas de reproducción en el dashboard.
 */
data class PlaylistSummaryState(
    val id: String,
    val name: String,
    val trackCount: Int,
    val coverPath: String?
)
