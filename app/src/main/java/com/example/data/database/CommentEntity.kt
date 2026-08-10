package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Comment
import com.example.data.model.PostCommentDto
import com.example.data.model.Profile

@Entity(tableName = "local_comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val targetId: String, // postId or stateId
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String?,
    val content: String,
    val createdAt: String,
    val isReel: Boolean, // false for Post comments, true for Reel/Story comments
    val parentCommentId: String? = null,
    val deletedAt: String? = null,
    val syncStatus: String = "synced" // "synced", "pending_add", "pending_delete"
) {
    fun toPostCommentDto(): PostCommentDto {
        return PostCommentDto(
            id = id,
            postId = targetId,
            userId = authorId,
            content = content,
            createdAt = createdAt,
            profile = Profile(
                id = authorId,
                displayName = authorName,
                avatarUrl = authorAvatarUrl
            )
        )
    }

    fun toStateComment(): Comment {
        return Comment(
            id = id,
            stateId = targetId,
            userId = authorId,
            text = content,
            createdAt = createdAt,
            authorName = authorName,
            avatarUrl = authorAvatarUrl,
            parentCommentId = parentCommentId,
            deletedAt = deletedAt
        )
    }

    companion object {
        fun fromPostCommentDto(dto: PostCommentDto): CommentEntity {
            return CommentEntity(
                id = dto.id ?: java.util.UUID.randomUUID().toString(),
                targetId = dto.postId ?: "",
                authorId = dto.userId ?: "",
                authorName = dto.profile?.displayName?.takeIf { it.isNotBlank() } ?: "",
                authorAvatarUrl = dto.profile?.avatarUrl,
                content = dto.content ?: "",
                createdAt = dto.createdAt ?: com.example.data.supabase.SupabaseClient.getNowIsoString(),
                isReel = false,
                syncStatus = "synced"
            )
        }

        fun fromStateComment(comment: Comment, isReel: Boolean): CommentEntity {
            return CommentEntity(
                id = comment.id,
                targetId = comment.stateId,
                authorId = comment.userId,
                authorName = comment.authorName,
                authorAvatarUrl = comment.avatarUrl,
                content = comment.text,
                createdAt = comment.createdAt,
                isReel = isReel,
                parentCommentId = comment.parentCommentId,
                deletedAt = comment.deletedAt,
                syncStatus = "synced"
            )
        }
    }
}
