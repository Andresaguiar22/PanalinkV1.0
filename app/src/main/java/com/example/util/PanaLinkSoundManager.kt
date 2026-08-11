package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import com.example.R

enum class PanaSoundEvent {
    VOICE_START,
    VOICE_LOCK,
    VOICE_CANCEL,
    VOICE_SEND,
    MESSAGE_SEND,
    MESSAGE_RECEIVED,
    MESSAGE_READ
}

class PanaLinkSoundManager private constructor(context: Context) {

    private val TAG = "PanaLinkSoundManager"
    private val appContext = context.applicationContext
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<PanaSoundEvent, Int>()
    private var isLoaded = false

    init {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            soundPool = SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build()

            soundPool?.let { pool ->
                soundMap[PanaSoundEvent.VOICE_START] = pool.load(appContext, R.raw.pana_voice_start, 1)
                soundMap[PanaSoundEvent.VOICE_LOCK] = pool.load(appContext, R.raw.pana_voice_lock, 1)
                soundMap[PanaSoundEvent.VOICE_CANCEL] = pool.load(appContext, R.raw.pana_voice_cancel, 1)
                soundMap[PanaSoundEvent.VOICE_SEND] = pool.load(appContext, R.raw.pana_voice_send, 1)
                soundMap[PanaSoundEvent.MESSAGE_SEND] = pool.load(appContext, R.raw.pana_message_send, 1)
                soundMap[PanaSoundEvent.MESSAGE_RECEIVED] = pool.load(appContext, R.raw.pana_message_received, 1)
                soundMap[PanaSoundEvent.MESSAGE_READ] = pool.load(appContext, R.raw.pana_message_read, 1)
            }
            isLoaded = true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SoundPool", e)
        }
    }

    fun playSound(event: PanaSoundEvent) {
        try {
            val soundId = soundMap[event] ?: return
            if (soundId != 0) {
                soundPool?.play(soundId, 0.8f, 0.8f, 1, 0, 1.0f)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error playing sound event: $event", e)
        }
    }

    fun release() {
        try {
            soundPool?.release()
            soundPool = null
            soundMap.clear()
            isLoaded = false
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing SoundPool", e)
        }
    }

    companion object {
        @Volatile
        private var instance: PanaLinkSoundManager? = null

        fun getInstance(context: Context): PanaLinkSoundManager {
            return instance ?: synchronized(this) {
                instance ?: PanaLinkSoundManager(context).also { instance = it }
            }
        }

        fun play(context: Context, event: PanaSoundEvent) {
            getInstance(context).playSound(event)
        }
    }
}
