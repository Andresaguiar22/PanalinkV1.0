package com.example.notification.engine.producers.social

import androidx.annotation.Keep
import com.example.notification.engine.producers.NotificationEventPublisher

@Keep
object WallEventPublisher {

    suspend fun publishPostLike(
        postId: String,
        postAuthorId: String,
        actorId: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "POST_LIKE",
            actorId = actorId,
            targetUserId = postAuthorId,
            entityId = postId,
            title = actorName?.let { "$it le dio Me gusta a tu publicación" },
            body = "Le ha gustado tu publicación en el muro",
            domain = "SOCIAL",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }

    suspend fun publishPostComment(
        postId: String,
        commentId: String,
        postAuthorId: String,
        actorId: String,
        commentText: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null,
        isReply: Boolean = false
    ) {
        val eventType = if (isReply) "POST_REPLY" else "POST_COMMENT"
        NotificationEventPublisher.publishEvent(
            eventType = eventType,
            actorId = actorId,
            targetUserId = postAuthorId,
            entityId = postId,
            payload = mapOf("comment_id" to commentId, "text" to commentText),
            title = actorName?.let { if (isReply) "$it respondió a tu comentario" else "$it comentó tu publicación" },
            body = commentText,
            domain = "SOCIAL",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }

    suspend fun publishPostShared(
        postId: String,
        postAuthorId: String,
        actorId: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null,
        isRepost: Boolean = false
    ) {
        val eventType = if (isRepost) "POST_REPOST" else "POST_SHARED"
        NotificationEventPublisher.publishEvent(
            eventType = eventType,
            actorId = actorId,
            targetUserId = postAuthorId,
            entityId = postId,
            title = actorName?.let { if (isRepost) "$it republicó tu publicación" else "$it compartió tu publicación" },
            body = "Compartió tu contenido con su red",
            domain = "SOCIAL",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }

    suspend fun publishUserTaggedOrMentioned(
        postId: String,
        targetUserId: String,
        actorId: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null,
        contentSnippet: String = ""
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "USER_TAGGED",
            actorId = actorId,
            targetUserId = targetUserId,
            entityId = postId,
            title = actorName?.let { "$it te mencionó en una publicación" },
            body = contentSnippet.ifBlank { "Te ha etiquetado en el muro" },
            domain = "SOCIAL",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }

    suspend fun publishFollowEvent(
        targetUserId: String,
        actorId: String,
        actorName: String? = null,
        actorAvatarUrl: String? = null
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "USER_FOLLOWED_YOU",
            actorId = actorId,
            targetUserId = targetUserId,
            entityId = actorId,
            title = "Nuevo seguidor",
            body = "${actorName ?: "Alguien"} comenzó a seguirte",
            domain = "SOCIAL",
            actorName = actorName,
            actorAvatarUrl = actorAvatarUrl
        )
    }
}
