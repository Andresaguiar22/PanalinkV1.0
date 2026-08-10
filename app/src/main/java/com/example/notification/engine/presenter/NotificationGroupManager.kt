package com.example.notification.engine.presenter

import android.content.Context
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import com.example.notification.engine.rules.RuleResult

@Keep
object NotificationGroupManager {

    private const val GROUP_PREFIX = "panalink_group_"

    fun getGroupKey(ruleResult: RuleResult): String {
        val rawKey = ruleResult.groupingKey ?: ruleResult.event.effectiveGroupingKey()
        return GROUP_PREFIX + rawKey
    }

    fun buildSummaryNotification(
        context: Context,
        channelId: String,
        groupKey: String,
        summaryText: String,
        ruleResult: RuleResult
    ): NotificationCompat.Builder {
        val title = "PanaLink"

        return NotificationCompat.Builder(context, channelId).apply {
            setSmallIcon(android.R.drawable.stat_notify_chat)
            setContentTitle(title)
            setContentText(summaryText)
            setStyle(NotificationCompat.InboxStyle().setSummaryText(summaryText))
            setGroup(groupKey)
            setGroupSummary(true)
            setAutoCancel(true)
            setContentIntent(DeepLinkResolver.createPendingIntent(context, ruleResult.event))
        }
    }
}
