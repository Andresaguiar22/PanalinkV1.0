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

data class UploadMediaResult(val url: String, val thumbnailUrl: String? = null, val mime: String? = null, val size: Long? = null, val duration: Long? = null, val width: Int? = null, val height: Int? = null)

@JsonClass(generateAdapter = true)
data class ContactEntity(@Json(name = "id") val id: String = "", @Json(name = "owner_user_id") val ownerUserId: String, @Json(name = "contact_user_id") val contactUserId: String, @Json(name = "created_at") val createdAt: String? = null)
@JsonClass(generateAdapter = true)
data class ContactWithProfileEntity(@Json(name = "id") val id: String = "", @Json(name = "owner_user_id") val ownerUserId: String, @Json(name = "contact_user_id") val contactUserId: String, @Json(name = "created_at") val createdAt: String? = null, @Json(name = "profiles") val profiles: EmbeddedProfile? = null) { val rawProfile: Profile? get() = profiles?.profile; fun getProfile(moshi: com.squareup.moshi.Moshi): Profile? = rawProfile }
data class EmbeddedProfile(val profile: Profile?)
class EmbeddedProfileAdapter {
    @FromJson fun fromJson(reader: com.squareup.moshi.JsonReader, delegate: com.squareup.moshi.JsonAdapter<Profile>): EmbeddedProfile? = try { when (reader.peek()) { com.squareup.moshi.JsonReader.Token.BEGIN_ARRAY -> { reader.beginArray(); var prof: Profile? = null; if (reader.hasNext() && reader.peek() != com.squareup.moshi.JsonReader.Token.END_ARRAY) prof = delegate.fromJson(reader); while (reader.hasNext()) reader.skipValue(); reader.endArray(); EmbeddedProfile(prof) }; com.squareup.moshi.JsonReader.Token.BEGIN_OBJECT -> EmbeddedProfile(delegate.fromJson(reader)); com.squareup.moshi.JsonReader.Token.NULL -> { reader.nextNull<Unit>(); null }; else -> { reader.skipValue(); null } } } catch (e: Exception) { android.util.Log.e("EmbeddedProfileAdapter", "Deserialization error in embedded profile JSON", e); null }
    @ToJson fun toJson(writer: com.squareup.moshi.JsonWriter, value: EmbeddedProfile?, delegate: com.squareup.moshi.JsonAdapter<Profile>) { if (value?.profile == null) writer.nullValue() else delegate.toJson(writer, value.profile) }
}

@JsonClass(generateAdapter = true)
data class FriendRequestEntity(@Json(name = "id") val id: String = "", @Json(name = "sender_id") val senderId: String, @Json(name = "receiver_id") val receiverId: String, @Json(name = "status") val status: String, @Json(name = "created_at") val createdAt: String? = null, @Json(name = "sender") val sender: Profile? = null, @Json(name = "receiver") val receiver: Profile? = null)

@JsonClass(generateAdapter = true)
data class ThreadMessage(
    @Json(name = "id") val id: String = "", @Json(name = "thread_id") val threadId: String? = null, @Json(name = "chat_id") val chatId: String? = null, @Json(name = "sender_id") val senderId: String, @Json(name = "receiver_id") val receiverId: String? = null, @Json(name = "message_type") val messageType: String? = null, @Json(name = "text_content") val textContent: String? = null, @Json(name = "media_url") val mediaUrl: String? = null, @Json(name = "thumbnail_url") val thumbnailUrl: String? = null, @Json(name = "media_mime") val mediaMime: String? = null, @Json(name = "file_size") val mediaSize: Long? = null, @Json(name = "duration") val mediaDuration: Long? = null, @Json(name = "width") val mediaWidth: Int? = null, @Json(name = "height") val mediaHeight: Int? = null, @Json(name = "client_message_uuid") val clientMessageUuid: String = "", @Json(name = "created_at") val createdAt: String, @Json(name = "status") val status: String? = "sent", @Json(name = "delivered_at") val deliveredAt: String? = null, @Json(name = "seen_at") val seenAt: String? = null, @Json(name = "reply_to") val replyTo: String? = null, @Json(name = "deleted_at") val deletedAt: String? = null, @Json(name = "edited_at") val editedAt: String? = null, @Json(name = "is_ghost") val isGhost: Boolean? = false, @Json(name = "ghost_opened_at") val ghostOpenedAt: String? = null, @Json(name = "updated_at") val updatedAt: String? = null, @Json(name = "music_playlist_id") val musicPlaylistId: String? = null
) {
    fun toMessage(): Message {
        val calculatedStatus = when { seenAt != null -> "seen"; deliveredAt != null -> "delivered"; else -> status ?: "sent" }
        val displayContent = if (messageType == "sticker" && !mediaUrl.isNullOrEmpty()) "[Sticker] $mediaUrl" else textContent ?: ""
        return Message(id = id, chatId = chatId ?: threadId ?: "", senderId = senderId, receiverId = receiverId, content = displayContent, createdAt = createdAt, status = calculatedStatus, replyToMessageId = replyTo, clientMessageUuid = clientMessageUuid, deliveredAt = deliveredAt, seenAt = seenAt, thumbnailUrl = thumbnailUrl, mediaUrl = mediaUrl, mediaMime = mediaMime, mediaSize = mediaSize, duration = mediaDuration, width = mediaWidth, height = mediaHeight, messageType = messageType ?: "text", isEdited = editedAt != null, deletedAt = deletedAt, isGhost = (isGhost == true) || (textContent?.startsWith("[Ghost]") == true) || (messageType == "ghost"), ghostOpenedAt = ghostOpenedAt, updatedAt = updatedAt, musicPlaylistId = musicPlaylistId)
    }
}

