package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class NotificationType {
    LIKE, COMMENT, FOLLOWER, MESSAGE, CALL, FAVORITE, SHARE, EVENT, GROUP, REEL, POST, VIEW, TRENDING
}

data class Notification(
    val id: String,
    val type: NotificationType,
    val sourceId: String,
    val profile: Profile,
    val timestamp: String,
    val isRead: Boolean,
    val actionText: String,
    val previewText: String? = null
)

@JsonClass(generateAdapter = true)
data class NotificationDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "actor_id") val actorId: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "entity_id") val entityId: String? = null,
    @Json(name = "is_read") val isRead: Boolean? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "actor_profile") val actorProfile: Profile? = null
) {
    fun toDomain(): Notification {
        val safeType = type ?: ""
        val notifType = when (safeType.lowercase()) {
            "like" -> NotificationType.LIKE
            "comment" -> NotificationType.COMMENT
            "follow" -> NotificationType.FOLLOWER
            "message" -> NotificationType.MESSAGE
            "call" -> NotificationType.CALL
            "favorite" -> NotificationType.FAVORITE
            "share" -> NotificationType.SHARE
            "reel", "status", "state" -> NotificationType.REEL
            "post" -> NotificationType.POST
            "trending", "viral" -> NotificationType.TRENDING
            "view" -> NotificationType.VIEW
            else -> NotificationType.LIKE
        }

        val actionText = when (notifType) {
            NotificationType.LIKE -> "reaccionó a tu publicación."
            NotificationType.COMMENT -> "comentó tu publicación."
            NotificationType.FOLLOWER -> "comenzó a seguirte."
            NotificationType.MESSAGE -> "te envió un mensaje."
            NotificationType.CALL -> "perdió una llamada contigo."
            NotificationType.FAVORITE -> "guardó tu publicación."
            NotificationType.SHARE -> "compartió tu publicación."
            NotificationType.REEL -> "publicó un nuevo estado."
            NotificationType.POST -> "hizo una nueva publicación."
            NotificationType.VIEW -> "vio tu historia o contenido."
            NotificationType.TRENDING -> "¡tu contenido se está volviendo tendencia! 🚀"
            else -> "interactuó contigo."
        }
        return Notification(
            id = id,
            type = notifType,
            sourceId = entityId ?: "",
            profile = actorProfile ?: Profile(actorId ?: "", "", null),
            timestamp = createdAt ?: "",
            isRead = isRead ?: false,
            actionText = actionText,
            previewText = null
        )
    }
}
