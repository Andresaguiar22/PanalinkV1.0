package com.example.notification.engine.storage

import androidx.annotation.Keep
import com.example.notification.engine.core.NotificationContext
import com.example.notification.engine.core.NotificationSubscriber
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationTypeV2
import com.example.notification.engine.rules.RuleResult
import com.example.notification.engine.storage.dao.ActivityFeedDao
import com.example.notification.engine.storage.dao.NotificationDaoV2
import com.example.notification.engine.storage.entity.ActivityFeedEntityV2
import com.example.notification.engine.storage.entity.NotificationEntityV2
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Keep
class NotificationLocalRepository(
    private val notificationDao: NotificationDaoV2,
    private val activityFeedDao: ActivityFeedDao
) : NotificationSubscriber {

    override val id: String = "NotificationLocalRepository"
    override val pipelinePriority: Int = 500 // Intermediate pipeline priority: save after rules evaluation

    override suspend fun process(event: NotificationEvent, context: NotificationContext): Boolean {
        // Passive activity feed events (e.g. story views, profile views, post views) -> activity_feed_v2
        val isPassiveActivity = when (event.type) {
            NotificationTypeV2.STORY_VIEW,
            NotificationTypeV2.PROFILE_VIEW -> true
            else -> false
        }

        if (isPassiveActivity) {
            val activityEntity = ActivityFeedEntityV2(
                id = event.id,
                domain = event.domain,
                type = event.type,
                actorId = event.actor?.id,
                actorName = event.actor?.name,
                actorAvatarUrl = event.actor?.avatarUrl,
                targetEntityId = event.target?.entityId,
                targetEntityType = event.target?.entityType,
                title = event.title,
                body = event.body,
                mediaPreviewUrl = event.attachments.firstOrNull()?.url,
                timestamp = event.timestamp
            )
            activityFeedDao.insert(activityEntity)
        } else {
            // Standard notification entity -> notifications_v2
            val entity = convertEventToEntity(event)
            notificationDao.insert(entity)
        }
        return true
    }

    suspend fun saveRuleResult(ruleResult: RuleResult) {
        val event = ruleResult.event
        val entity = convertEventToEntity(
            event = event,
            effectivePriority = ruleResult.effectivePriority,
            effectiveInterruptiveness = ruleResult.effectiveInterruptiveness,
            groupingKey = ruleResult.groupingKey,
            groupSummaryText = ruleResult.groupSummaryText,
            isGrouped = ruleResult.isGrouped
        )
        notificationDao.insert(entity)
    }

    suspend fun markAsRead(id: String) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    suspend fun deleteById(id: String) {
        notificationDao.deleteById(id)
    }

    suspend fun deleteExpired(now: Long = System.currentTimeMillis()) {
        notificationDao.deleteExpired(now)
    }

    suspend fun clearAll() {
        notificationDao.clearAll()
        activityFeedDao.clearAll()
    }

    fun observeAllNotifications(): Flow<List<NotificationEntityV2>> {
        return notificationDao.observeAll()
    }

    fun observeUnreadCount(): Flow<Int> {
        return notificationDao.observeUnreadCount()
    }

    fun observeByDomain(domain: NotificationDomain): Flow<List<NotificationEntityV2>> {
        return notificationDao.observeByDomain(domain)
    }

    fun observeByType(type: NotificationTypeV2): Flow<List<NotificationEntityV2>> {
        return notificationDao.observeByType(type)
    }

    fun observeActivityFeed(): Flow<List<ActivityFeedEntityV2>> {
        return activityFeedDao.observeAll()
    }

    private fun convertEventToEntity(
        event: NotificationEvent,
        effectivePriority: com.example.notification.engine.model.NotificationPriority = event.priority,
        effectiveInterruptiveness: com.example.notification.engine.model.InterruptivenessLevel = event.interruptiveness,
        groupingKey: String? = event.groupingKey,
        groupSummaryText: String? = null,
        isGrouped: Boolean = false
    ): NotificationEntityV2 {
        val attachmentsJson = if (event.attachments.isNotEmpty()) {
            val jsonArray = JSONArray()
            event.attachments.forEach { att ->
                val obj = JSONObject().apply {
                    put("type", att.type.name)
                    put("url", att.url)
                    put("localPath", att.localPath)
                    put("mimeType", att.mimeType)
                    put("title", att.title)
                    put("description", att.description)
                }
                jsonArray.put(obj)
            }
            jsonArray.toString()
        } else null

        val payloadJson = if (event.payload.isNotEmpty()) {
            JSONObject(event.payload).toString()
        } else null

        return NotificationEntityV2(
            id = event.id,
            domain = event.domain,
            type = event.type,
            priority = effectivePriority,
            interruptiveness = effectiveInterruptiveness,
            actorId = event.actor?.id,
            actorName = event.actor?.name,
            actorUsername = event.actor?.username,
            actorAvatarUrl = event.actor?.avatarUrl,
            actorIsVerified = event.actor?.isVerified ?: false,
            targetEntityId = event.target?.entityId,
            targetEntityType = event.target?.entityType,
            targetParentEntityId = event.target?.parentEntityId,
            targetTitle = event.target?.title,
            targetPreviewText = event.target?.previewText,
            deepLinkUrl = event.target?.deepLinkUrl,
            title = event.title,
            body = event.body,
            attachmentsJson = attachmentsJson,
            payloadJson = payloadJson,
            groupingKey = groupingKey ?: event.effectiveGroupingKey(),
            groupSummaryText = groupSummaryText,
            isGrouped = isGrouped,
            isRead = event.isRead,
            timestamp = event.timestamp,
            expiresAt = event.expiresAt
        )
    }
}
