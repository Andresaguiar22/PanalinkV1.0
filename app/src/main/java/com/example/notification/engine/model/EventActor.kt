package com.example.notification.engine.model

import androidx.annotation.Keep

/**
 * Encapsulates the actor (user or system entity) who initiated the notification event.
 */
@Keep
data class EventActor(
    val id: String,
    val name: String,
    val username: String? = null,
    val avatarUrl: String? = null,
    val isVerified: Boolean = false
)
