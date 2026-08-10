package com.example.notification.engine.producers

import android.util.Log
import androidx.annotation.Keep
import com.example.data.supabase.SupabaseClient
import com.example.notification.engine.analytics.NotificationAnalytics
import com.example.notification.engine.core.NotificationEngine
import com.example.notification.engine.model.EventActor
import com.example.notification.engine.model.EventTarget
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

@Keep
object NotificationEventPublisher {

    private const val TAG = "NotificationEventPublisher"

    suspend fun publishEvent(
        eventType: String,
        actorId: String,
        targetUserId: String,
        entityId: String,
        payload: Map<String, Any> = emptyMap(),
        title: String? = null,
        body: String? = null,
        domain: String? = null,
        groupingKey: String? = null,
        priority: String? = null,
        actorName: String? = null,
        actorAvatarUrl: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currentUid = SupabaseClient.currentUser?.id ?: actorId
            val finalActorId = if (actorId.isBlank()) currentUid else actorId

            // Self-notification rule check
            if (finalActorId.isNotBlank() && targetUserId.isNotBlank() && finalActorId == targetUserId) {
                Log.d(TAG, "Self-notification omitted for target $targetUserId")
                return@withContext Result.success(Unit)
            }

            val typeV2 = runCatching { NotificationTypeV2.valueOf(eventType) }.getOrDefault(NotificationTypeV2.POST_LIKE)
            val domainV2 = domain?.let { runCatching { NotificationDomain.valueOf(it) }.getOrNull() }
                ?: getDomainFromType(typeV2)

            val priorityV2 = priority?.let { runCatching { NotificationPriority.valueOf(it) }.getOrNull() }
                ?: getPriorityFromType(typeV2)

            val eventId = UUID.randomUUID().toString()
            val finalTitle = title ?: defaultTitleFor(typeV2, actorName)
            val finalBody = body ?: defaultBodyFor(typeV2, actorName)

            val event = NotificationEvent(
                id = eventId,
                domain = domainV2,
                type = typeV2,
                priority = priorityV2,
                interruptiveness = getInterruptiveness(priorityV2, typeV2),
                actor = EventActor(
                    id = finalActorId,
                    name = actorName ?: "Usuario",
                    avatarUrl = actorAvatarUrl
                ),
                target = EventTarget(
                    entityId = entityId,
                    entityType = domainV2.name.lowercase(),
                    previewText = finalBody
                ),
                title = finalTitle,
                body = finalBody,
                groupingKey = groupingKey ?: "${typeV2.name}_$entityId"
            )

            // Analytics creation tracking
            NotificationAnalytics.trackCreated(eventId, typeV2.name)

            // 1. Publish locally to NotificationEngine V2 pipeline
            NotificationEngine.getInstance().publish(event)

            // 2. Async remote publish to notification_events table if API is available
            val service = SupabaseClient.apiService
            val apiKey = SupabaseClient.supabaseAnonKey
            val token = SupabaseClient.currentToken
            if (service != null && !token.isNullOrBlank()) {
                val bearer = "Bearer $token"
                val dbPayload = mapOf(
                    "id" to eventId,
                    "event_type" to eventType,
                    "actor_id" to finalActorId,
                    "target_user_id" to targetUserId,
                    "entity_id" to entityId,
                    "domain" to domainV2.name,
                    "title" to finalTitle,
                    "body" to finalBody,
                    "grouping_key" to (groupingKey ?: "${typeV2.name}_$entityId"),
                    "priority" to priorityV2.name,
                    "status" to "PENDING"
                )
                runCatching {
                    // Safe best-effort insert into notification_events table
                    service.createNotification(apiKey, bearer, dbPayload)
                }
            }

            Log.d(TAG, "Successfully published event $eventId ($eventType) for target $targetUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing notification event", e)
            Result.failure(e)
        }
    }

    private fun getDomainFromType(type: NotificationTypeV2): NotificationDomain {
        return when (type) {
            NotificationTypeV2.CHAT_MESSAGE, NotificationTypeV2.CHAT_MENTION, NotificationTypeV2.CHAT_REPLY -> NotificationDomain.CHAT
            NotificationTypeV2.CALL_INCOMING, NotificationTypeV2.CALL_MISSED, NotificationTypeV2.CALL_REJECTED -> NotificationDomain.CALLS
            NotificationTypeV2.SYSTEM_ANNOUNCEMENT, NotificationTypeV2.SECURITY_ALERT, NotificationTypeV2.LOGIN_NEW_DEVICE -> NotificationDomain.SYSTEM
            else -> NotificationDomain.SOCIAL
        }
    }

    private fun getPriorityFromType(type: NotificationTypeV2): NotificationPriority {
        return when (type) {
            NotificationTypeV2.CALL_INCOMING -> NotificationPriority.CRITICAL
            NotificationTypeV2.CALL_MISSED, NotificationTypeV2.CHAT_MESSAGE, NotificationTypeV2.LOGIN_NEW_DEVICE, NotificationTypeV2.SECURITY_ALERT -> NotificationPriority.HIGH
            else -> NotificationPriority.NORMAL
        }
    }

    private fun getInterruptiveness(priority: NotificationPriority, type: NotificationTypeV2): InterruptivenessLevel {
        return when {
            type == NotificationTypeV2.CALL_INCOMING -> InterruptivenessLevel.FULLSCREEN
            priority == NotificationPriority.HIGH || priority == NotificationPriority.CRITICAL -> InterruptivenessLevel.HEADS_UP
            else -> InterruptivenessLevel.STATUS_BAR_ONLY
        }
    }

    private fun defaultTitleFor(type: NotificationTypeV2, actorName: String?): String {
        val name = actorName ?: "Alguien"
        return when (type) {
            NotificationTypeV2.POST_LIKE -> "Me gusta de $name"
            NotificationTypeV2.POST_COMMENT -> "Comentario de $name"
            NotificationTypeV2.USER_FOLLOWED_YOU -> "Nuevo seguidor"
            NotificationTypeV2.CHAT_MESSAGE -> name
            NotificationTypeV2.CALL_INCOMING -> "Llamada de $name"
            NotificationTypeV2.CALL_MISSED -> "Llamada perdida"
            else -> "PanaLink"
        }
    }

    private fun defaultBodyFor(type: NotificationTypeV2, actorName: String?): String {
        val name = actorName ?: "Alguien"
        return when (type) {
            NotificationTypeV2.POST_LIKE -> "$name reaccionó a tu publicación"
            NotificationTypeV2.POST_COMMENT -> "$name comentó tu publicación"
            NotificationTypeV2.USER_FOLLOWED_YOU -> "$name comenzó a seguirte"
            NotificationTypeV2.CHAT_MESSAGE -> "Te ha enviado un mensaje"
            NotificationTypeV2.CALL_INCOMING -> "Llamada entrante"
            NotificationTypeV2.CALL_MISSED -> "$name te llamó"
            else -> "Nueva interacción en PanaLink"
        }
    }
}
