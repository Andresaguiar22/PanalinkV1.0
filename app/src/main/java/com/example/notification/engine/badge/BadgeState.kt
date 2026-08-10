package com.example.notification.engine.badge

import androidx.annotation.Keep

@Keep
data class BadgeState(
    val unreadChats: Int = 0,
    val unreadNotifications: Int = 0,
    val missedCalls: Int = 0,
    val followRequests: Int = 0,
    val securityAlerts: Int = 0,
    val unreadGroups: Int = 0,
    val unreadChannels: Int = 0
) {
    val total: Int
        get() = unreadChats +
                unreadNotifications +
                missedCalls +
                followRequests +
                securityAlerts +
                unreadGroups +
                unreadChannels

    fun formattedTotal(max: Int = 99): String {
        return if (total > max) "$max+" else total.toString()
    }

    fun countForCategory(category: BadgeCategory): Int {
        return when (category) {
            BadgeCategory.CHATS -> unreadChats
            BadgeCategory.NOTIFICATIONS -> unreadNotifications
            BadgeCategory.CALLS -> missedCalls
            BadgeCategory.SOCIAL -> followRequests
            BadgeCategory.SECURITY -> securityAlerts
            BadgeCategory.GROUPS -> unreadGroups
            BadgeCategory.CHANNELS -> unreadChannels
        }
    }
}
