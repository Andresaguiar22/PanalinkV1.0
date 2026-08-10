package com.example.notification.engine.model

import androidx.annotation.Keep

/**
 * Defines how aggressively a notification interrupts the user across OS surface layers.
 * Decouples technical priority from user UI presentation / disturbance.
 */
@Keep
enum class InterruptivenessLevel {
    /** Opens full screen intent (e.g. Active WebRTC Incoming Call UI). */
    FULLSCREEN,

    /** High priority heads-up visual banner with sound/vibration. */
    HEADS_UP,

    /** Audible tone / haptic vibration without displaying a drop-down visual heads-up banner. */
    SOUND_ONLY,

    /** Appears quietly in the Android status bar and shade without sound or popup. */
    STATUS_BAR_ONLY,

    /** Recorded only in internal Notification Center & Badge counts; no Android OS notification shown. */
    IN_APP_ONLY,

    /** Processed internally by the Engine with zero UI disturbance. */
    SILENT
}
