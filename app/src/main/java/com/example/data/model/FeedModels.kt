package com.example.data.model

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class PostDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "type") val type: String? = "TEXT",
    @Json(name = "content") val content: String? = null,
    @Json(name = "media_urls") val mediaUrls: List<String>? = emptyList(),
    @Json(name = "audio_url") val audioUrl: String? = null,
    @Json(name = "privacy") val privacy: String? = "PUBLIC",
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "comments_count") val commentsCount: Int = 0,
    @Json(name = "shares_count") val sharesCount: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "profiles") val profile: Profile? = null,
    @Json(name = "preview_metadata") val previewMetadata: Map<String, String>? = null, // JSON Object or Map
    @kotlin.jvm.Transient val isLikedByMe: Boolean = false,
    @Json(name = "media_ids") val customMediaIds: List<String>? = null
) {
    val mediaIds: List<String>
        get() = if (!customMediaIds.isNullOrEmpty()) customMediaIds else mediaUrls?.map { url ->
            "media_${kotlin.math.abs(url.hashCode())}"
        } ?: emptyList()
}

@Immutable
@JsonClass(generateAdapter = true)
data class PostLikeDto(
    @Json(name = "post_id") val postId: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class PostCommentDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "post_id") val postId: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "profiles") val profile: Profile? = null,
    @Json(name = "media_url") val mediaUrl: String? = null,
    @Json(name = "media_id") val customMediaId: String? = null
) {
    val mediaId: String?
        get() = customMediaId ?: mediaUrl?.let { "media_${kotlin.math.abs(it.hashCode())}" }
}


@Immutable
@JsonClass(generateAdapter = true)
data class PostShareDto(
    @Json(name = "post_id") val postId: String,
    @Json(name = "user_id") val userId: String
)
