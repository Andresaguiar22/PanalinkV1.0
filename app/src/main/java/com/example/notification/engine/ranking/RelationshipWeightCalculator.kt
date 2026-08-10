package com.example.notification.engine.ranking

import androidx.annotation.Keep
import java.util.concurrent.ConcurrentHashMap

@Keep
class RelationshipWeightCalculator private constructor() {

    private val interactionCounts = ConcurrentHashMap<String, Int>()
    private val favoriteUsers = ConcurrentHashMap.newKeySet<String>()
    private val closeFriends = ConcurrentHashMap.newKeySet<String>()

    fun recordInteraction(userId: String, weight: Int = 1) {
        interactionCounts.merge(userId, weight) { old, new -> old + new }
    }

    fun markFavorite(userId: String, isFavorite: Boolean) {
        if (isFavorite) {
            favoriteUsers.add(userId)
        } else {
            favoriteUsers.remove(userId)
        }
    }

    fun markCloseFriend(userId: String, isClose: Boolean) {
        if (isClose) {
            closeFriends.add(userId)
        } else {
            closeFriends.remove(userId)
        }
    }

    fun isFavorite(userId: String): Boolean = favoriteUsers.contains(userId)

    fun isCloseFriend(userId: String): Boolean = closeFriends.contains(userId)

    fun calculateWeight(userId: String): Float {
        var weight = 0.2f // baseline score for unknown / normal user

        if (favoriteUsers.contains(userId)) {
            weight += 0.5f
        }
        if (closeFriends.contains(userId)) {
            weight += 0.3f
        }

        val interactions = interactionCounts[userId] ?: 0
        val interactionBonus = (interactions * 0.05f).coerceAtMost(0.3f)
        weight += interactionBonus

        return weight.coerceIn(0.0f, 1.0f)
    }

    companion object {
        @Volatile
        private var instance: RelationshipWeightCalculator? = null

        fun getInstance(): RelationshipWeightCalculator {
            return instance ?: synchronized(this) {
                instance ?: RelationshipWeightCalculator().also { instance = it }
            }
        }
    }
}
