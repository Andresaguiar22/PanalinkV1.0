package com.example.creative.reels

/** Pure state controller for background audio editing. */
class ReelAudioController(initialTrack: ReelAudioTrack? = null) {
    var track: ReelAudioTrack? = initialTrack
        private set

    fun setTrack(track: ReelAudioTrack?) {
        this.track = track
    }

    fun trim(startMs: Long, endMs: Long) {
        track = track?.withTrim(startMs, endMs)
    }

    fun setVolume(volume: Float) {
        track = track?.copy(volume = volume.coerceIn(0f, 1f))
    }

    fun setMuted(muted: Boolean) {
        track = track?.copy(muted = muted)
    }

    fun clear() {
        track = null
    }
}
