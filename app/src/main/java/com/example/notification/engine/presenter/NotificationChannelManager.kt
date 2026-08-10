package com.example.notification.engine.presenter

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationDomain

@Keep
object NotificationChannelManager {

    const val CHANNEL_CHAT_V2 = "panalink_chat_v2"
    const val CHANNEL_CALLS_V2 = "panalink_calls_v2"
    const val CHANNEL_SOCIAL_V2 = "panalink_social_v2"
    const val CHANNEL_SYSTEM_V2 = "panalink_system_v2"
    const val CHANNEL_UPLOADS_V2 = "panalink_uploads_v2"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // 1. Chat Channel (High Importance)
        val chatChannel = NotificationChannel(
            CHANNEL_CHAT_V2,
            "PanaLink Chat & Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Direct messages, replies, and group chat notifications"
            enableVibration(true)
            setShowBadge(true)
        }

        // 2. Calls Channel (High Importance with Ringtone)
        val callsChannel = NotificationChannel(
            CHANNEL_CALLS_V2,
            "PanaLink Voice & Video Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming, ongoing, and missed call notifications"
            enableVibration(true)
            setShowBadge(true)
            setSound(
                Uri.parse("android.resource://${context.packageName}/raw/incoming_call_ringtone"),
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build()
            )
        }

        // 3. Social Channel (Default Importance)
        val socialChannel = NotificationChannel(
            CHANNEL_SOCIAL_V2,
            "PanaLink Social Activity",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Likes, comments, shares, stories, and new followers"
            enableVibration(true)
            setShowBadge(true)
        }

        // 4. System Channel (Low Importance)
        val systemChannel = NotificationChannel(
            CHANNEL_SYSTEM_V2,
            "PanaLink System & Security",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Security alerts, backups, and app status"
            setShowBadge(false)
        }

        // 5. Uploads Channel (Low Importance)
        val uploadsChannel = NotificationChannel(
            CHANNEL_UPLOADS_V2,
            "PanaLink Media Uploads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Progress notifications for background media uploads"
            setShowBadge(false)
        }

        notificationManager.createNotificationChannels(
            listOf(chatChannel, callsChannel, socialChannel, systemChannel, uploadsChannel)
        )
    }

    fun getChannelIdForDomain(domain: NotificationDomain): String {
        return when (domain) {
            NotificationDomain.CHAT, NotificationDomain.GROUPS, NotificationDomain.CHANNELS -> CHANNEL_CHAT_V2
            NotificationDomain.CALLS -> CHANNEL_CALLS_V2
            NotificationDomain.SOCIAL, NotificationDomain.POSTS, NotificationDomain.COMMENTS,
            NotificationDomain.STORIES, NotificationDomain.REELS, NotificationDomain.PROFILE,
            NotificationDomain.COMMUNITIES -> CHANNEL_SOCIAL_V2
            NotificationDomain.UPLOADS -> CHANNEL_UPLOADS_V2
            else -> CHANNEL_SYSTEM_V2
        }
    }
}
