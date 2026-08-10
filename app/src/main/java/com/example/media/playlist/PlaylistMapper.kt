package com.example.media.playlist

import com.example.media.audio.AudioTrackEntity

/**
 * P6.7.2 - Playlist Mapper
 * Transforma entidades de datos en modelos de UI o estados.
 */
object PlaylistMapper {

    fun toSummary(playlist: PlaylistEntity, trackCount: Int): PlaylistSummaryState {
        return PlaylistSummaryState(
            id = playlist.id,
            name = playlist.name,
            trackCount = trackCount,
            coverPath = playlist.coverPath
        )
    }

    /**
     * Calcula el estado completo de una playlist.
     */
    fun toFullState(
        playlist: PlaylistEntity,
        tracks: List<AudioTrackEntity>,
        isLoading: Boolean = false
    ): PlaylistState {
        return PlaylistState(
            playlist = playlist,
            tracks = tracks,
            isLoading = isLoading,
            totalDurationMs = tracks.sumOf { it.durationMs }
        )
    }
}
