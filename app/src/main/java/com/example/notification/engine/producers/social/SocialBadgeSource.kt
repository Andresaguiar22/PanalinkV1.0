package com.example.notification.engine.producers.social

import androidx.annotation.Keep
import com.example.notification.engine.badge.BadgeCategory
import com.example.notification.engine.badge.BadgeSource
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.storage.dao.NotificationDaoV2
import kotlinx.coroutines.flow.Flow

@Keep
class SocialBadgeSource(
    private val notificationDao: NotificationDaoV2
) : BadgeSource {

    override val category: BadgeCategory = BadgeCategory.SOCIAL

    override fun observeCount(): Flow<Int> {
        val socialDomains = listOf(
            NotificationDomain.SOCIAL,
            NotificationDomain.POSTS,
            NotificationDomain.COMMENTS,
            NotificationDomain.REELS,
            NotificationDomain.STORIES,
            NotificationDomain.PROFILE
        )
        return notificationDao.observeUnreadCountForDomains(socialDomains)
    }
}
