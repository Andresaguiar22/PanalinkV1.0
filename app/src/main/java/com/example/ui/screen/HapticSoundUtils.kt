package com.example.ui.screen

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.util.PanaLinkSoundManager
import com.example.util.PanaSoundEvent

fun triggerLightVibration(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(30, 50))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    } catch (e: Exception) {}
}

fun playPanaSound(context: Context, event: PanaSoundEvent) {
    PanaLinkSoundManager.play(context, event)
}

fun playShortBeep(context: Context? = null) {
    if (context != null) {
        PanaLinkSoundManager.play(context, PanaSoundEvent.VOICE_START)
    }
}

fun playCancelBeep(context: Context? = null) {
    if (context != null) {
        PanaLinkSoundManager.play(context, PanaSoundEvent.VOICE_CANCEL)
    }
}

fun playSendBeep(context: Context? = null) {
    if (context != null) {
        PanaLinkSoundManager.play(context, PanaSoundEvent.VOICE_SEND)
    }
}

