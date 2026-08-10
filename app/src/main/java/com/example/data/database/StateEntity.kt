package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.UserState
import com.example.data.model.UserStateWithUser
import com.example.data.model.Profile

@Entity(tableName = "user_states")
data class StateEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val mediaUrl: String,
    val mediaType: String,
    val caption: String?,
    val expiresAt: String?,
    val createdAt: String,
    val isReel: Boolean,
    val viewsCount: Int = 0,
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val favoritesCount: Int = 0,
    val viewedByMe: Boolean = false,
    val likedByMe: Boolean = false,
    val favoritedByMe: Boolean = false,
    val mediaUrls: String? = null,
    val audioUrl: String? = null,
    val localVideoPath: String? = null,
    // Store user profile info in the same entity or join later. 
    // To keep it simple and reactive for the feed, we'll store basic profile info here.
    val authorDisplayName: String? = null,
    val authorAvatarUrl: String? = null
) {
    fun toUserStateWithUser(): UserStateWithUser {
        val state = UserState(
            id = id,
            authorId = userId,
            userIdField = userId,
            mediaUrl = mediaUrl,
            mediaUrls = mediaUrls?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
            audioUrl = audioUrl,
            mediaType = mediaType,
            caption = caption,
            expiresAt = expiresAt,
            createdAt = createdAt,
            viewsCount = viewsCount,
            likesCount = likesCount,
            commentsCount = commentsCount,
            sharesCount = sharesCount,
            favoritesCount = favoritesCount,
            viewedByMe = viewedByMe,
            likedByMe = likedByMe,
            favoritedByMe = favoritedByMe,
            localVideoPath = localVideoPath,
            type = if (isReel) "reel" else "story"
        )
        val profile = Profile(
            id = userId,
            displayName = authorDisplayName ?: "Usuario",
            avatarUrl = authorAvatarUrl
        )
        return UserStateWithUser(state, profile)
    }

    companion object {
        fun fromUserStateWithUser(item: UserStateWithUser, localPath: String? = null): StateEntity {
            return StateEntity(
                id = item.state.id,
                userId = item.state.userId,
                mediaUrl = item.state.mediaUrl ?: "",
                mediaType = item.state.mediaType,
                caption = item.state.caption,
                expiresAt = item.state.expiresAt,
                createdAt = item.state.createdAt ?: "",
                isReel = item.state.isReel,
                viewsCount = item.state.viewsCount ?: 0,
                likesCount = item.state.likesCount ?: 0,
                commentsCount = item.state.commentsCount ?: 0,
                sharesCount = item.state.sharesCount ?: 0,
                favoritesCount = item.state.favoritesCount ?: 0,
                viewedByMe = item.state.viewedByMe ?: false,
                likedByMe = item.state.likedByMe ?: false,
                favoritedByMe = item.state.favoritedByMe ?: false,
                mediaUrls = item.state.mediaUrls?.joinToString(","),
                audioUrl = item.state.audioUrl,
                localVideoPath = localPath ?: item.state.localVideoPath,
                authorDisplayName = item.profile?.displayName?.takeIf { it.isNotBlank() },
                authorAvatarUrl = item.profile?.avatarUrl
            )
        }
    }
}
