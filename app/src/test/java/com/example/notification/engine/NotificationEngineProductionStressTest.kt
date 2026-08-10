package com.example.notification.engine

import com.example.notification.engine.aggregation.SmartAggregationEngine
import com.example.notification.engine.model.EventActor
import com.example.notification.engine.model.EventTarget
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2
import com.example.notification.security.NotificationRateLimiter
import com.example.notification.security.NotificationSpamDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class NotificationEngineProductionStressTest {

    @Test
    fun testHighVolumeAggregationAndSpamDetection() = runBlocking(Dispatchers.Default) {
        val spamDetector = NotificationSpamDetector(
            rateLimiter = NotificationRateLimiter(maxEventsPerWindow = 100, windowSizeMs = 1000L)
        )
        val aggregationEngine = SmartAggregationEngine()

        val totalEvents = 10000
        val concurrency = 20
        val batchSize = totalEvents / concurrency

        val startTime = System.currentTimeMillis()

        val jobs = (0 until concurrency).map { worker ->
            async {
                var processedCount = 0
                for (i in 0 until batchSize) {
                    val event = NotificationEvent(
                        id = UUID.randomUUID().toString(),
                        domain = NotificationDomain.SOCIAL,
                        type = NotificationTypeV2.POST_LIKE,
                        priority = NotificationPriority.NORMAL,
                        interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
                        actor = EventActor(id = "user_${worker}_$i", name = "User $i"),
                        target = EventTarget(entityId = "post_123", entityType = "post"),
                        title = "Like Event",
                        body = "Liked post_123",
                        groupingKey = "post_123"
                    )

                    val isSpam = spamDetector.isSpam(event)
                    if (!isSpam) {
                        aggregationEngine.processEvent(event)
                        processedCount++
                    }
                }
                processedCount
            }
        }

        val results = jobs.awaitAll()
        val totalProcessed = results.sum()
        val durationMs = System.currentTimeMillis() - startTime

        assertTrue("Should process events efficiently under 3000ms", durationMs < 3000)
        assertTrue("Processed count should be positive", totalProcessed > 0)

        val finalAgg = aggregationEngine.processEvent(
            NotificationEvent(
                id = UUID.randomUUID().toString(),
                domain = NotificationDomain.SOCIAL,
                type = NotificationTypeV2.POST_LIKE,
                priority = NotificationPriority.NORMAL,
                interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
                actor = EventActor(id = "final_actor", name = "Final User"),
                target = EventTarget(entityId = "post_123", entityType = "post"),
                title = "Like Event",
                body = "Liked post_123",
                groupingKey = "post_123"
            )
        )

        assertNotNull("Aggregation result should be generated", finalAgg)
        assertEquals("post_123", finalAgg?.groupingKey)
    }
}
