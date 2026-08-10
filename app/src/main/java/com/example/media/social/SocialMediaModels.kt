package com.example.media.social

enum class MediaType {
    IMAGE,
    VIDEO,
    THUMBNAIL
}

data class SocialMediaReference(
    val mediaId: String,
    val ownerId: String,
    val type: MediaType,
    val remoteUrl: String?,
    val localPath: String?
)
