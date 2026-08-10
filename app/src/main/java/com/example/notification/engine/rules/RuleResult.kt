package com.example.notification.engine.rules

import androidx.annotation.Keep
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority

@Keep
enum class RuleDecision {
    ACCEPTED,
    REJECTED_DUPLICATE,
    REJECTED_EXPIRED,
    REJECTED_MUTED,
    REJECTED_FILTERED
}

@Keep
data class RuleResult(
    val decision: RuleDecision,
    val event: NotificationEvent,
    val effectivePriority: NotificationPriority,
    val effectiveInterruptiveness: InterruptivenessLevel,
    val groupingKey: String?,
    val groupSummaryText: String? = null,
    val isGrouped: Boolean = false,
    val reason: String? = null
)
