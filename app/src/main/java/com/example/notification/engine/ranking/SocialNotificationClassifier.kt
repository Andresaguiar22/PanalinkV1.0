package com.example.notification.engine.ranking

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2

@Keep
enum class SocialClassification {
    PERSONAL,
    VIRAL,
    PROMOTIONAL,
    GENERIC
}

@Keep
class SocialNotificationClassifier private constructor() {

    fun classify(event: NotificationEvent, recentCountForEntity: Int = 1): Pair<SocialClassification, NotificationPriority> {
        val payloadStr = event.payload.toString().lowercase()

        if (payloadStr.contains("promo") || payloadStr.contains("ad") || payloadStr.contains("sponsored")) {
            return Pair(SocialClassification.PROMOTIONAL, NotificationPriority.LOW)
        }

        if (recentCountForEntity > 50) {
            return Pair(SocialClassification.VIRAL, NotificationPriority.NORMAL)
        }

        return when (event.type) {
            NotificationTypeV2.CHAT_MESSAGE,
            NotificationTypeV2.CHAT_MENTION,
            NotificationTypeV2.POST_REPLY,
            NotificationTypeV2.CALL_INCOMING -> Pair(SocialClassification.PERSONAL, NotificationPriority.HIGH)

            NotificationTypeV2.POST_COMMENT,
            NotificationTypeV2.POST_LIKE,
            NotificationTypeV2.REEL_LIKE,
            NotificationTypeV2.STORY_REACTION -> Pair(SocialClassification.PERSONAL, NotificationPriority.NORMAL)

            else -> Pair(SocialClassification.GENERIC, NotificationPriority.NORMAL)
        }
    }

    companion object {
        @Volatile
        private var instance: SocialNotificationClassifier? = null

        fun getInstance(): SocialNotificationClassifier {
            return instance ?: synchronized(this) {
                instance ?: SocialNotificationClassifier().also { instance = it }
            }
        }
    }
}
