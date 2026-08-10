package com.example.notification.engine.aggregation

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationTypeV2

@Keep
data class AggregationResult(
    val groupingKey: String,
    val type: NotificationTypeV2,
    val totalCount: Int,
    val actorNames: List<String>,
    val aggregatedTitle: String,
    val aggregatedBody: String,
    val lastTimestamp: Long = System.currentTimeMillis()
)
