package com.example.notification.engine.device

import androidx.annotation.Keep

@Keep
enum class TokenType {
    FCM,
    FALLBACK
}

@Keep
data class DeviceRegistration(
    val deviceId: String,
    val userId: String,
    val pushToken: String,
    val tokenType: TokenType,
    val platform: String = "android",
    val lastSeen: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
