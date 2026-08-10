package com.example.notification.engine.rules

import androidx.annotation.Keep
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2

@Keep
class PriorityResolver {

    fun resolveDefaults(event: NotificationEvent): Pair<NotificationPriority, InterruptivenessLevel> {
        val priority = resolvePriority(event)
        val interruptiveness = resolveInterruptiveness(event, priority)
        return Pair(priority, interruptiveness)
    }

    private fun resolvePriority(event: NotificationEvent): NotificationPriority {
        // If event already has a non-NORMAL priority explicitly specified, preserve it
        if (event.priority != NotificationPriority.NORMAL) {
            return event.priority
        }

        return when (event.type) {
            NotificationTypeV2.CALL_INCOMING,
            NotificationTypeV2.LOGIN_NEW_DEVICE,
            NotificationTypeV2.SECURITY_ALERT -> NotificationPriority.CRITICAL

            NotificationTypeV2.CHAT_MESSAGE,
            NotificationTypeV2.CHAT_MENTION,
            NotificationTypeV2.CHAT_REPLY,
            NotificationTypeV2.CALL_MISSED,
            NotificationTypeV2.POST_REPLY,
            NotificationTypeV2.FRIEND_REQUEST -> NotificationPriority.HIGH

            NotificationTypeV2.POST_LIKE,
            NotificationTypeV2.POST_COMMENT,
            NotificationTypeV2.REEL_LIKE,
            NotificationTypeV2.REEL_COMMENT,
            NotificationTypeV2.STORY_REACTION,
            NotificationTypeV2.PROFILE_FOLLOW -> NotificationPriority.NORMAL

            NotificationTypeV2.STORY_VIEW,
            NotificationTypeV2.PROFILE_VIEW -> NotificationPriority.LOW

            NotificationTypeV2.CHAT_TYPING,
            NotificationTypeV2.CHAT_RECORDING,
            NotificationTypeV2.UPLOAD_PROGRESS -> NotificationPriority.SILENT

            else -> when (event.domain) {
                NotificationDomain.CALLS, NotificationDomain.SECURITY -> NotificationPriority.CRITICAL
                NotificationDomain.CHAT -> NotificationPriority.HIGH
                NotificationDomain.SYSTEM, NotificationDomain.UPLOADS -> NotificationPriority.NORMAL
                else -> NotificationPriority.NORMAL
            }
        }
    }

    private fun resolveInterruptiveness(
        event: NotificationEvent,
        priority: NotificationPriority
    ): InterruptivenessLevel {
        if (event.interruptiveness != InterruptivenessLevel.STATUS_BAR_ONLY) {
            return event.interruptiveness
        }

        return when (priority) {
            NotificationPriority.CRITICAL -> {
                if (event.type == NotificationTypeV2.CALL_INCOMING) InterruptivenessLevel.FULLSCREEN
                else InterruptivenessLevel.HEADS_UP
            }
            NotificationPriority.HIGH -> InterruptivenessLevel.HEADS_UP
            NotificationPriority.NORMAL -> InterruptivenessLevel.STATUS_BAR_ONLY
            NotificationPriority.LOW -> InterruptivenessLevel.IN_APP_ONLY
            NotificationPriority.SILENT -> InterruptivenessLevel.SILENT
        }
    }
}
