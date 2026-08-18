package com.example.data.model.reels

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ReelCommentReactionInsertDto(

    @Json(name = "comment_id")
    val commentId: String,

    @Json(name = "user_id")
    val userId: String,

    @Json(name = "reaction")
    val reaction: String
)
