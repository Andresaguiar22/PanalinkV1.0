package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// DOMAIN MODELS

data class Like(
    val stateId: String,
    val userId: String
)

data class Comment(
    val id: String,
    val stateId: String,
    val userId: String,
    val text: String,
    val createdAt: String,
    val authorName: String,
    val avatarUrl: String?,
    val parentCommentId: String? = null,
    val deletedAt: String? = null
)

data class StatusViewer(
    val viewerId: String,
    val name: String,
    val avatarUrl: String?,
    val viewedAt: String
)

// DATA LAYER DTOs

@JsonClass(generateAdapter = true)
data class LikeDto(
    @Json(name = "reel_id") val reelId: String? = null,
    @Json(name = "story_id") val storyId: String? = null,
    @Json(name = "state_id") val stateIdField: String? = null,
    @Json(name = "status_id") val statusId: String? = null,
    @Json(name = "user_id") val userIdField: String? = null,
    @Json(name = "author_id") val authorId: String? = null
) {
    val stateId: String get() = reelId ?: storyId ?: statusId ?: stateIdField ?: ""
    val userId: String get() = authorId ?: userIdField ?: ""

    fun toDomain() = Like(
        stateId = stateId,
        userId = userId
    )
}

@JsonClass(generateAdapter = true)
data class StateCommentDto(
    @Json(name = "id") val id: String,
    @Json(name = "reel_id") val reelId: String? = null,
    @Json(name = "story_id") val storyId: String? = null,
    @Json(name = "state_id") val stateIdField: String? = null,
    @Json(name = "status_id") val statusId: String? = null,
    @Json(name = "user_id") val userIdField: String? = null,
    @Json(name = "author_id") val authorId: String? = null,
    @Json(name = "body") val body: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "comment_text") val commentText: String? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "parent_comment_id") val parentCommentId: String? = null,
    @Json(name = "profiles") val profiles: Profile? = null,
    @Json(name = "deleted_at") val deletedAt: String? = null
) {
    fun toDomain(): Comment {
        val commentText = body ?: text ?: content ?: commentText ?: ""
        return Comment(
            id = id,
            stateId = reelId ?: storyId ?: statusId ?: stateIdField ?: "",
            userId = authorId ?: userIdField ?: "",
            text = commentText,
            createdAt = createdAt,
            authorName = profiles?.displayName ?: "Pana",
            avatarUrl = profiles?.avatarUrl,
            parentCommentId = parentCommentId,
            deletedAt = deletedAt
        )
    }
}

@JsonClass(generateAdapter = true)
data class StatusViewDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "reel_id") val reelId: String? = null,
    @Json(name = "story_id") val storyId: String? = null,
    @Json(name = "status_id") val statusId: String? = null,
    @Json(name = "viewer_id") val viewerId: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "profiles") val profiles: Profile? = null
) {
    fun toDomain() = StatusViewer(
        viewerId = viewerId,
        name = profiles?.displayName ?: "Pana",
        avatarUrl = profiles?.avatarUrl,
        viewedAt = createdAt
    )
}

@JsonClass(generateAdapter = true)
data class FollowerDto(
    @Json(name = "follower_id") val followerId: String,
    @Json(name = "followed_id") val followedId: String,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class ToggleLikeResponseDto(
    @Json(name = "liked") val liked: Boolean,
    @Json(name = "likes_count") val likesCount: Int
)

@JsonClass(generateAdapter = true)
data class ToggleFavoriteResponseDto(
    @Json(name = "favorited") val favorited: Boolean,
    @Json(name = "favorites_count") val favoritesCount: Int
)
