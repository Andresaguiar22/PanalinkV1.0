package com.example.notification.engine.aggregation

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationTypeV2
import java.util.concurrent.ConcurrentHashMap

@Keep
class SmartAggregationEngine(
    private val rules: Map<NotificationTypeV2, AggregationRule> = SocialAggregationPolicy.defaultRules
) {

    private val eventBuffers = ConcurrentHashMap<String, MutableList<NotificationEvent>>()

    fun processEvent(event: NotificationEvent): AggregationResult? {
        val groupingKey = event.groupingKey ?: event.target?.entityId ?: return null
        val rule = rules[event.type] ?: return null

        val buffer = eventBuffers.computeIfAbsent(groupingKey) { mutableListOf() }
        synchronized(buffer) {
            buffer.add(event)
            val count = buffer.size
            val actorNames = buffer.mapNotNull { it.actor?.name }.distinct()

            if (count >= rule.minCountToAggregate) {
                val (title, body) = SocialAggregationPolicy.formatSocialText(event.type, actorNames, count)
                return AggregationResult(
                    groupingKey = groupingKey,
                    type = event.type,
                    totalCount = count,
                    actorNames = actorNames,
                    aggregatedTitle = title,
                    aggregatedBody = body
                )
            }
        }
        return null
    }

    fun clearBuffer(groupingKey: String) {
        eventBuffers.remove(groupingKey)
    }

    fun clearAll() {
        eventBuffers.clear()
    }
}
