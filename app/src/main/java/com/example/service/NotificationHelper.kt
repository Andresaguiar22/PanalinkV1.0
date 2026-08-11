package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity

object NotificationHelper {
    private const val TAG = "NotificationHelper"

    // Mandatory Channel IDs
    const val CHANNEL_MESSAGES = "panalink_messages_v3"
    const val CHANNEL_CALLS = "panalink_calls_v3"
    const val CHANNEL_SYSTEM = "panalink_system"
    const val CHANNEL_ALERTS = "panalink_alerts_v3"

    // Create the four required Notification Channels with High Priority where appropriate
    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // 1. Messages Channel (WhatsApp-like, High Importance)
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Mensajes de Pana 💬",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de chats y mensajes entrantes de Panalink"
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttributes)
            }

            // 2. Calls Channel (Incoming calls, High Importance)
            val callsChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Llamadas 📞",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de llamadas entrantes de Panalink"
                enableLights(true)
                lightColor = android.graphics.Color.BLUE
                enableVibration(true)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttributes)
            }

            // 3. System Channel (Low Importance ongoing foreground tasks)
            val systemChannel = NotificationChannel(
                CHANNEL_SYSTEM,
                "Servicio de Sistema ⚙️",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Estado de sincronización y conexión en segundo plano de Panalink"
                setShowBadge(false)
            }

            // 4. Alerts Channel (High priority alerts, security/important notices)
            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Alertas Importantes 🚨",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones críticas de seguridad y alertas importantes de Panalink"
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttributes)
            }

            manager.createNotificationChannel(messagesChannel)
            manager.createNotificationChannel(callsChannel)
            manager.createNotificationChannel(systemChannel)
            manager.createNotificationChannel(alertsChannel)

            Log.d(TAG, "All four Notification Channels created/updated successfully")
        }
    }

    // Play selected custom notification sound (System default or synthetic tones)
    fun playNotificationSound(context: Context) {
        val prefs = context.getSharedPreferences("panalink_prefs", Context.MODE_PRIVATE)
        val soundEnabled = prefs.getBoolean("notifications_sound_enabled", true)
        if (!soundEnabled) {
            Log.d(TAG, "Notification sound is disabled in settings.")
            return
        }

        val toneType = prefs.getString("notifications_sound_tone", "default") ?: "default"
        if (toneType == "silent") {
            Log.d(TAG, "Notification tone is set to silent.")
            return
        }

        try {
            when (toneType) {
                "default" -> {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(context, defaultUri)
                    ringtone?.play()
                    Log.d(TAG, "Played default system notification sound")
                }
                "pana_beep" -> playSyntheticTone(ToneGenerator.TONE_PROP_BEEP, 120)
                "pana_double" -> playSyntheticTone(ToneGenerator.TONE_PROP_BEEP2, 250)
                "pana_pip" -> playSyntheticTone(ToneGenerator.TONE_CDMA_PIP, 150)
                "pana_high" -> playSyntheticTone(ToneGenerator.TONE_CDMA_HIGH_L, 200)
                else -> {
                    val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val ringtone = RingtoneManager.getRingtone(context, defaultUri)
                    ringtone?.play()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play custom notification sound", e)
        }
    }

    // Programmatically generates clean, high-quality audio beeps using ToneGenerator
    private fun playSyntheticTone(toneType: Int, duration: Int) {
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            tg.startTone(toneType, duration)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    tg.release()
                } catch (e: Exception) {
                    // Ignore release errors
                }
            }, (duration + 100).toLong())
        } catch (e: Exception) {
            Log.e(TAG, "Error generating synthetic tone", e)
        }
    }

    // Gentle sound inside active chat using PanaLinkSoundManager
    fun playActiveChatSound(context: Context) {
        val prefs = context.getSharedPreferences("panalink_prefs", Context.MODE_PRIVATE)
        val globalEnabled = prefs.getBoolean("notifications_global_enabled", true)
        if (!globalEnabled) return

        val chatSoundEnabled = prefs.getBoolean("notifications_chat_sound_enabled", true)
        if (!chatSoundEnabled) return

        com.example.util.PanaLinkSoundManager.play(context, com.example.util.PanaSoundEvent.MESSAGE_RECEIVED)
    }

    // Gentle sound for sending your own messages
    fun playOutgoingSound(context: Context) {
        val prefs = context.getSharedPreferences("panalink_prefs", Context.MODE_PRIVATE)
        val outgoingEnabled = prefs.getBoolean("notifications_outgoing_sound_enabled", true)
        if (!outgoingEnabled) return

        com.example.util.PanaLinkSoundManager.play(context, com.example.util.PanaSoundEvent.MESSAGE_SEND)
    }

    fun playReadSound(context: Context) {
        val prefs = context.getSharedPreferences("panalink_prefs", Context.MODE_PRIVATE)
        val globalEnabled = prefs.getBoolean("notifications_global_enabled", true)
        if (!globalEnabled) return

        com.example.util.PanaLinkSoundManager.play(context, com.example.util.PanaSoundEvent.MESSAGE_READ)
    }

    private fun playWaterDrop() {
        val sampleRate = 44100
        val durationMs = 120
        val numSamples = (durationMs * sampleRate) / 1000
        val sample = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Upward frequency sweep (chirp) from 950Hz to 1750Hz
            val freq = 950.0 + (800.0 * (i.toDouble() / numSamples))
            val angle = 2.0 * Math.PI * freq * t
            
            // Fast exponential decay envelope
            val envelope = Math.exp(-5.0 * (i.toDouble() / numSamples))
            sample[i] = (Math.sin(angle) * 30000.0 * envelope).toInt().toShort()
        }
        playBuffer(sample, sampleRate)
    }

    private fun playSoftPop() {
        val sampleRate = 44100
        val durationMs = 40
        val numSamples = (durationMs * sampleRate) / 1000
        val sample = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Pleasant clear 550Hz frequency
            val freq = 550.0
            val angle = 2.0 * Math.PI * freq * t
            
            // Quadratic decay envelope for smoother sound
            val progress = i.toDouble() / numSamples
            val envelope = (1.0 - progress) * (1.0 - progress)
            sample[i] = (Math.sin(angle) * 22000.0 * envelope).toInt().toShort()
        }
        playBuffer(sample, sampleRate)
    }

    private fun playSwoosh() {
        val sampleRate = 44100
        val durationMs = 140
        val numSamples = (durationMs * sampleRate) / 1000
        val sample = ShortArray(numSamples)
        
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Downward sweep from 1400Hz to 400Hz (creates a nice swoosh)
            val freq = 1400.0 - (1000.0 * (i.toDouble() / numSamples))
            val angle = 2.0 * Math.PI * freq * t
            
            // Exponential decay envelope
            val envelope = Math.exp(-4.2 * (i.toDouble() / numSamples))
            sample[i] = (Math.sin(angle) * 18000.0 * envelope).toInt().toShort()
        }
        playBuffer(sample, sampleRate)
    }

    private fun playBuffer(sample: ShortArray, sampleRate: Int) {
        Thread {
            var audioTrack: AudioTrack? = null
            try {
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                val bufferSize = Math.max(minBufferSize, sample.size * 2)
                
                audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(
                            android.media.AudioAttributes.Builder()
                                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_COMMUNICATION_INSTANT)
                                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STREAM)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_NOTIFICATION,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STREAM
                    )
                }
                
                audioTrack.play()
                audioTrack.write(sample, 0, sample.size)
                
                Thread.sleep(sample.size * 1000L / sampleRate + 50)
                audioTrack.stop()
            } catch (e: Exception) {
                Log.e(TAG, "Error playing synthetic audio", e)
            } finally {
                try {
                    audioTrack?.release()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }.start()
    }

    // Vibrate device based on user preference patterns (short, long, double, triple)
    fun triggerVibration(context: Context) {
        val prefs = context.getSharedPreferences("panalink_prefs", Context.MODE_PRIVATE)
        val vibrationEnabled = prefs.getBoolean("notifications_vibration_enabled", true)
        if (!vibrationEnabled) {
            Log.d(TAG, "Vibration is disabled in settings.")
            return
        }

        val patternType = prefs.getString("notifications_vibration_pattern", "default") ?: "default"

        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                manager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (!vibrator.hasVibrator()) {
                Log.d(TAG, "Device has no vibrator hardware.")
                return
            }

            val pattern = when (patternType) {
                "short" -> longArrayOf(0, 100)
                "long" -> longArrayOf(0, 600)
                "double" -> longArrayOf(0, 120, 100, 120)
                "triple" -> longArrayOf(0, 80, 80, 80, 80, 80)
                else -> longArrayOf(0, 200) // Default medium vibration
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
            Log.d(TAG, "Triggered vibration pattern: $patternType")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger vibration", e)
        }
    }

    /**
     * Highly optimized helper to generate a circular bitmap with user initials.
     * Perfect for fallback when no avatar URL is available or loading fails.
     */
    fun createInitialsBitmap(context: Context, name: String, size: Int = 128): android.graphics.Bitmap {
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Use a consistent background color based on name hash for familiarity
        val colors = intArrayOf(
            0xFF1ABC9C.toInt(), 0xFF2ECC71.toInt(), 0xFF3498DB.toInt(), 
            0xFF9B59B6.toInt(), 0xFF34495E.toInt(), 0xFF16A085.toInt(), 
            0xFF27AE60.toInt(), 0xFF2980B9.toInt(), 0xFF8E44AD.toInt(), 
            0xFF2C3E50.toInt(), 0xFFF1C40F.toInt(), 0xFFE67E22.toInt(), 
            0xFFE74C3C.toInt(), 0xFF95A5A6.toInt(), 0xFFF39C12.toInt(), 
            0xFFD35400.toInt(), 0xFFC0392B.toInt(), 0xFFBDC3C7.toInt(), 0xFF7F8C8D.toInt()
        )
        val bgColor = colors[Math.abs(name.hashCode()) % colors.size]
        
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        paint.color = bgColor
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        
        val initials = name.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it[0].uppercaseChar() }
            .joinToString("")
            
        paint.color = android.graphics.Color.WHITE
        paint.textSize = size / 2.2f
        paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        
        val fontMetrics = paint.fontMetrics
        val y = (size / 2f) - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(initials, size / 2f, y, paint)
        
        return bitmap
    }

    // Unified helper to build and post manual notification with custom vibration, sound & grouping
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        chatId: String = "",
        stateId: String = "",
        notificationType: String = "new_message",
        channelId: String = CHANNEL_MESSAGES,
        extras: Map<String, String> = emptyMap(),
        imageUrl: String? = null,
        largeIcon: android.graphics.Bitmap? = null,
        senderName: String? = null
    ) {
        val prefs = context.getSharedPreferences("panalink_prefs", Context.MODE_PRIVATE)
        val globalEnabled = prefs.getBoolean("notifications_global_enabled", true)
        if (!globalEnabled) {
            Log.d(TAG, "Notifications are globally disabled in settings. Skipping showNotification.")
            return
        }

        val soundEnabled = prefs.getBoolean("notifications_sound_enabled", true)
        val toneType = prefs.getString("notifications_sound_tone", "default") ?: "default"

        // Trigger vibration programmatically for maximum consistency
        triggerVibration(context)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open MainActivity and launch the specific content when clicked
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("chatId", chatId)
            putExtra("chat_id", chatId)
            putExtra("stateId", stateId)
            putExtra("state_id", stateId)
            putExtra("notificationType", notificationType)
            putExtra("notification_type", notificationType)
            extras.forEach { (key, value) ->
                putExtra(key, value)
            }
        }

        val uniqueRequestCode = when {
            chatId.isNotEmpty() -> chatId.hashCode()
            stateId.isNotEmpty() -> stateId.hashCode()
            else -> System.currentTimeMillis().toInt()
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            uniqueRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (notificationType == "new_message") {
            val finalSenderName = senderName ?: title
            val finalIcon = largeIcon ?: createInitialsBitmap(context, finalSenderName)
            
            val senderPerson = Person.Builder()
                .setName(finalSenderName)
                .setIcon(IconCompat.createWithBitmap(finalIcon))
                .build()

            val messagingStyle = NotificationCompat.MessagingStyle(senderPerson)
                .setConversationTitle(if (chatId.isNotEmpty() && senderName != null) null else title)
                .addMessage(body, System.currentTimeMillis(), senderPerson)
            
            builder.setStyle(messagingStyle)
            builder.setLargeIcon(finalIcon)
        } else {
            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon)
            }

            if (!imageUrl.isNullOrEmpty() && largeIcon != null) {
                builder.setStyle(NotificationCompat.BigPictureStyle()
                    .bigPicture(largeIcon)
                    .bigLargeIcon(null as android.graphics.Bitmap?)
                    .setSummaryText(body))
            } else {
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
            }
        }

        if (notificationType == "llamada_entrante") {
            builder.setCategory(NotificationCompat.CATEGORY_CALL)
            builder.setFullScreenIntent(pendingIntent, true)
            builder.setOngoing(true)
        } else {
            builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
        }

        if (soundEnabled) {
            if (toneType == "default") {
                val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                builder.setSound(defaultSoundUri)
            } else if (toneType != "silent") {
                // Play custom synthetic tone programmatically, keep system silent to prevent double play
                playNotificationSound(context)
                builder.setSound(null)
            } else {
                builder.setSound(null)
            }
        } else {
            builder.setSound(null)
        }

        builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)

        // Unique notification ID to prevent overwrite unless they are part of the same chat/story group
        val notificationId = uniqueRequestCode
        
        try {
            notificationManager.notify(notificationId, builder.build())
            Log.d(TAG, "Manually posted notification ID $notificationId to channel $channelId, type: $notificationType")
        } catch (e: Exception) {
            Log.e(TAG, "Error posting notification to manager", e)
        }
    }
}
