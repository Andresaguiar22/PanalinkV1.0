package com.example.notification.ui

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2
import com.example.notification.engine.storage.entity.NotificationEntityV2
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Keep
data class NotificationUiModel(
    val id: String,
    val domain: NotificationDomain,
    val type: NotificationTypeV2,
    val priority: NotificationPriority,
    val title: String,
    val body: String,
    val actorName: String?,
    val actorAvatarUrl: String?,
    val targetEntityId: String?,
    val deepLinkUrl: String?,
    val formattedTimestamp: String,
    val isRead: Boolean,
    val isGrouped: Boolean,
    val groupSummaryText: String?
)

@Keep
object NotificationUiMapper {

    fun mapToUiModel(entity: NotificationEntityV2): NotificationUiModel {
        val sdf = SimpleDateFormat("HH:mm - dd MMM", Locale.getDefault())
        val formattedTime = sdf.format(Date(entity.timestamp))

        return NotificationUiModel(
            id = entity.id,
            domain = entity.domain,
            type = entity.type,
            priority = entity.priority,
            title = entity.title,
            body = entity.body,
            actorName = entity.actorName,
            actorAvatarUrl = entity.actorAvatarUrl,
            targetEntityId = entity.targetEntityId,
            deepLinkUrl = entity.deepLinkUrl,
            formattedTimestamp = formattedTime,
            isRead = entity.isRead,
            isGrouped = entity.isGrouped,
            groupSummaryText = entity.groupSummaryText
        )
    }
}
