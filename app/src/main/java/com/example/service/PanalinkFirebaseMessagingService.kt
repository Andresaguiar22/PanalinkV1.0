package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.repository.ProfilesRepository
import com.example.data.supabase.SupabaseClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import coil.imageLoader
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import androidx.core.graphics.drawable.toBitmap

class PanalinkFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "PanalinkFCM"
        const val CHANNEL_ID = "panalink_chats_channel"
        const val CHANNEL_NAME = "Mensajes de Panalink"
        const val PREFS_NAME = "panalink_fcm_prefs"
        const val KEY_FCM_TOKEN = "fcm_token"

        fun getSavedToken(context: Context): String? {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_FCM_TOKEN, null)
        }

        fun saveToken(context: Context, token: String) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_FCM_TOKEN, token)
                .apply()
        }

        fun sendTokenToSupabase(context: Context, token: String, explicitUserId: String? = null) {
            val currentUserId = explicitUserId ?: SupabaseClient.currentUser?.id
            if (!currentUserId.isNullOrEmpty()) {
                Log.d(TAG, "Sending FCM Token to Supabase for user $currentUserId: $token")
                val profilesRepo = ProfilesRepository()
                CoroutineScope(Dispatchers.IO).launch {
                    val resFingerprint = profilesRepo.updateDeviceFingerprint(currentUserId, token)
                    Log.d(TAG, "FCM device fingerprint update result: ${resFingerprint.isSuccess}")
                    val resEdge = profilesRepo.saveFcmTokenToEdgeFunction(currentUserId, token)
                    Log.d(TAG, "FCM Edge function save-token result: ${resEdge.isSuccess}")
                }
            } else {
                Log.d(TAG, "Cannot send token to Supabase, user is not logged in yet.")
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "onNewToken triggered. Token: $token")
        saveToken(applicationContext, token)
        sendTokenToSupabase(applicationContext, token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "onMessageReceived from: ${remoteMessage.from}")

        // Handle in scope to allow background loading of images
        serviceScope.launch {
            try {
                // Ensure notification channels are initialized
                NotificationHelper.createNotificationChannels(applicationContext)

                // Log payload data if any
                if (remoteMessage.data.isNotEmpty()) {
                    Log.d(TAG, "Message data payload: ${remoteMessage.data}")
                }

                // Handle notification payload
                val notificationTitle = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "Pana 💬"
                val notificationBody = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Nueva notificación recibida"
                val chatId = remoteMessage.data["chat_id"] ?: remoteMessage.data["chatId"] ?: ""
                val stateId = remoteMessage.data["state_id"] ?: remoteMessage.data["stateId"] ?: ""
                val notificationType = remoteMessage.data["notification_type"] ?: remoteMessage.data["notificationType"] ?: "new_message"
                val thumbnailUrl = remoteMessage.data["thumbnail_url"] ?: remoteMessage.data["thumbnailUrl"]
                val mediaUrl = remoteMessage.data["media_url"] ?: remoteMessage.data["mediaUrl"]
                val senderAvatar = remoteMessage.data["sender_avatar"] ?: remoteMessage.data["senderAvatar"]

                val isChatMuted = if (chatId.isNotEmpty() && notificationType == "new_message") {
                    try {
                        val db = com.example.data.database.PanalinkDatabase.getDatabase(applicationContext)
                        db.chatDao().getChatById(chatId)?.isMuted == true
                    } catch (e: Exception) { false }
                } else false

                if (isChatMuted) {
                    Log.d(TAG, "Chat $chatId is muted. FCM notification suppressed.")
                    return@launch
                }

                val isChatActive = notificationType == "new_message" && 
                        SupabaseClient.isChatScreenActive && 
                        SupabaseClient.activeChatId == chatId

                if (isChatActive) {
                    // If the chat is active in foreground, play gentle active chat sound
                    NotificationHelper.playActiveChatSound(applicationContext)
                    Log.d(TAG, "User is actively reading this chat. Skipping system notification pop.")
                } else if (notificationType == "new_message") {
                    PanaLinkNotificationManager.showChatNotification(
                        context = applicationContext,
                        senderName = remoteMessage.data["sender_name"] ?: remoteMessage.data["senderName"] ?: "",
                        senderAvatarUrl = remoteMessage.data["sender_avatar"] ?: remoteMessage.data["senderAvatar"],
                        messageText = notificationBody,
                        chatId = chatId
                    )
                } else {
                    // Determine appropriate channel based on type
                    val channelId = when (notificationType) {
                        "new_story", "new_reel" -> NotificationHelper.CHANNEL_ALERTS
                        "system_news", "app_update", "new_content" -> NotificationHelper.CHANNEL_ALERTS
                        "llamada_entrante" -> NotificationHelper.CHANNEL_CALLS
                        else -> NotificationHelper.CHANNEL_MESSAGES
                    }
                    
                    val extrasMap = mutableMapOf<String, String>()
                    if (notificationType == "llamada_entrante") {
                        val callerId = remoteMessage.data["callerId"] ?: ""
                        val callerName = remoteMessage.data["callerName"] ?: ""
                        val callType = remoteMessage.data["callType"] ?: ""
                        val sdp = remoteMessage.data["sdp"] ?: ""

                        extrasMap["callerId"] = callerId
                        extrasMap["callerName"] = callerName
                        extrasMap["callType"] = callType
                        extrasMap["sdp"] = sdp

                        // Proactively notify CallManager to start ringing/service even if app is in background
                        com.example.call.CallManager.getInstance(applicationContext)
                            .handleFCMIncomingCall(callerId, callerName, callType, sdp)
                    }

                    val senderName = remoteMessage.data["sender_name"] ?: remoteMessage.data["senderName"] ?: notificationTitle

                    // Load large icon (avatar) or big picture if available
                    var largeIconBitmap: android.graphics.Bitmap? = null
                    val loadUrl = thumbnailUrl ?: mediaUrl ?: senderAvatar
                    
                    if (!loadUrl.isNullOrEmpty()) {
                        try {
                            val request = ImageRequest.Builder(applicationContext)
                                .data(loadUrl)
                                .size(512, 512) // Optimize for notification size
                                .build()
                            val result = applicationContext.imageLoader.execute(request)
                            largeIconBitmap = result.drawable?.toBitmap()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading notification image: ${e.message}")
                        }
                    }

                    // Show status bar notification
                    NotificationHelper.showNotification(
                        context = applicationContext,
                        title = if (notificationType == "new_message") senderName else notificationTitle,
                        body = notificationBody,
                        chatId = chatId,
                        stateId = stateId,
                        notificationType = notificationType,
                        channelId = channelId,
                        extras = extrasMap,
                        imageUrl = thumbnailUrl ?: mediaUrl,
                        largeIcon = largeIconBitmap,
                        senderName = senderName
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing FCM message: ${e.message}")
            }
        }
    }
}
