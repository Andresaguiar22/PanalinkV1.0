package com.example.notification.engine.aggregation

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationTypeV2

@Keep
enum class AggregationWindowType {
    SLIDING_TEN_SECONDS,
    SLIDING_ONE_MINUTE,
    SLIDING_ONE_HOUR,
    DAILY
}

@Keep
data class AggregationRule(
    val type: NotificationTypeV2,
    val windowType: AggregationWindowType = AggregationWindowType.SLIDING_ONE_MINUTE,
    val minCountToAggregate: Int = 2,
    val maxActorsInTitle: Int = 3
)
