package com.example.ui.screen

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

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

fun playShortBeep() {
    try {
        val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
        toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
        Handler(Looper.getMainLooper()).postDelayed({
            toneG.release()
        }, 100)
    } catch (e: Exception) {}
}

fun playCancelBeep() {
    try {
        val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
        toneG.startTone(ToneGenerator.TONE_PROP_NACK, 100)
        Handler(Looper.getMainLooper()).postDelayed({
            toneG.release()
        }, 150)
    } catch (e: Exception) {}
}

fun playSendBeep() {
    try {
        val toneG = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
        toneG.startTone(ToneGenerator.TONE_PROP_ACK, 70)
        Handler(Looper.getMainLooper()).postDelayed({
            toneG.release()
        }, 120)
    } catch (e: Exception) {}
}
