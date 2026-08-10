package com.example.notification.engine.presenter

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.Keep
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationTypeV2

@Keep
object NotificationActionFactory {

    const val KEY_TEXT_REPLY = "key_text_reply"
    const val ACTION_NOTIFICATION_REPLY = "com.example.notification.ACTION_REPLY"
    const val ACTION_MARK_READ = "com.example.notification.ACTION_MARK_READ"
    const val ACTION_ACCEPT_CALL = "com.example.notification.ACTION_ACCEPT_CALL"
    const val ACTION_REJECT_CALL = "com.example.notification.ACTION_REJECT_CALL"
    const val ACTION_FOLLOW_BACK = "com.example.notification.ACTION_FOLLOW_BACK"

    fun createActionsForEvent(
        context: Context,
        event: NotificationEvent
    ): List<NotificationCompat.Action> {
        val actions = mutableListOf<NotificationCompat.Action>()

        when (event.type) {
            NotificationTypeV2.CHAT_MESSAGE, NotificationTypeV2.CHAT_REPLY, NotificationTypeV2.CHAT_MENTION -> {
                // Inline Text Reply Action
                val replyAction = createInlineReplyAction(context, event)
                if (replyAction != null) actions.add(replyAction)

                // Mark as Read Action
                val markReadAction = createMarkReadAction(context, event)
                if (markReadAction != null) actions.add(markReadAction)
            }

            NotificationTypeV2.CALL_INCOMING -> {
                // Accept Call Action
                val acceptAction = createCallAction(context, event, ACTION_ACCEPT_CALL, "Aceptar", android.R.drawable.ic_menu_call)
                if (acceptAction != null) actions.add(acceptAction)

                // Reject Call Action
                val rejectAction = createCallAction(context, event, ACTION_REJECT_CALL, "Rechazar", android.R.drawable.ic_menu_close_clear_cancel)
                if (rejectAction != null) actions.add(rejectAction)
            }

            NotificationTypeV2.PROFILE_FOLLOW -> {
                // Follow Back Action
                val followAction = createGenericAction(context, event, ACTION_FOLLOW_BACK, "Seguir también")
                if (followAction != null) actions.add(followAction)
            }

            else -> {}
        }

        return actions
    }

    private fun createInlineReplyAction(
        context: Context,
        event: NotificationEvent
    ): NotificationCompat.Action? {
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel("Escribe tu respuesta...")
            .build()

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_NOTIFICATION_REPLY
            putExtra(DeepLinkResolver.EXTRA_NOTIFICATION_ID, event.id)
            putExtra(DeepLinkResolver.EXTRA_CHAT_ID, event.target?.entityId ?: "")
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (event.id + "_reply").hashCode(),
            intent,
            flags
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_send,
            "Responder",
            pendingIntent
        ).addRemoteInput(remoteInput).build()
    }

    private fun createMarkReadAction(
        context: Context,
        event: NotificationEvent
    ): NotificationCompat.Action? {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_MARK_READ
            putExtra(DeepLinkResolver.EXTRA_NOTIFICATION_ID, event.id)
            putExtra(DeepLinkResolver.EXTRA_CHAT_ID, event.target?.entityId ?: "")
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (event.id + "_read").hashCode(),
            intent,
            flags
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_compass,
            "Marcar como leído",
            pendingIntent
        ).build()
    }

    private fun createCallAction(
        context: Context,
        event: NotificationEvent,
        actionType: String,
        label: String,
        iconRes: Int
    ): NotificationCompat.Action? {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = actionType
            putExtra(DeepLinkResolver.EXTRA_NOTIFICATION_ID, event.id)
            putExtra(DeepLinkResolver.EXTRA_CALL_ID, event.target?.entityId ?: "")
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (event.id + "_" + actionType).hashCode(),
            intent,
            flags
        )

        return NotificationCompat.Action.Builder(iconRes, label, pendingIntent).build()
    }

    private fun createGenericAction(
        context: Context,
        event: NotificationEvent,
        actionType: String,
        label: String
    ): NotificationCompat.Action? {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = actionType
            putExtra(DeepLinkResolver.EXTRA_NOTIFICATION_ID, event.id)
            putExtra(DeepLinkResolver.EXTRA_USER_ID, event.actor?.id ?: "")
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            (event.id + "_" + actionType).hashCode(),
            intent,
            flags
        )

        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_add,
            label,
            pendingIntent
        ).build()
    }
}
