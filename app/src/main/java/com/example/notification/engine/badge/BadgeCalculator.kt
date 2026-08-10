package com.example.notification.engine.badge

import androidx.annotation.Keep

@Keep
class BadgeCalculator(
    private val maxBadgeLimit: Int = 99,
    private val mutedCategories: MutableSet<BadgeCategory> = mutableSetOf()
) {

    fun calculateEffectiveState(rawState: BadgeState): BadgeState {
        return BadgeState(
            unreadChats = if (mutedCategories.contains(BadgeCategory.CHATS)) 0 else rawState.unreadChats,
            unreadNotifications = if (mutedCategories.contains(BadgeCategory.NOTIFICATIONS)) 0 else rawState.unreadNotifications,
            missedCalls = if (mutedCategories.contains(BadgeCategory.CALLS)) 0 else rawState.missedCalls,
            followRequests = if (mutedCategories.contains(BadgeCategory.SOCIAL)) 0 else rawState.followRequests,
            securityAlerts = if (mutedCategories.contains(BadgeCategory.SECURITY)) 0 else rawState.securityAlerts,
            unreadGroups = if (mutedCategories.contains(BadgeCategory.GROUPS)) 0 else rawState.unreadGroups,
            unreadChannels = if (mutedCategories.contains(BadgeCategory.CHANNELS)) 0 else rawState.unreadChannels
        )
    }

    fun formatBadge(count: Int): String {
        return if (count > maxBadgeLimit) "$maxBadgeLimit+" else count.toString()
    }

    fun muteCategory(category: BadgeCategory) {
        mutedCategories.add(category)
    }

    fun unmuteCategory(category: BadgeCategory) {
        mutedCategories.remove(category)
    }

    fun isCategoryMuted(category: BadgeCategory): Boolean {
        return mutedCategories.contains(category)
    }
}
