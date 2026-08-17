package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Persistent reaction on a Reel/Story comment.
 * A user has at most one active reaction per comment in Supabase.
 */
@JsonClass(generateAdapter = true)
data class ReelCommentReaction(
    @Json(name = "comment_id") val commentId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "reaction") val reaction: String,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
)

@JsonClass(generateAdapter = true)
data class ReelCommentReactionSummary(
    @Json(name = "reaction") val reaction: String,
    @Json(name = "count") val count: Int,
    @Json(name = "reacted_by_me") val reactedByMe: Boolean = false,
)
