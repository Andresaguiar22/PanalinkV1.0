package com.example.notification.engine.storage.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2

@Keep
@Entity(
    tableName = "notifications_v2",
    indices = [
        Index("type"),
        Index("domain"),
        Index("isRead"),
        Index("timestamp"),
        Index("groupingKey")
    ]
)
data class NotificationEntityV2(
    @PrimaryKey val id: String,
    val domain: NotificationDomain,
    val type: NotificationTypeV2,
    val priority: NotificationPriority,
    val interruptiveness: InterruptivenessLevel,
    val actorId: String? = null,
    val actorName: String? = null,
    val actorUsername: String? = null,
    val actorAvatarUrl: String? = null,
    val actorIsVerified: Boolean = false,
    val targetEntityId: String? = null,
    val targetEntityType: String? = null,
    val targetParentEntityId: String? = null,
    val targetTitle: String? = null,
    val targetPreviewText: String? = null,
    val deepLinkUrl: String? = null,
    val title: String,
    val body: String,
    val attachmentsJson: String? = null,
    val payloadJson: String? = null,
    val groupingKey: String? = null,
    val groupSummaryText: String? = null,
    val isGrouped: Boolean = false,
    val isRead: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null
)
