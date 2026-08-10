package com.example.notification.engine.ranking

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2

@Keep
class NotificationRankingEngine private constructor(
    private val weightCalculator: RelationshipWeightCalculator = RelationshipWeightCalculator.getInstance()
) {

    fun evaluate(event: NotificationEvent): RankingResult {
        val actorId = event.actor?.id.orEmpty()
        val relationshipWeight = weightCalculator.calculateWeight(actorId)
        val isFavorite = weightCalculator.isFavorite(actorId)
        val isCloseFriend = weightCalculator.isCloseFriend(actorId)

        // Calls and Security Alerts are always top priority
        if (event.domain == NotificationDomain.CALLS || event.type == NotificationTypeV2.CALL_INCOMING) {
            return RankingResult(
                score = 1.0f,
                priority = NotificationPriority.CRITICAL,
                reason = "Incoming call or critical communications event"
            )
        }

        if (event.domain == NotificationDomain.SECURITY) {
            return RankingResult(
                score = 0.95f,
                priority = NotificationPriority.CRITICAL,
                reason = "Security alert"
            )
        }

        // Direct Mentions or Direct Chat Messages
        if (event.type == NotificationTypeV2.CHAT_MENTION || event.type == NotificationTypeV2.POST_REPLY) {
            val score = 0.85f + (relationshipWeight * 0.15f)
            return RankingResult(
                score = score,
                priority = NotificationPriority.HIGH,
                reason = "Direct mention or reply"
            )
        }

        // Social interactions (Likes, Comments) weighted by affinity
        var baseScore = when (event.type) {
            NotificationTypeV2.POST_COMMENT, NotificationTypeV2.REEL_COMMENT -> 0.70f
            NotificationTypeV2.POST_LIKE, NotificationTypeV2.REEL_LIKE -> 0.40f
            NotificationTypeV2.PROFILE_FOLLOW -> 0.50f
            NotificationTypeV2.STORY_REACTION -> 0.60f
            else -> 0.30f
        }

        // Boost based on close friend / favorite status
        if (isFavorite || isCloseFriend) {
            baseScore += 0.30f
        } else {
            baseScore += (relationshipWeight * 0.20f)
        }

        val finalScore = baseScore.coerceIn(0.0f, 1.0f)
        val computedPriority = when {
            finalScore >= 0.80f -> NotificationPriority.HIGH
            finalScore >= 0.40f -> NotificationPriority.NORMAL
            else -> NotificationPriority.LOW
        }

        val reason = "Social event with relationship weight: $relationshipWeight (Favorite: $isFavorite)"
        return RankingResult(score = finalScore, priority = computedPriority, reason = reason)
    }

    companion object {
        @Volatile
        private var instance: NotificationRankingEngine? = null

        fun getInstance(): NotificationRankingEngine {
            return instance ?: synchronized(this) {
                instance ?: NotificationRankingEngine().also { instance = it }
            }
        }
    }
}
