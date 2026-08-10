package com.example.notification.engine.storage.entity

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationTypeV2

@Keep
@Entity(
    tableName = "activity_feed_v2",
    indices = [
        Index("domain"),
        Index("type"),
        Index("timestamp")
    ]
)
data class ActivityFeedEntityV2(
    @PrimaryKey val id: String,
    val domain: NotificationDomain,
    val type: NotificationTypeV2,
    val actorId: String? = null,
    val actorName: String? = null,
    val actorAvatarUrl: String? = null,
    val targetEntityId: String? = null,
    val targetEntityType: String? = null,
    val title: String,
    val body: String,
    val mediaPreviewUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
