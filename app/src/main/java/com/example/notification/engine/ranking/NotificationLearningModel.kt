package com.example.notification.engine.ranking

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority

@Keep
interface NotificationLearningModel {
    suspend fun predictPriority(event: NotificationEvent): NotificationPriority
    suspend fun predictBestTime(event: NotificationEvent): Long
}
