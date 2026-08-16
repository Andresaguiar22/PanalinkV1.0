package com.example.data.model

import androidx.compose.runtime.Immutable
import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.ToJson

@Immutable
data class Profile(
    @Json(name = "id") val id: String,
    @Json(name = "display_name") val displayName: String,
    @Json(name = "avatar_url") val avatarUrl: String?,
    @Json(name = "pin_hash") val pinHash: String? = null,
    @Json(name = "pin") val pin: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "profile_theme") val profileTheme: String? = "dark_teal",
    @Json(name = "profile_badges") val profileBadges: List<String>? = emptyList(),
    @Json(name = "last_profile_edit") val lastProfileEdit: String? = null,
    @Json(name = "device_fingerprint") val deviceFingerprint: String? = null,
    @Json(name = "public_key") val publicKey: String? = null,
    @Json(name = "is_profile_complete") val isProfileComplete: Boolean = false,
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "status") val status: String? = "active",
    @Json(name = "birth_date") val birthDate: String? = null,
    @Json(name = "sex") val sex: String? = null,
    @Json(name = "interests") val interests: List<String>? = emptyList(),
    @Json(name = "qr_payload") val qrPayload: String? = null,
    @Json(name = "cover_url") val coverUrl: String? = null,
    @Json(name = "profile_edit_count") val profileEditCount: Int? = 0,
    @Json(name = "pin_updated_at") val pinUpdatedAt: String? = null
) {
    val activePublicKey: String? get() = publicKey
}

@Immutable
@JsonClass(generateAdapter = true)
data class Chat(
    @Json(name = "id") val id: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "chat_type") val type: String = "dm",
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "cover_url") val coverUrl: String? = null,
    @Json(name = "visibility") val visibility: String = "private",
    @Json(name = "is_readonly") val isReadonly: Boolean = false,
    @Json(name = "owner_id") val ownerId: String? = null,
    @Json(name = "last_message_at") val lastMessageAt: String? = null,
    @Json(name = "is_archived") val isArchived: Boolean = false,
    @Json(name = "is_muted") val isMuted: Boolean = false,
    @Json(name = "is_pinned") val isPinned: Boolean = false,
    @Json(name = "pinned_at") val pinnedAt: String? = null,
    @Json(name = "thread_id") val threadId: String? = null
)

@JsonClass(generateAdapter = true)
data class OneToOneThread(
    @Json(name = "id") val id: String = "",
    @Json(name = "user_a") val userA: String,
    @Json(name = "user_b") val userB: String,
    @Json(name = "created_at") val createdAt: String? = null
) {
    fun toChat(isMuted: Boolean = false, isPinned: Boolean = false, pinnedAt: String? = null): Chat =
        Chat(id = id, createdAt = createdAt, type = "dm", isMuted = isMuted, isPinned = isPinned, pinnedAt = pinnedAt, threadId = id)
}

@JsonClass(generateAdapter = true)
data class ChatMember(
    @Json(name = "chat_id") val chatId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "role") val role: String = "member",
    @Json(name = "joined_at") val joinedAt: String? = null,
    @Json(name = "last_cleared_at") val lastClearedAt: String? = null,
    @Json(name = "is_hidden") val isHidden: Boolean = false,
    @Json(name = "is_muted") val isMuted: Boolean = false,
    @Json(name = "is_pinned") val isPinned: Boolean = false,
    @Json(name = "pinned_at") val pinnedAt: String? = null
)

@Immutable
@JsonClass(generateAdapter = true)
data class Message(
    @Json(name = "id") val id: String,
    @Json(name = "chat_id") val chatId: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "receiver_id") val receiverId: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "status") val status: String? = "sent",
    @Json(name = "reply_to_message_id") val replyToMessageId: String? = null,
    @Json(name = "client_message_uuid") val clientMessageUuid: String = "",
    @Json(name = "delivered_at") val deliveredAt: String? = null,
    @Json(name = "seen_at") val seenAt: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "media_url") val mediaUrl: String? = null,
    @Json(name = "media_mime") val mediaMime: String? = null,
    @Json(name = "media_size") val mediaSize: Long? = null,
    @Json(name = "media_duration") val duration: Long? = null,
    @Json(name = "media_width") val width: Int? = null,
    @Json(name = "media_height") val height: Int? = null,
    @Json(name = "message_type") val messageType: String? = "text",
    @Json(name = "is_favorited") val isFavorited: Boolean = false,
    @Json(name = "is_edited") val isEdited: Boolean = false,
    @Json(name = "deleted_at") val deletedAt: String? = null,
    @Json(name = "allow_comments") val allowComments: Boolean = true,
    @Json(name = "is_ghost") val isGhost: Boolean = false,
    @Json(name = "ghost_opened_at") val ghostOpenedAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "music_playlist_id") val musicPlaylistId: String? = null
) {
    val textContent: String get() = content ?: ""
}

