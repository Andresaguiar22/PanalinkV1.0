package com.example.notification.engine.presenter

import android.content.Context
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationPriority
import com.example.notification.engine.model.NotificationTypeV2
import com.example.notification.engine.rules.RuleResult

@Keep
class NotificationBuilderV2(private val context: Context) {

    fun buildNotification(ruleResult: RuleResult): NotificationCompat.Builder {
        val event = ruleResult.event
        val channelId = NotificationChannelManager.getChannelIdForDomain(event.domain)

        val builder = NotificationCompat.Builder(context, channelId).apply {
            setSmallIcon(getSmallIconResource(event.domain))
            setContentTitle(event.title)
            setContentText(event.body)
            setAutoCancel(true)

            // Content PendingIntent
            setContentIntent(DeepLinkResolver.createPendingIntent(context, event))

            // Priority Mapping
            priority = mapPriorityToCompat(ruleResult.effectivePriority)

            // Category Mapping
            setCategory(mapCategoryToCompat(event.domain, event.type))

            // Grouping
            val groupKey = NotificationGroupManager.getGroupKey(ruleResult)
            setGroup(groupKey)

            // High Interruptiveness / Heads Up / Fullscreen Call handling
            if (ruleResult.effectiveInterruptiveness == InterruptivenessLevel.FULLSCREEN &&
                event.type == NotificationTypeV2.CALL_INCOMING
            ) {
                setFullScreenIntent(DeepLinkResolver.createPendingIntent(context, event), true)
            }

            // MessagingStyle for Chat messages
            if (event.domain == NotificationDomain.CHAT || event.type == NotificationTypeV2.CHAT_MESSAGE) {
                val senderName = event.actor?.name ?: "Pana"
                val person = Person.Builder()
                    .setName(senderName)
                    .setKey(event.actor?.id)
                    .build()

                val messagingStyle = NotificationCompat.MessagingStyle(person)
                    .setConversationTitle(event.target?.title ?: senderName)
                    .addMessage(event.body, event.timestamp, person)

                setStyle(messagingStyle)
            }

            // Quick Actions
            val actions = NotificationActionFactory.createActionsForEvent(context, event)
            actions.forEach { addAction(it) }
        }

        return builder
    }

    private fun getSmallIconResource(domain: NotificationDomain): Int {
        return when (domain) {
            NotificationDomain.CHAT -> android.R.drawable.stat_notify_chat
            NotificationDomain.CALLS -> android.R.drawable.stat_sys_phone_call
            NotificationDomain.SOCIAL, NotificationDomain.POSTS, NotificationDomain.STORIES -> android.R.drawable.stat_notify_more
            else -> android.R.drawable.stat_notify_sync
        }
    }

    private fun mapPriorityToCompat(priority: NotificationPriority): Int {
        return when (priority) {
            NotificationPriority.CRITICAL, NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            NotificationPriority.SILENT -> NotificationCompat.PRIORITY_MIN
        }
    }

    private fun mapCategoryToCompat(domain: NotificationDomain, type: NotificationTypeV2): String? {
        return when {
            type == NotificationTypeV2.CALL_INCOMING || type == NotificationTypeV2.CALL_MISSED -> NotificationCompat.CATEGORY_CALL
            domain == NotificationDomain.CHAT -> NotificationCompat.CATEGORY_MESSAGE
            domain == NotificationDomain.SOCIAL -> NotificationCompat.CATEGORY_SOCIAL
            domain == NotificationDomain.SECURITY -> NotificationCompat.CATEGORY_ALARM
            domain == NotificationDomain.SYSTEM -> NotificationCompat.CATEGORY_STATUS
            else -> null
        }
    }
}
