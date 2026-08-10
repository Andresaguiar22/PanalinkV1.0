package com.example.notification.engine.presenter

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.Keep
import com.example.MainActivity
import com.example.notification.engine.model.NotificationDomain
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationTypeV2

@Keep
object DeepLinkResolver {

    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    const val EXTRA_TARGET_ROUTE = "extra_target_route"
    const val EXTRA_CHAT_ID = "chat_id"
    const val EXTRA_POST_ID = "post_id"
    const val EXTRA_STORY_ID = "story_id"
    const val EXTRA_REEL_ID = "reel_id"
    const val EXTRA_CALL_ID = "call_id"
    const val EXTRA_USER_ID = "user_id"

    fun resolveRoute(event: NotificationEvent): String {
        val targetId = event.target?.entityId ?: event.payload["id"] ?: ""
        
        return when (event.type) {
            NotificationTypeV2.CHAT_MESSAGE,
            NotificationTypeV2.CHAT_REPLY,
            NotificationTypeV2.CHAT_MENTION,
            NotificationTypeV2.CHAT_REACTION,
            NotificationTypeV2.CHAT_PIN -> "chat/$targetId"

            NotificationTypeV2.CALL_INCOMING,
            NotificationTypeV2.CALL_MISSED,
            NotificationTypeV2.CALL_ENDED -> "calls_history"

            NotificationTypeV2.POST_CREATED,
            NotificationTypeV2.POST_UPDATED,
            NotificationTypeV2.POST_LIKE,
            NotificationTypeV2.POST_REACTION,
            NotificationTypeV2.POST_SHARE,
            NotificationTypeV2.POST_SHARED,
            NotificationTypeV2.POST_REPOSTED,
            NotificationTypeV2.POST_TAG,
            NotificationTypeV2.POST_MENTION -> if (!event.target?.deepLinkUrl.isNullOrEmpty()) event.target.deepLinkUrl!!.removePrefix("panalink://app/") else "post/$targetId"

            NotificationTypeV2.POST_COMMENT,
            NotificationTypeV2.POST_REPLY,
            NotificationTypeV2.POST_REPLY_COMMENT,
            NotificationTypeV2.COMMENT_MENTION -> if (!event.target?.deepLinkUrl.isNullOrEmpty()) event.target.deepLinkUrl!!.removePrefix("panalink://app/") else "comment/$targetId"

            NotificationTypeV2.USER_FOLLOW_REQUEST -> "follow_requests"

            NotificationTypeV2.USER_FOLLOWED_YOU,
            NotificationTypeV2.USER_ACCEPTED_FOLLOW,
            NotificationTypeV2.PROFILE_FOLLOW,
            NotificationTypeV2.PROFILE_VIEW,
            NotificationTypeV2.FRIEND_REQUEST,
            NotificationTypeV2.FRIEND_ACCEPT -> "profile/$targetId"

            NotificationTypeV2.LOGIN_NEW_DEVICE,
            NotificationTypeV2.PASSWORD_CHANGED,
            NotificationTypeV2.SECURITY_ALERT -> "settings_security"

            else -> when (event.domain) {
                NotificationDomain.CHAT -> "chat/$targetId"
                NotificationDomain.CALLS -> "calls_history"
                NotificationDomain.POSTS, NotificationDomain.COMMENTS -> "post_detail/$targetId"
                NotificationDomain.STORIES -> "story_viewer/$targetId"
                NotificationDomain.REELS -> "reel_viewer/$targetId"
                NotificationDomain.PROFILE -> "profile/$targetId"
                else -> "notifications_center"
            }
        }
    }

    fun createPendingIntent(
        context: Context,
        event: NotificationEvent,
        requestCode: Int = event.id.hashCode()
    ): PendingIntent {
        val route = resolveRoute(event)
        val targetId = event.target?.entityId ?: ""

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = Intent.ACTION_VIEW
            data = Uri.parse("panalink://app/$route")
            putExtra(EXTRA_NOTIFICATION_ID, event.id)
            putExtra(EXTRA_TARGET_ROUTE, route)
            
            when (event.domain) {
                NotificationDomain.CHAT -> putExtra(EXTRA_CHAT_ID, targetId)
                NotificationDomain.POSTS, NotificationDomain.COMMENTS -> putExtra(EXTRA_POST_ID, targetId)
                NotificationDomain.STORIES -> putExtra(EXTRA_STORY_ID, targetId)
                NotificationDomain.REELS -> putExtra(EXTRA_REEL_ID, targetId)
                NotificationDomain.CALLS -> putExtra(EXTRA_CALL_ID, targetId)
                NotificationDomain.PROFILE -> putExtra(EXTRA_USER_ID, targetId)
                else -> {}
            }
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }
}
