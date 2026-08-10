package com.example.media.attachment

import com.example.media.audio.AudioTrackEntity
import com.example.media.externalvideo.ExternalMediaObject
import com.example.media.playlist.PlaylistEntity

enum class MediaAttachmentType {
    IMAGE,
    VIDEO,
    AUDIO,
    PLAYLIST,
    EXTERNAL_VIDEO,
    DOCUMENT
}

/**
 * P6.7 - Media Attachment
 * Unified media attachment model supporting chat, feed posts, stories, and reels in PanaLink.
 */
data class MediaAttachment(
    val id: String,
    val type: MediaAttachmentType,
    val urlOrPath: String,
    val title: String? = null,
    val thumbnail: String? = null,
    val audioTrack: AudioTrackEntity? = null,
    val playlist: PlaylistEntity? = null,
    val externalVideo: ExternalMediaObject? = null
)
