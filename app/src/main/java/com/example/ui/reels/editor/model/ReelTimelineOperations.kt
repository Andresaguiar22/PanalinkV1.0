package com.example.ui.reels.editor.model

/** Pure timeline operations. These are deliberately UI-agnostic and easy to unit test. */
object ReelTimelineOperations {
    fun addTrack(timeline: ReelTimeline, track: ReelTrack): ReelTimeline =
        timeline.copy(tracks = timeline.tracks + track)

    fun removeTrack(timeline: ReelTimeline, trackId: String): ReelTimeline =
        timeline.copy(tracks = timeline.tracks.filterNot { it.id == trackId })

    fun addLayer(timeline: ReelTimeline, trackId: String, layer: ReelLayer): ReelTimeline =
        timeline.copy(
            tracks = timeline.tracks.map { track ->
                if (track.id != trackId) track
                else track.copy(layers = track.layers + layer)
            }
        )

    fun updateLayer(
        timeline: ReelTimeline,
        trackId: String,
        layer: ReelLayer
    ): ReelTimeline = timeline.copy(
        tracks = timeline.tracks.map { track ->
            if (track.id != trackId) track
            else track.copy(
                layers = track.layers.map { existing ->
                    if (existing.id == layer.id) layer else existing
                }
            )
        }
    )

    fun removeLayer(
        timeline: ReelTimeline,
        trackId: String,
        layerId: String
    ): ReelTimeline = timeline.copy(
        tracks = timeline.tracks.map { track ->
            if (track.id != trackId) track
            else track.copy(layers = track.layers.filterNot { it.id == layerId })
        }
    )
}
