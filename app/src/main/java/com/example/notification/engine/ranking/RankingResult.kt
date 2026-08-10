package com.example.notification.engine.ranking

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationPriority

@Keep
data class RankingResult(
    val score: Float,
    val priority: NotificationPriority,
    val reason: String
)
