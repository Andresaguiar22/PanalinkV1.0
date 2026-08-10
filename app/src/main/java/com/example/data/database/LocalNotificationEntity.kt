package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.NotificationType
import com.example.data.model.Notification
import com.example.data.model.Profile

@Entity(tableName = "local_notifications")
data class LocalNotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val sourceId: String,
    val actorId: String,
    val actorName: String,
    val actorAvatarUrl: String?,
    val timestamp: String,
    val isRead: Boolean,
    val actionText: String,
    val previewText: String?
) {
    fun toDomain(): Notification {
        return Notification(
            id = id,
            type = try { NotificationType.valueOf(type) } catch(e: Exception) { NotificationType.LIKE },
            sourceId = sourceId,
            profile = Profile(actorId, actorName, actorAvatarUrl),
            timestamp = timestamp,
            isRead = isRead,
            actionText = actionText,
            previewText = previewText
        )
    }

    companion object {
        fun fromDomain(notif: Notification): LocalNotificationEntity {
            return LocalNotificationEntity(
                id = notif.id,
                type = notif.type.name,
                sourceId = notif.sourceId,
                actorId = notif.profile.id,
                actorName = notif.profile.displayName,
                actorAvatarUrl = notif.profile.avatarUrl,
                timestamp = notif.timestamp,
                isRead = notif.isRead,
                actionText = notif.actionText,
                previewText = notif.previewText
            )
        }
    }
}
