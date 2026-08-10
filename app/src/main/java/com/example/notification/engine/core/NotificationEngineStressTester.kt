package com.example.notification.engine.core

import android.util.Log
import androidx.annotation.Keep
import com.example.notification.engine.analytics.NotificationAnalyticsEngine
import com.example.notification.engine.model.EventActor
import com.example.notification.engine.model.EventTarget
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.UUID

@Keep
class NotificationEngineStressTester(
    private val engine: NotificationEngine = NotificationEngine.getInstance()
) {

    data class StressTestResult(
        val totalEventsSent: Int,
        val totalDurationMs: Long,
        val eventsPerSecond: Double,
        val averageLatencyMs: Long
    )

    suspend fun runStressTest(eventCount: Int = 1000, concurrencyLevel: Int = 10): StressTestResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Iniciando prueba de estrés: $eventCount eventos con concurrencia $concurrencyLevel")

        val batchSize = eventCount / concurrencyLevel

        coroutineScope {
            val jobs = (0 until concurrencyLevel).map { workerIndex ->
                async {
                    for (i in 0 until batchSize) {
                        val event = NotificationEvent(
                            id = UUID.randomUUID().toString(),
                            domain = NotificationDomain.values()[i % NotificationDomain.values().size],
                            type = NotificationTypeV2.values()[i % NotificationTypeV2.values().size],
                            priority = NotificationPriority.NORMAL,
                            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
                            actor = EventActor(id = "actor_$i", name = "Stress User $i"),
                            target = EventTarget(entityId = "entity_$i", entityType = "stress"),
                            title = "Stress Test Event $i",
                            body = "Contenido de prueba de carga #$i",
                            groupingKey = "group_${i % 10}"
                        )
                        engine.publish(event)
                    }
                }
            }
            jobs.awaitAll()
        }

        val totalDuration = System.currentTimeMillis() - startTime
        val eventsPerSec = if (totalDuration > 0) (eventCount.toDouble() / totalDuration) * 1000 else 0.0
        val avgLatency = NotificationAnalyticsEngine.getInstance().metricsState.value.averageLatencyMs

        Log.d(TAG, "Prueba de estrés completada: $eventCount eventos en ${totalDuration}ms (${String.format("%.2f", eventsPerSec)} ev/sec)")

        StressTestResult(
            totalEventsSent = eventCount,
            totalDurationMs = totalDuration,
            eventsPerSecond = eventsPerSec,
            averageLatencyMs = avgLatency
        )
    }

    companion object {
        private const val TAG = "NotificationStressTester"
    }
}
