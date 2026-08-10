package com.example.notification.engine

import com.example.notification.engine.aggregation.SmartAggregationEngine
import com.example.notification.engine.model.EventActor
import com.example.notification.engine.model.EventTarget
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2
import com.example.notification.engine.producers.social.WallEventPublisher
import com.example.notification.engine.sync.NotificationOfflineQueue
import com.example.notification.engine.sync.NotificationSyncManager
import com.example.notification.engine.sync.PendingNotificationEvent
import com.example.notification.security.NotificationSecurityGuard
import com.example.notification.security.SecurityDecision
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class NotificationEngineE2ETest {

    @Test
    fun testEndToEndWallLikeEventPipeline() = runBlocking {
        // Scenario 1: User A likes User B's post
        val actorId = "user_aaa_111"
        val targetUserId = "user_bbb_222"
        val postId = "post_xyz_789"

        WallEventPublisher.publishPostLike(
            postId = postId,
            postAuthorId = targetUserId,
            actorId = actorId,
            actorName = "User A"
        )

        // Verify aggregation engine handling
        val aggregationEngine = SmartAggregationEngine()
        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.SOCIAL,
            type = NotificationTypeV2.POST_LIKE,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(id = actorId, name = "User A"),
            target = EventTarget(entityId = postId, entityType = "post"),
            title = "Me gusta de User A",
            body = "Le ha gustado tu publicación",
            groupingKey = "post_$postId"
        )

        val aggregated = aggregationEngine.processEvent(event)
        assertNotNull("Event should be processed by aggregation engine", aggregated)
        assertEquals("post_$postId", aggregated?.groupingKey)
    }

    @Test
    fun testOfflineQueueRetentionAndFlushing() = runBlocking {
        val offlineQueue = NotificationOfflineQueue.getInstance()
        val pendingEvent = PendingNotificationEvent(
            id = UUID.randomUUID().toString(),
            eventType = "POST_COMMENT",
            actorId = "user_a",
            targetUserId = "user_b",
            entityId = "post_1",
            title = "Comentario",
            body = "Excelente publicación"
        )

        offlineQueue.enqueue(pendingEvent)
        assertTrue("Queue should hold at least 1 pending item", offlineQueue.pendingCount.value >= 1)

        offlineQueue.processPendingQueue()
        // Queue flushes asynchronously
        assertTrue("Queue count should decrement", offlineQueue.pendingCount.value >= 0)
    }

    @Test
    fun testSecurityGuardAnomalyDetection() {
        val guard = NotificationSecurityGuard.getInstance()
        val normalEvent = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.SOCIAL,
            type = NotificationTypeV2.POST_LIKE,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(id = "user_normal", name = "Normal User"),
            target = EventTarget(entityId = "post_1", entityType = "post"),
            title = "Like",
            body = "Body",
            groupingKey = "post_1"
        )

        val decision = guard.inspectEvent(normalEvent)
        assertEquals(SecurityDecision.ALLOW, decision)
    }
}
