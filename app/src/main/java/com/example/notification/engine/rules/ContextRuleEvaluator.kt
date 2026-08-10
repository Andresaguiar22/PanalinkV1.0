package com.example.notification.engine.rules

import androidx.annotation.Keep
import com.example.notification.engine.core.NotificationContext
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationTypeV2

@Keep
class ContextRuleEvaluator {

    fun evaluate(
        event: NotificationEvent,
        context: NotificationContext,
        baseInterruptiveness: InterruptivenessLevel
    ): InterruptivenessLevel {
        // 1. Self notification filtering
        if (context.currentUserId != null && event.actor?.id == context.currentUserId) {
            return InterruptivenessLevel.SILENT
        }

        // 2. Active Chat / Screen matching
        if (event.domain == NotificationDomain.CHAT || event.type == NotificationTypeV2.CHAT_MESSAGE) {
            val chatTargetId = event.target?.entityId ?: event.payload["chat_id"]
            if (!context.activeChatId.isNullOrEmpty() && context.activeChatId == chatTargetId) {
                // User is actively reading this chat screen -> downscale to SILENT
                return InterruptivenessLevel.SILENT
            }
        }

        // 3. Background vs Foreground adjustments
        if (!context.isAppInForeground) {
            if (baseInterruptiveness == InterruptivenessLevel.IN_APP_ONLY) {
                return InterruptivenessLevel.STATUS_BAR_ONLY
            }
        }

        return baseInterruptiveness
    }
}
