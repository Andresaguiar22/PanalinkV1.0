package com.example.notification.engine.producers.calls

import androidx.annotation.Keep
import com.example.notification.engine.producers.NotificationEventPublisher

@Keep
object CallEventPublisher {

    suspend fun publishIncomingCall(
        callId: String,
        targetUserId: String,
        callerId: String,
        callerName: String? = null,
        callerAvatarUrl: String? = null,
        isVideo: Boolean = false
    ) {
        val callType = if (isVideo) "Llamada de video" else "Llamada de voz"
        NotificationEventPublisher.publishEvent(
            eventType = "CALL_INCOMING",
            actorId = callerId,
            targetUserId = targetUserId,
            entityId = callId,
            payload = mapOf("is_video" to isVideo),
            title = callerName ?: "Llamada entrante",
            body = "$callType de ${callerName ?: "un contacto"}",
            domain = "CALLS",
            priority = "CRITICAL",
            groupingKey = "call_$callId",
            actorName = callerName,
            actorAvatarUrl = callerAvatarUrl
        )
    }

    suspend fun publishMissedCall(
        callId: String,
        targetUserId: String,
        callerId: String,
        callerName: String? = null,
        callerAvatarUrl: String? = null
    ) {
        NotificationEventPublisher.publishEvent(
            eventType = "CALL_MISSED",
            actorId = callerId,
            targetUserId = targetUserId,
            entityId = callId,
            title = "Llamada perdida",
            body = "Llamada perdida de ${callerName ?: "un contacto"}",
            domain = "CALLS",
            priority = "HIGH",
            groupingKey = "call_$callId",
            actorName = callerName,
            actorAvatarUrl = callerAvatarUrl
        )
    }

    suspend fun publishCallRejectedOrEnded(
        callId: String,
        targetUserId: String,
        actorId: String,
        actorName: String? = null,
        isRejected: Boolean = false
    ) {
        val eventType = if (isRejected) "CALL_REJECTED" else "CALL_ENDED"
        NotificationEventPublisher.publishEvent(
            eventType = eventType,
            actorId = actorId,
            targetUserId = targetUserId,
            entityId = callId,
            title = if (isRejected) "Llamada rechazada" else "Llamada finalizada",
            body = if (isRejected) "La llamada fue rechazada" else "La llamada ha terminado",
            domain = "CALLS",
            priority = "NORMAL",
            groupingKey = "call_$callId",
            actorName = actorName
        )
    }
}
