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
import com.example.notification.preferences.NotificationPreferenceEntity
import com.example.notification.preferences.NotificationPreferenceRepository
import com.example.notification.preferences.NotificationPreferenceSyncManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class NotificationBackendIntelligenceTest {

    @Test
    fun testFavoriteUserMessageHighPriority() {
        val favoriteRepo = FavoriteContactsRepository.getInstance()
        val weightCalculator = RelationshipWeightCalculator.getInstance()
        val rankingEngine = NotificationRankingEngine.getInstance()

        val actorId = "user_favorite_backend_777"
        favoriteRepo.addFavorite(actorId)
        weightCalculator.markFavorite(actorId, true)

        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            domain = NotificationDomain.CHAT,
            type = NotificationTypeV2.CHAT_MESSAGE,
            priority = NotificationPriority.NORMAL,
            interruptiveness = InterruptivenessLevel.STATUS_BAR_ONLY,
            actor = EventActor(id = actorId, name = "Best Friend"),
            target = EventTarget(entityId = "chat_456", entityType = "chat"),
            title = "Mensaje importante",
            body = "Hola amigo",
            groupingKey = "chat_456"
        )

        val result = rankingEngine.evaluate(event)
        assertEquals(NotificationPriority.HIGH, result.priority)
        assertTrue(result.score >= 0.80f)
    }

    @Test
    fun testMutedUserEntityHandling() {
        val preferenceRepo = NotificationPreferenceRepository.getInstance()
        val entityId = "chat_muted_123"

        preferenceRepo.muteEntity(entityId)
        assertTrue(preferenceRepo.isEntityMuted(entityId))

        preferenceRepo.unmuteEntity(entityId)
        assertFalse(preferenceRepo.isEntityMuted(entityId))
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
            actor = EventActor(id = "user_liker_1", name = "User 1"),
            target = EventTarget(entityId = "post_100", entityType = "post"),
            title = "Like",
            body = "Liked your photo",
            groupingKey = "post_100"
        )

        val (classification, priority) = classifier.classify(event, recentCountForEntity = 100)
        assertEquals(SocialClassification.VIRAL, classification)
        assertEquals(NotificationPriority.NORMAL, priority)

        val result = aggregationEngine.processEvent(event)
        assertNotNull(result)
    }

    @Test
    fun testOfflinePreferenceSyncConflictResolution() {
        val syncManager = NotificationPreferenceSyncManager.getInstance()
        val userId = "user_sync_123"
        val domain = "CHAT"

        val olderLocal = NotificationPreferenceEntity(
            userId = userId,
            domain = domain,
            enabled = true,
            updatedAt = 1000L
        )
        syncManager.updatePreferenceLocally(olderLocal)

        val newerRemote = NotificationPreferenceEntity(
            userId = userId,
            domain = domain,
            enabled = false,
            updatedAt = 2000L
        )
        val resolved = syncManager.syncFromRemote(newerRemote)

        assertEquals(2000L, resolved.updatedAt)
        assertFalse(resolved.enabled)
    }
}
