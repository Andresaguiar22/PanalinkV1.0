package com.example.ui.components.chat.gallery

import androidx.compose.runtime.Immutable

enum class MediaType {
    IMAGE, VIDEO, DOCUMENT, AUDIO
}

@Immutable
data class MediaGalleryItem(
    val id: String,
    val messageId: String,
    val type: MediaType,
    val url: String,
    val createdAt: Long,
    val senderId: String,
    val thumbnailUrl: String?
)
