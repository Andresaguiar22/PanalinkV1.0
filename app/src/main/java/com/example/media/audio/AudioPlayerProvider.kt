package com.example.media.audio

import android.content.Context

/**
 * P6.7.3 - Audio Player Provider
 * Singleton provider for AudioPlayerEngine and related controllers.
 */
object AudioPlayerProvider {
    private var instance: AudioPlayerEngine? = null
    private var effectsController: AudioEffectsController? = null

    fun getPlayerEngine(context: Context): AudioPlayerEngine {
        return instance ?: synchronized(this) {
            instance ?: AudioPlayerEngine(context.applicationContext).also {
                instance = it
                effectsController = AudioEffectsController(it.getExoPlayer())
            }
        }
    }

    fun getEffectsController(): AudioEffectsController? = effectsController
}
