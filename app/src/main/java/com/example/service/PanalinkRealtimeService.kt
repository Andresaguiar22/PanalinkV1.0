package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import coil.imageLoader
import coil.request.ImageRequest
import androidx.core.graphics.drawable.toBitmap
import com.example.data.model.Message
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

class PanalinkRealtimeService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var messageCollectionJob: Job? = null

    companion object {
        private const val TAG = "PanalinkRealtime"
        private const val ONGOING_NOTIFICATION_ID = 20261
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "PanalinkRealtimeService Created")
        NotificationHelper.createNotificationChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "PanalinkRealtimeService Started")

        // 1. Show persistent foreground service notification on the SYSTEM channel with fallback
        try {
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    startForeground(
                        ONGOING_NOTIFICATION_ID, 
                        notification, 
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } catch (e: Throwable) {
                    startForeground(ONGOING_NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(ONGOING_NOTIFICATION_ID, notification)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to startForeground due to OS security restrictions. Falling back to background service.", e)
        }

        // 2. Start collecting all realtime events into Room
        startListeningToAllRealtimeEvents()

        // 3. Ensure the Supabase real-time connection is running
        SupabaseClient.connectRealtime()

        return START_STICKY
    }

    private var allRealtimeJobs: Job? = null

    private fun startListeningToAllRealtimeEvents() {
        if (allRealtimeJobs != null) return

        allRealtimeJobs = serviceScope.launch {
            // 1. Messages Flow
            launch {
                Log.d(TAG, "Subscribing to SupabaseClient.realtimeMessages flow...")
                SupabaseClient.realtimeMessages.collect { msg ->
                    val decryptedMsg = com.example.util.CryptoManager.decryptMessageIfNeeded(msg)
                    val msgsRepo = com.example.data.repository.MessagesRepository.getInstance()
                    val effectiveClearedAt = msgsRepo.getEffectiveClearedAt(decryptedMsg.chatId, null)
                    val shouldKeep = com.example.util.MessageFilter.shouldKeepMessage(
                        messageId = decryptedMsg.id,
                        messageClientUuid = decryptedMsg.clientMessageUuid,
                        messageCreatedAt = decryptedMsg.createdAt,
                        lastClearedAt = effectiveClearedAt,
                        deletedMessageIds = msgsRepo.getUserDeletedMessageIds()
                    )
                    if (shouldKeep) {
                        try {
                            val db = com.example.data.database.PanalinkDatabase.getDatabase(this@PanalinkRealtimeService)
                            db.messageDao().mergeAndSaveMessage(com.example.data.database.MessageEntity.fromMessage(decryptedMsg))
                            Log.d(TAG, "Saved/Merged realtime message in Room: ${decryptedMsg.id}")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error inserting realtime message into Room", e)
                        }
                        handleIncomingMessage(decryptedMsg)
                    } else {
                        Log.d(TAG, "Realtime message filtered out by MessageFilter: ${decryptedMsg.id}")
                    }
                }
            }

            // 2. Statuses (Reels/Stories) Flow
            launch {
                Log.d(TAG, "Subscribing to SupabaseClient.realtimeStatuses flow...")
                SupabaseClient.realtimeStatuses.collect { newState ->
                    try {
                        val db = com.example.data.database.PanalinkDatabase.getDatabase(this@PanalinkRealtimeService)
                        val statesDao = db.statesDao()
                        val currentUid = SupabaseClient.currentUser?.id
                        
                        val pubRepo = com.example.data.repository.PublicProfileRepository.getInstance(applicationContext)
                        val result = pubRepo.getPublicProfile(newState.userId)
                        val finalProfile = if (result is com.example.data.repository.PublicProfileFetchResult.Success) {
                            com.example.data.repository.PublicProfileResolver.toProfile(result.data)
                        } else if (newState.userId == currentUid && SupabaseClient.currentProfile != null) {
                            SupabaseClient.currentProfile!!
                        } else {
                            com.example.data.model.Profile(newState.userId, "Usuario", null)
                        }
                        
                        val entity = com.example.data.database.StateEntity.fromUserStateWithUser(
                            com.example.data.model.UserStateWithUser(newState, finalProfile)
                        )
                        statesDao.insertState(entity)
                        Log.d(TAG, "Saved resolved live status ${newState.id} in Room")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving live status to Room", e)
                    }
                }
            }

            // 3. Likes Flow
            launch {
                Log.d(TAG, "Subscribing to SupabaseClient.realtimeLikes flow...")
                SupabaseClient.realtimeLikes.collect { update ->
                    try {
                        val db = com.example.data.database.PanalinkDatabase.getDatabase(this@PanalinkRealtimeService)
                        val statesDao = db.statesDao()
                        val existing = statesDao.getStateById(update.statusId)
                        if (existing != null) {
                            val newCount = when (update.eventType) {
                                "INSERT" -> existing.likesCount + 1
                                "DELETE" -> maxOf(0, existing.likesCount - 1)
                                else -> existing.likesCount
                            }
                            if (newCount != existing.likesCount) {
                                statesDao.insertState(existing.copy(likesCount = newCount))
                                Log.d(TAG, "Updated live like for status ${update.statusId} in Room to $newCount")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating live like in Room", e)
                    }
                }
            }

            // 4. Comments Flow
            launch {
                Log.d(TAG, "Subscribing to SupabaseClient.realtimeComments flow...")
                SupabaseClient.realtimeComments.collect { update ->
                    try {
                        val db = com.example.data.database.PanalinkDatabase.getDatabase(this@PanalinkRealtimeService)
                        val statesDao = db.statesDao()
                        val existing = statesDao.getStateById(update.statusId)
                        if (existing != null) {
                            val newCount = when (update.eventType) {
                                "INSERT" -> existing.commentsCount + 1
                                "DELETE" -> maxOf(0, existing.commentsCount - 1)
                                else -> existing.commentsCount
                            }
                            if (newCount != existing.commentsCount) {
                                statesDao.insertState(existing.copy(commentsCount = newCount))
                                Log.d(TAG, "Updated live comment count for status ${update.statusId} in Room to $newCount")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating live comment count in Room", e)
                    }
                }
            }

            // 5. App Notifications Flow
            launch {
                Log.d(TAG, "Subscribing to SupabaseClient.realtimeNotifications flow...")
                SupabaseClient.realtimeNotifications.collect { dto ->
                    try {
                        val finalProfile = if (!dto.actorId.isNullOrEmpty()) {
                            val pubRepo = com.example.data.repository.PublicProfileRepository.getInstance(applicationContext)
                            val result = pubRepo.getPublicProfile(dto.actorId)
                            if (result is com.example.data.repository.PublicProfileFetchResult.Success) {
                                com.example.data.repository.PublicProfileResolver.toProfile(result.data)
                            } else null
                        } else null
                        
                        val domainNotif = dto.copy(actorProfile = finalProfile).toDomain()
                        com.example.data.repository.NotificationsRepository().addLocalNotification(domainNotif)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving realtime notification", e)
                    }
                }
            }
        }
    }

    private fun handleIncomingMessage(msg: Message) {
        val currentUserId = SupabaseClient.currentUser?.id ?: ""
        
        // Only notify on messages from other users
        if (msg.senderId == currentUserId || msg.senderId.isEmpty()) {
            Log.d(TAG, "Skipping notification: message sent by self or empty senderId.")
            return
        }

        serviceScope.launch {
            val db = com.example.data.database.PanalinkDatabase.getDatabase(this@PanalinkRealtimeService)
            
            // Create internal app notification (Message tab preview)
            try {
                val pubEntity = db.publicProfileDao().getById(msg.senderId)
                val senderProfile = pubEntity?.let { com.example.data.repository.PublicProfileResolver.toProfile(com.example.data.mapper.PublicProfileMapper.entityToModel(it)) }
                    ?: com.example.data.model.Profile(id = msg.senderId, displayName = "", avatarUrl = null)

                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", java.util.Locale.getDefault())
                sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                val newNotif = com.example.data.model.Notification(
                    id = "msg_${msg.id}",
                    type = com.example.data.model.NotificationType.MESSAGE,
                    sourceId = msg.chatId,
                    profile = senderProfile,
                    timestamp = sdf.format(java.util.Date()),
                    isRead = false,
                    actionText = "te envió un mensaje.",
                    previewText = msg.content
                )
                com.example.data.repository.NotificationsRepository().addLocalNotification(newNotif)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving message as notification", e)
            }

            val isChatMuted = try {
                db.chatDao().getChatById(msg.chatId)?.isMuted == true
            } catch (e: Exception) { false }

            if (isChatMuted) {
                Log.d(TAG, "Chat ${msg.chatId} is muted. Message received silently.")
                return@launch
            }

            val isChatActive = SupabaseClient.isChatScreenActive && SupabaseClient.activeChatId == msg.chatId

            if (isChatActive) {
                // User is actively reading this chat. Only play the sutil active chat sound (no status bar pop)
                NotificationHelper.playActiveChatSound(this@PanalinkRealtimeService)
                Log.d(TAG, "User is actively reading this chat. Skipping system notification pop, sutil active chat sound played.")
            } else {
                // Load sender profile avatar if possible to use in MessagingStyle
                val decryptedMsg = com.example.util.CryptoManager.decryptMessageIfNeeded(msg)
                val bodyText = (if (decryptedMsg.messageType == "sticker") "[Sticker]" else decryptedMsg.content) ?: "Nuevo mensaje"
                Log.d(TAG, "Incoming live message from ${msg.senderId}: $bodyText")

                var largeIconBitmap: android.graphics.Bitmap? = null
                try {
                    // Resolve identity asynchronously via PublicProfileRepository (Memory -> Room -> Supabase)
                    val publicProfileRepo = com.example.data.repository.PublicProfileRepository.getInstance(applicationContext)
                    val fetchResult = publicProfileRepo.getPublicProfile(msg.senderId)
                    val pubProfile = if (fetchResult is com.example.data.repository.PublicProfileFetchResult.Success) fetchResult.data else null

                    val cleanName = com.example.data.repository.PublicProfileResolver.resolveDisplayName(pubProfile)
                    val displayName = if (cleanName.isNotBlank()) cleanName else "Contacto"
                    val avatarUrl = pubProfile?.avatarUrl
                    
                    val loadUrl = msg.thumbnailUrl ?: msg.mediaUrl ?: avatarUrl
                    if (!loadUrl.isNullOrEmpty()) {
                        val request = ImageRequest.Builder(this@PanalinkRealtimeService)
                            .data(loadUrl)
                            .size(512, 512)
                            .build()
                        val result = imageLoader.execute(request)
                        largeIconBitmap = result.drawable?.toBitmap()
                    }

                    // Show status bar system notification + sound/vibrate using NotificationHelper
                    NotificationHelper.showNotification(
                        context = this@PanalinkRealtimeService,
                        title = displayName,
                        body = bodyText,
                        chatId = msg.chatId,
                        channelId = NotificationHelper.CHANNEL_MESSAGES,
                        largeIcon = largeIconBitmap,
                        imageUrl = msg.thumbnailUrl ?: msg.mediaUrl,
                        senderName = displayName
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing incoming message for notification", e)
                }
            }
        }
    }

    private fun buildForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_SYSTEM)
            .setContentTitle("Panalink Activo ⚡")
            .setContentText("Conectado a la red en tiempo real de Supabase")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "PanalinkRealtimeService Destroyed")
        messageCollectionJob?.cancel()
        allRealtimeJobs?.cancel()
        serviceScope.cancel()
    }
}