@JsonClass(generateAdapter = true)
data class MessageReaction(@Json(name = "thread_message_id") val threadMessageId: String, @Json(name = "user_id") val userId: String, @Json(name = "emoji") val emoji: String, @Json(name = "created_at") val createdAt: String? = null, @Json(name = "updated_at") val updatedAt: String? = null)

@JsonClass(generateAdapter = true)
data class UserState(@Json(name = "id") val id: String, @Json(name = "author_id") val authorId: String? = null, @Json(name = "user_id") val userIdField: String? = null, @Json(name = "media_url") val mediaUrl: String? = null, @Json(name = "media_urls") val mediaUrls: List<String>? = emptyList(), @Json(name = "audio_url") val audioUrl: String? = null, @Json(name = "media_type") val mediaType: String, @Json(name = "caption") val caption: String? = null, @Json(name = "visibility") val visibility: String? = "public", @Json(name = "expires_at") val expiresAt: String? = null, @Json(name = "type") val type: String? = "story", @Json(name = "created_at") val createdAt: String? = null, @Json(name = "likes_count") val likesCount: Int? = 0, @Json(name = "comments_count") val commentsCount: Int? = 0, @Json(name = "favorites_count") val favoritesCount: Int? = 0, @Json(name = "shares_count") val sharesCount: Int? = 0, @Json(name = "liked_by_me") val likedByMe: Boolean? = false, @Json(name = "favorited_by_me") val favoritedByMe: Boolean? = false, @Json(name = "viewed_by_me") val viewedByMe: Boolean? = false, @Json(name = "views_count") val viewsCount: Int? = 0, @Json(name = "preview_metadata") val previewMetadata: String? = null, val localVideoPath: String? = null) {
    val userId: String get() = authorId ?: userIdField ?: ""
    val isReel: Boolean get() { if (type != null) return type == "reel"; val cap = caption ?: ""; return cap.contains("[Transition:") || cap.contains("[CoverFrame:") }
}

