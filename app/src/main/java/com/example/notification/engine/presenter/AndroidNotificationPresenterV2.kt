package com.example.notification.engine.presenter

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.Keep
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import com.example.notification.engine.core.NotificationContext
import com.example.notification.engine.core.NotificationSubscriber
import com.example.notification.engine.model.InterruptivenessLevel
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.rules.NotificationRulesEngine
import com.example.notification.engine.rules.RuleDecision
import com.example.notification.engine.rules.RuleResult

@Keep
class AndroidNotificationPresenterV2(
    private val context: Context,
    private val rulesEngine: NotificationRulesEngine = NotificationRulesEngine(),
    private val builder: NotificationBuilderV2 = NotificationBuilderV2(context)
) : NotificationSubscriber {

    override val id: String = "AndroidNotificationPresenterV2"
    override val pipelinePriority: Int = 100 // Lower execution order than Rules & Storage

    init {
        NotificationChannelManager.createChannels(context)
    }

    override suspend fun process(event: NotificationEvent, context: NotificationContext): Boolean {
        val ruleResult = rulesEngine.evaluate(event, context)
        present(ruleResult)
        return true
    }

    fun present(ruleResult: RuleResult) {
        // 1. Verify acceptance decision
        if (ruleResult.decision != RuleDecision.ACCEPTED) return

        // 2. Check SILENT or IN_APP_ONLY interruptiveness
        if (ruleResult.effectiveInterruptiveness == InterruptivenessLevel.SILENT ||
            ruleResult.effectiveInterruptiveness == InterruptivenessLevel.IN_APP_ONLY
        ) {
            return
        }

        // 3. Permission checks (Android 13+)
        if (!hasNotificationPermission()) return

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (!notificationManager.areNotificationsEnabled()) return

            val event = ruleResult.event
            val notificationId = event.id.hashCode()

            // 4. Build and notify individual notification
            val notificationBuilder = builder.buildNotification(ruleResult)
            notificationManager.notify(notificationId, notificationBuilder.build())

            // 5. Post Group Summary if grouped
            if (ruleResult.isGrouped) {
                val groupKey = NotificationGroupManager.getGroupKey(ruleResult)
                val summaryText = ruleResult.groupSummaryText ?: "Nuevas notificaciones de PanaLink"
                val channelId = NotificationChannelManager.getChannelIdForDomain(event.domain)

                val summaryBuilder = NotificationGroupManager.buildSummaryNotification(
                    context = context,
                    channelId = channelId,
                    groupKey = groupKey,
                    summaryText = summaryText,
                    ruleResult = ruleResult
                )

                val summaryNotificationId = groupKey.hashCode()
                notificationManager.notify(summaryNotificationId, summaryBuilder.build())
            }

        } catch (e: SecurityException) {
            // Permission revoked or missing
        } catch (e: Exception) {
            // Unexpected OS notification exception
        }
    }

    fun cancelNotification(id: String) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancel(id.hashCode())
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun cancelAll() {
        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancelAll()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}
