package com.example.notification.engine.core

import androidx.annotation.Keep

/**
 * Contextual environment state passed along with Notification Events through the pipeline.
 * Contains purely passive state variables (e.g., active screen, foreground status, active chat).
 */
@Keep
data class NotificationContext(
    val isAppInForeground: Boolean = true,
    val currentUserId: String? = null,
    val activeScreenRoute: String? = null,
    val activeChatId: String? = null,
    val currentTimeMillis: Long = System.currentTimeMillis()
)
