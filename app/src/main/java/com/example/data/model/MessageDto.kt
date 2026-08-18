package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageDto(
    @Json(name = "id") val id: String,
    @Json(name = "thread_id") val threadId: String?,
    @Json(name = "chat_id") val chatId: String?,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "receiver_id") val receiverId: String?,
    @Json(name = "reply_to") val replyTo: String?,
    @Json(name = "text_content") val textContent: String?,
    @Json(name = "client_message_uuid") val clientMessageUuid: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "media_url") val mediaUrl: String?,
    @Json(name = "thumbnail_url") val thumbnailUrl: String?,
    @Json(name = "media_mime") val mediaMime: String?,
    @Json(name = "message_type") val messageType: String,
    @Json(name = "file_size") val fileSize: Long?,
    @Json(name = "duration") val duration: Long?,
    @Json(name = "width") val width: Int?,
    @Json(name = "height") val height: Int?,
    @Json(name = "is_ghost") val isGhost: Boolean? = null
)
