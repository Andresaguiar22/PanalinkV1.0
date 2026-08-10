package com.example.notification.engine.model

import androidx.annotation.Keep

/**
 * Enriched media attachment associated with a notification event.
 * Enables rich previews (e.g. post thumbnails, video keyframes, avatars, voice note waveforms).
 */
@Keep
data class NotificationAttachment(
    val type: AttachmentType,
    val url: String? = null,
    val localPath: String? = null,
    val mimeType: String? = null,
    val title: String? = null,
    val description: String? = null,
    val metadata: Map<String, String>? = emptyMap()
) {
    @Keep
    enum class AttachmentType {
        IMAGE,
        VIDEO,
        AUDIO,
        STICKER,
        GIF,
        THUMBNAIL,
        AVATAR,
        PREVIEW,
        DOCUMENT,
        LOCATION,
        CONTACT
    }
}
