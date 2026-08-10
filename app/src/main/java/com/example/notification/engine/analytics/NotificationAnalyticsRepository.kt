package com.example.notification.engine.analytics

import androidx.annotation.Keep
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

@Keep
class NotificationAnalyticsRepository {

    private val metricsBuffer = CopyOnWriteArrayList<NotificationMetric>()
    private val _recentMetricsFlow = MutableStateFlow<List<NotificationMetric>>(emptyList())
    val recentMetricsFlow: StateFlow<List<NotificationMetric>> = _recentMetricsFlow.asStateFlow()

    fun logMetric(metric: NotificationMetric) {
        metricsBuffer.add(metric)
        if (metricsBuffer.size > 500) {
            metricsBuffer.removeAt(0)
        }
        _recentMetricsFlow.value = metricsBuffer.toList()
    }

    fun getAllMetrics(): List<NotificationMetric> = metricsBuffer.toList()

    fun clear() {
        metricsBuffer.clear()
        _recentMetricsFlow.value = emptyList()
    }
}