typealias UserStatus = UserState
data class ChatWithDetails(val chat: Chat, val otherMember: Profile?, val lastMessage: Message?, val unreadCount: Int = 0)
data class UserStateWithUser(val state: UserState, val profile: Profile)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(@Json(name = "display_name") val displayName: String, @Json(name = "avatar_url") val avatarUrl: String? = null, @Json(name = "is_profile_complete") val isProfileComplete: Boolean? = null, @Json(name = "first_name") val firstName: String? = null, @Json(name = "last_name") val lastName: String? = null, @Json(name = "status") val status: String? = null, @Json(name = "birth_date") val birthDate: String? = null, @Json(name = "sex") val sex: String? = null, @Json(name = "interests") val interests: List<String>? = null, @Json(name = "cover_url") val coverUrl: String? = null)
@JsonClass(generateAdapter = true)
data class SignUpOptions(@Json(name = "emailRedirectTo") val emailRedirectTo: String? = "panalink://verify")
@JsonClass(generateAdapter = true)
data class SignUpRequest(@Json(name = "email") val email: String, @Json(name = "password") val password: String, @Json(name = "data") val data: Map<String, String>? = null, @Json(name = "options") val options: SignUpOptions? = SignUpOptions())
@JsonClass(generateAdapter = true)
data class SignInRequest(@Json(name = "email") val email: String, @Json(name = "password") val password: String)
@JsonClass(generateAdapter = true)
data class ResendRequest(@Json(name = "email") val email: String, @Json(name = "type") val type: String = "signup")
@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(@Json(name = "token_hash") val tokenHash: String, @Json(name = "type") val type: String = "signup")
@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(@Json(name = "refresh_token") val refreshToken: String)
@JsonClass(generateAdapter = true)
data class AuthUser(@Json(name = "id") val id: String, @Json(name = "email") val email: String?, @Json(name = "email_confirmed_at") val emailConfirmedAt: String?, @Json(name = "user_metadata") val userMetadata: Map<String, Any>? = null)
@JsonClass(generateAdapter = true)
data class AuthResponse(@Json(name = "access_token") val accessToken: String, @Json(name = "refresh_token") val refreshToken: String?, @Json(name = "user") val user: AuthUser)
@JsonClass(generateAdapter = true)
data class UserResponse(@Json(name = "id") val id: String, @Json(name = "email") val email: String?, @Json(name = "email_confirmed_at") val emailConfirmedAt: String?)
@JsonClass(generateAdapter = true)
data class UserKeyDto(@Json(name = "user_id") val userId: String, @Json(name = "public_key") val publicKey: String)
@JsonClass(generateAdapter = true)
data class AddContactByPinResponse(@Json(name = "success") val success: Boolean, @Json(name = "contact_id") val contactId: String, @Json(name = "display_name") val displayName: String, @Json(name = "avatar_url") val avatarUrl: String?, @Json(name = "thread_id") val threadId: String, @Json(name = "is_already_contact") val isAlreadyContact: Boolean? = false)
@JsonClass(generateAdapter = true)
data class ContactIdentifierResponse(@Json(name = "pin") val pin: String = "", @Json(name = "qr_token") val qrToken: String? = null, @Json(name = "qr_payload") val qrPayload: String = "")
fun formatIsoDateTime(isoStr: String?): String { if (isoStr.isNullOrEmpty()) return ""; return try { val cleanStr = if (isoStr.contains(".")) isoStr.substringBefore(".") else if (isoStr.contains("+")) isoStr.substringBefore("+") else if (isoStr.endsWith("Z")) isoStr.substringBefore("Z") else isoStr; val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }; val date = parser.parse(cleanStr); if (date != null) java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(date) else "" } catch (e: Exception) { "" } }
@JsonClass(generateAdapter = true)
data class StickerResult(@Json(name = "id") val id: String? = null, @Json(name = "title") val title: String? = null, @Json(name = "url") val url: String, @Json(name = "preview") val preview: String, @Json(name = "width") val width: Int? = null, @Json(name = "height") val height: Int? = null)
@JsonClass(generateAdapter = true)
data class StickerSearchResponse(@Json(name = "results") val results: List<StickerResult>)
@JsonClass(generateAdapter = true)
data class SearchStickersRequest(@Json(name = "query") val query: String, @Json(name = "limit") val limit: Int = 24)
@JsonClass(generateAdapter = true)
data class GiphyResponse(@Json(name = "data") val data: List<GiphySticker>)
@JsonClass(generateAdapter = true)
data class GiphySticker(@Json(name = "id") val id: String, @Json(name = "images") val images: GiphyImages)
@JsonClass(generateAdapter = true)
data class GiphyImages(@Json(name = "fixed_width") val fixedWidth: GiphyImage)
@JsonClass(generateAdapter = true)
data class GiphyImage(@Json(name = "url") val url: String, @Json(name = "width") val width: String, @Json(name = "height") val height: String)
@JsonClass(generateAdapter = true)
data class ChannelComment(@Json(name = "id") val id: String = "", @Json(name = "chat_message_id") val chatMessageId: String, @Json(name = "chat_id") val chatId: String, @Json(name = "author_user_id") val authorUserId: String, @Json(name = "content_text") val contentText: String, @Json(name = "created_at") val createdAt: String? = null, @Json(name = "edited_at") val editedAt: String? = null, @Json(name = "deleted_at") val deletedAt: String? = null, @Json(name = "author") val author: Profile? = null)
@JsonClass(generateAdapter = true)
data class BlockedUser(@Json(name = "id") val id: String? = null, @Json(name = "user_id") val userId: String = "", @Json(name = "blocked_user_id") val blockedUserId: String = "", @Json(name = "created_at") val createdAt: String? = null)
@JsonClass(generateAdapter = true)
data class PresenceSession(@Json(name = "id") val id: String = "", @Json(name = "user_id") val userId: String = "", @Json(name = "device_type") val deviceType: String? = null, @Json(name = "os_name") val osName: String? = null, @Json(name = "ip_address") val ipAddress: String? = null, @Json(name = "location_approx") val locationApprox: String? = null, @Json(name = "isp_name") val ispName: String? = null, @Json(name = "app_version") val appVersion: String? = null, @Json(name = "created_at") val createdAt: String? = null, @Json(name = "last_active_at") val lastActiveAt: String? = null, @Json(name = "is_active") val isActive: Boolean = false, @Json(name = "push_token") val pushToken: String? = null)
