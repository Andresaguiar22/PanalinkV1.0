package com.example.notification.engine.rules

import androidx.annotation.Keep
import com.example.notification.engine.core.NotificationContext
import com.example.notification.engine.core.NotificationSubscriber
import com.example.notification.engine.model.NotificationEvent

/**
 * Master rules orchestrator in Notification Engine V2.
 * Evaluates deduplication, expiration, context, priority, quiet hours, and grouping.
 */
@Keep
class NotificationRulesEngine(
    val deduplicationEngine: DeduplicationEngine = DeduplicationEngine(),
    val expirationManager: ExpirationManager = ExpirationManager(),
    val priorityResolver: PriorityResolver = PriorityResolver(),
    val contextEvaluator: ContextRuleEvaluator = ContextRuleEvaluator(),
    val quietHoursEngine: QuietHoursEngine = QuietHoursEngine(),
    val groupingEngine: GroupingEngine = GroupingEngine()
) : NotificationSubscriber {

    override val id: String = "NotificationRulesEngine"
    override val pipelinePriority: Int = 1000 // High priority to run early in pipeline

    fun evaluate(event: NotificationEvent, context: NotificationContext): RuleResult {
        val now = context.currentTimeMillis

        // 1. Deduplication check
        if (deduplicationEngine.isDuplicate(event, now)) {
            return RuleResult(
                decision = RuleDecision.REJECTED_DUPLICATE,
                event = event,
                effectivePriority = event.priority,
                effectiveInterruptiveness = event.interruptiveness,
                groupingKey = null,
                reason = "Duplicate event detected by key: ${event.effectiveDeduplicationKey()}"
            )
        }

        // 2. Expiration check
        if (expirationManager.isExpired(event, now)) {
            return RuleResult(
                decision = RuleDecision.REJECTED_EXPIRED,
                event = event,
                effectivePriority = event.priority,
                effectiveInterruptiveness = event.interruptiveness,
                groupingKey = null,
                reason = "Event expired based on timestamp or TTL"
            )
        }

        // 3. Resolve Priority & Default Interruptiveness
        val (basePriority, baseInterruptiveness) = priorityResolver.resolveDefaults(event)

        // 4. Context Rule Evaluation (Foreground/Background, Active Chat, Self-Action)
        val contextAdjustedInterruptiveness = contextEvaluator.evaluate(event, context, baseInterruptiveness)

        // 5. Quiet Hours Adjustment
        val (finalPriority, finalInterruptiveness) = quietHoursEngine.applyQuietHoursPolicy(
            event = event,
            currentPriority = basePriority,
            currentInterruptiveness = contextAdjustedInterruptiveness,
            nowMillis = now
        )

        // 6. Grouping Analysis
        val groupingAnalysis = groupingEngine.analyze(event)

        return RuleResult(
            decision = RuleDecision.ACCEPTED,
            event = event,
            effectivePriority = finalPriority,
            effectiveInterruptiveness = finalInterruptiveness,
            groupingKey = groupingAnalysis.groupingKey,
            groupSummaryText = groupingAnalysis.summaryText,
            isGrouped = groupingAnalysis.isGrouped,
            reason = "Event accepted and successfully processed by Rules Engine"
        )
    }

    override suspend fun process(event: NotificationEvent, context: NotificationContext): Boolean {
        val result = evaluate(event, context)
        // If rejected (duplicate, expired, etc.), return false to halt pipeline execution for this event
        return result.decision == RuleDecision.ACCEPTED
    }
}
