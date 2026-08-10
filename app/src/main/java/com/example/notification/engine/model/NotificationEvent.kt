package com.example.notification.engine.model

import androidx.annotation.Keep
import java.util.UUID

/**
 * Universal, immutable contract for all notification events across PanaLink V2.0.
 * Every producer (Chat, Feed, Stories, Calls, Security, WorkManager) emits instances of NotificationEvent.
 */
@Keep
data class NotificationEvent(
    val id: String = UUID.randomUUID().toString(),
    val domain: NotificationDomain,
    val type: NotificationTypeV2,
    val actor: EventActor? = null,
    val target: EventTarget? = null,
    val priority: NotificationPriority = NotificationPriority.NORMAL,
    val interruptiveness: InterruptivenessLevel = InterruptivenessLevel.STATUS_BAR_ONLY,
    val origin: NotificationOrigin = NotificationOrigin.LOCAL_UI,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long? = null,
    val title: String,
    val body: String,
    val attachments: List<NotificationAttachment> = emptyList(),
    val payload: Map<String, String> = emptyMap(),
    val deduplicationKey: String? = null,
    val groupingKey: String? = null,
    val isRead: Boolean = false
) {
    /**
     * Checks if this event has expired based on current timestamp.
     */
    fun isExpired(now: Long = System.currentTimeMillis()): Boolean {
        return expiresAt != null && now > expiresAt
    }

    /**
     * Helper to compute default deduplication key if not explicitly supplied.
     */
    fun effectiveDeduplicationKey(): String {
        val key = deduplicationKey
        if (!key.isNullOrBlank()) return key
        val actorId = actor?.id ?: "system"
        val entityId = target?.entityId ?: "none"
        return "${type.name}_${actorId}_${entityId}"
    }

    /**
     * Helper to compute default grouping key if not explicitly supplied.
     */
    fun effectiveGroupingKey(): String {
        val key = groupingKey
        if (!key.isNullOrBlank()) return key
        val entityId = target?.entityId ?: target?.parentEntityId ?: "global"
        return "${type.domain.name}_$entityId"
    }
}

private fun String?.isNull_orEmpty(): Boolean = this == null || this.trim().isEmpty()
