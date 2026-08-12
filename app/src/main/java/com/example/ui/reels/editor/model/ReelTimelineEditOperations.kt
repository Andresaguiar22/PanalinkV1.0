package com.example.ui.reels.editor.model

/** Editing commands kept independent from Compose and Media3. */
object ReelTimelineEditOperations {
    fun splitLayer(timeline: ReelTimeline, trackId: String, layerId: String, atMs: Long): ReelTimeline {
        val track = timeline.tracks.firstOrNull { it.id == trackId } ?: return timeline
        val layer = track.layers.firstOrNull { it.id == layerId } ?: return timeline
        if (atMs <= layer.startTimeMs || atMs >= layer.endTimeMs) return timeline
        val left = layer.copy(id = "${layer.id}_a", endTimeMs = atMs)
        val right = layer.copy(id = "${layer.id}_b", startTimeMs = atMs)
        return timeline.copy(tracks = timeline.tracks.map { current ->
            if (current.id != trackId) current
            else current.copy(layers = current.layers.flatMap { if (it.id == layerId) listOf(left, right) else listOf(it) })
        })
    }

    fun reorderLayer(timeline: ReelTimeline, trackId: String, layerId: String, targetIndex: Int): ReelTimeline =
        timeline.copy(tracks = timeline.tracks.map { track ->
            if (track.id != trackId) track
            else {
                val ordered = track.layers.sortedBy { it.startTimeMs }.toMutableList()
                val from = ordered.indexOfFirst { it.id == layerId }
                if (from < 0) return@map track
                val item = ordered.removeAt(from)
                ordered.add(targetIndex.coerceIn(0, ordered.size), item)
                var cursor = 0L
                track.copy(layers = ordered.map { layer ->
                    val duration = layer.durationMs
                    layer.copy(startTimeMs = cursor, endTimeMs = cursor + duration).also { cursor += duration }
                })
            }
        })

    fun trimLayer(timeline: ReelTimeline, trackId: String, layerId: String, startMs: Long, endMs: Long): ReelTimeline {
        if (endMs <= startMs) return timeline
        return timeline.copy(tracks = timeline.tracks.map { track ->
            if (track.id != trackId) track
            else track.copy(layers = track.layers.map { layer ->
                if (layer.id != layerId) layer
                else layer.copy(startTimeMs = startMs.coerceAtLeast(0L), endTimeMs = endMs.coerceAtLeast(startMs))
            })
        })
    }
}