data class UploadMediaResult(
    val url: String,
    val thumbnailUrl: String? = null,
    val mime: String? = null,
    val size: Long? = null,
    val duration: Long? = null,
    val width: Int? = null,
    val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class ContactEntity(@Json(name = "id") val id: String = "", @Json(name = "owner_user_id") val ownerUserId: String, @Json(name = "contact_user_id") val contactUserId: String, @Json(name = "created_at") val createdAt: String? = null)
@JsonClass(generateAdapter = true)
data class ContactWithProfileEntity(@Json(name = "id") val id: String = "", @Json(name = "owner_user_id") val ownerUserId: String, @Json(name = "contact_user_id") val contactUserId: String, @Json(name = "created_at") val createdAt: String? = null, @Json(name = "profiles") val profiles: EmbeddedProfile? = null) {
    val rawProfile: Profile? get() = profiles?.profile
    fun getProfile(moshi: com.squareup.moshi.Moshi): Profile? = rawProfile
}
data class EmbeddedProfile(val profile: Profile?)
class EmbeddedProfileAdapter {
    @FromJson fun fromJson(reader: com.squareup.moshi.JsonReader, delegate: com.squareup.moshi.JsonAdapter<Profile>): EmbeddedProfile? = try {
        when (reader.peek()) {
            com.squareup.moshi.JsonReader.Token.BEGIN_ARRAY -> { reader.beginArray(); var prof: Profile? = null; if (reader.hasNext() && reader.peek() != com.squareup.moshi.JsonReader.Token.END_ARRAY) prof = delegate.fromJson(reader); while (reader.hasNext()) reader.skipValue(); reader.endArray(); EmbeddedProfile(prof) }
            com.squareup.moshi.JsonReader.Token.BEGIN_OBJECT -> EmbeddedProfile(delegate.fromJson(reader))
            com.squareup.moshi.JsonReader.Token.NULL -> { reader.nextNull<Unit>(); null }
            else -> { reader.skipValue(); null }
        }
    } catch (e: Exception) { android.util.Log.e("EmbeddedProfileAdapter", "Deserialization error in embedded profile JSON", e); null }
    @ToJson fun toJson(writer: com.squareup.moshi.JsonWriter, value: EmbeddedProfile?, delegate: com.squareup.moshi.JsonAdapter<Profile>) { if (value?.profile == null) writer.nullValue() else delegate.toJson(writer, value.profile) }
}

@JsonClass(generateAdapter = true)
data class FriendRequestEntity(@Json(name = "id") val id: String = "", @Json(name = "sender_id") val senderId: String, @Json(name = "receiver_id") val receiverId: String, @Json(name = "status") val status: String, @Json(name = "created_at") val createdAt: String? = null, @Json(name = "sender") val sender: Profile? = null)

@JsonClass(generateAdapter = true)
data class ThreadMessage(
    @Json(name = "id") val id: String = "",
    @Json(name = "thread_id") val threadId: String? = null,
    @Json(name = "chat_id") val chatId: String? = null,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "receiver_id") val receiverId: String? = null,
    @Json(name = "message_type") val messageType: String? = null,
    @Json(name = "text_content") val textContent: String? = null,
    @Json(name = "media_url") val mediaUrl: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "media_mime") val mediaMime: String? = null,
    @Json(name = "file_size") val mediaSize: Long? = null,
    @Json(name = "duration") val mediaDuration: Long? = null,
    @Json(name = "width") val mediaWidth: Int? = null,
    @Json(name = "height") val mediaHeight: Int? = null,
    @Json(name = "client_message_uuid") val clientMessageUuid: String = "",
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "status") val status: String? = "sent",
    @Json(name = "delivered_at") val deliveredAt: String? = null,
    @Json(name = "seen_at") val seenAt: String? = null,
    @Json(name = "reply_to") val replyTo: String? = null,
    @Json(name = "deleted_at") val deletedAt: String? = null,
    @Json(name = "edited_at") val editedAt: String? = null,
    @Json(name = "is_ghost") val isGhost: Boolean? = false,
    @Json(name = "ghost_opened_at") val ghostOpenedAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "music_playlist_id") val musicPlaylistId: String? = null
) {
    fun toMessage(): Message {
        val calculatedStatus = when {
            seenAt != null -> "seen"
            deliveredAt != null -> "delivered"
            else -> status ?: "sent"
        }
        val displayContent = if (messageType == "sticker" && !mediaUrl.isNullOrEmpty()) "[Sticker] $mediaUrl" else textContent ?: ""
        return Message(
            id = id,
            // Prefer the canonical chat_id populated by the DB trigger. The old
            // implementation preferred thread_id, which moved every DM into a
            // different Room chat and made the sender/receiver UI lose the row.
            chatId = chatId ?: threadId ?: "",
            senderId = senderId,
            receiverId = receiverId,
            content = displayContent,
            createdAt = createdAt,
            status = calculatedStatus,
            replyToMessageId = replyTo,
            clientMessageUuid = clientMessageUuid,
            deliveredAt = deliveredAt,
            seenAt = seenAt,
            thumbnailUrl = thumbnailUrl,
            mediaUrl = mediaUrl,
            mediaMime = mediaMime,
            mediaSize = mediaSize,
            duration = mediaDuration,
            width = mediaWidth,
            height = mediaHeight,
            messageType = messageType ?: "text",
            isEdited = editedAt != null,
            deletedAt = deletedAt,
            isGhost = (isGhost == true) || (textContent?.startsWith("[Ghost]") == true) || (messageType == "ghost"),
            ghostOpenedAt = ghostOpenedAt,
            updatedAt = updatedAt,
            musicPlaylistId = musicPlaylistId
        )
    }
}

@JsonClass(generateAdapter = true)
data class MessageReaction(@Json(name = "thread_message_id") val threadMessageId: String, @Json(name = "user_id") val userId: String, @Json(name = "emoji") val emoji: String, @Json(name = "created_at") val createdAt: String? = null, @Json(name = "updated_at") val updatedAt: String? = null)

@JsonClass(generateAdapter = true)
data class UserState(@Json(name = "id") val id: String, @Json(name = "author_id") val authorId: String? = null, @Json(name = "user_id") val userIdField: String? = null, @Json(name = "media_url") val mediaUrl: String? = null, @Json(name = "media_urls") val mediaUrls: List<String>? = emptyList(), @Json(name = "audio_url") val audioUrl: String? = null,