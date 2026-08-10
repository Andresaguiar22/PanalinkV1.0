package com.example.notification.engine.ranking

import com.example.notification.engine.aggregation.SmartAggregationEngine
import com.example.notification.engine.model.EventActor
import com.example.notification.engine.model.EventTarget
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2
import com.example.notification.preferences.FavoriteContactsRepository
import com.example.notification.security.NotificationSecurityGuard
import com.example.notification.security.SecurityDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class NotificationRankingEngineTest {

    @Test
    fun testFavoriteContactMessagePriority() {
        val favoriteRepo = FavoriteContactsRepository.getInstance()
        val weightCalculator = RelationshipWeightCalculator.getInstance()
        val rankingEngine = NotificationRankingEngine.getInstance()

        val actorId = "user_favorite_999"
        favoriteRepo.addFavorite(actorId)
        weightCalculator.markFavorite(actorId, true)

        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.CHAT,
            type = NotificationTypeV2.CHAT_MESSAGE,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(id = actorId, name = "Favorite Friend"),
            target = EventTarget(entityId = "chat_123", entityType = "chat"),
            title = "Hola!",
            body = "¿Cómo estás?",
            groupingKey = "chat_123"
        )

        val result = rankingEngine.evaluate(event)
        assertEquals(NotificationPriority.HIGH, result.priority)
        assertTrue("Ranking score should be high for favorite", result.score >= 0.80f)

        val interruptiveness = favoriteRepo.resolveInterruptivenessForFavorite(actorId, isCall = false)
        assertEquals(InterruptivenessLevel.HEADS_UP, interruptiveness)
    }

    @Test
    fun testMassLikesAggregation() {
        val aggregationEngine = SmartAggregationEngine()
        val classifier = SocialNotificationClassifier.getInstance()

        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.SOCIAL,
            type = NotificationTypeV2.POST_LIKE,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(id = "user_viral_like", name = "User 1"),
            target = EventTarget(entityId = "post_viral_99", entityType = "post"),
            title = "Like",
            body = "Liked your post",
            groupingKey = "post_viral_99"
        )

        val (classification, priority) = classifier.classify(event, recentCountForEntity = 150)
        assertEquals(SocialClassification.VIRAL, classification)
        assertEquals(NotificationPriority.NORMAL, priority)

        val aggregated = aggregationEngine.processEvent(event)
        assertNotNull("Aggregation should accept viral event", aggregated)
    }

    @Test
    fun testIncomingCallCriticalPriority() {
        val rankingEngine = NotificationRankingEngine.getInstance()
        val favoriteRepo = FavoriteContactsRepository.getInstance()

        val callerId = "caller_007"
        val callEvent = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.CALLS,
            type = NotificationTypeV2.CALL_INCOMING,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(id = callerId, name = "Caller"),
            target = EventTarget(entityId = "call_channel_1", entityType = "call"),
            title = "Llamada entrante",
            body = "Llamada de voz",
            groupingKey = "call_channel_1"
        )

        val result = rankingEngine.evaluate(callEvent)
        assertEquals(NotificationPriority.CRITICAL, result.priority)
        assertEquals(1.0f, result.score, 0.001f)

        favoriteRepo.addFavorite(callerId)
        val interruptiveness = favoriteRepo.resolveInterruptivenessForFavorite(callerId, isCall = true)
        assertEquals(InterruptivenessLevel.FULLSCREEN, interruptiveness)
    }

    @Test
    fun testBlockedOrSpamUserRejection() {
        val guard = NotificationSecurityGuard.getInstance()
        val actorId = "spam_bot_000"

        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.SOCIAL,
            type = NotificationTypeV2.POST_LIKE,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(id = actorId, name = "Bot"),
            target = EventTarget(entityId = "post_1", entityType = "post"),
            title = "Like",
            body = "Like",
            groupingKey = "post_1"
        )

        var decision = guard.inspectEvent(event)
        var count = 0
        while (decision == SecurityDecision.ALLOW && count < 600) {
            decision = guard.inspectEvent(event)
            count++
        }

        assertTrue(
            "Guard should detect anomaly or suspend producer",
            decision == SecurityDecision.FORCE_AGGRESSIVE_GROUPING || decision == SecurityDecision.SUSPEND_PRODUCER || decision == SecurityDecision.BLOCK_SUSPENDED
        )
    }
}
