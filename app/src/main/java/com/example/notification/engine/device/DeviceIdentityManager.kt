package com.example.notification.engine.device

import android.content.Context
import androidx.annotation.Keep

@Keep
object DeviceIdentityManager {
    fun initialize(context: Context) {
        // No-op stub
    }
    
    fun getInstance(): DeviceIdentityManager = this

    fun getDeviceId(context: Context): String {
        return "unknown_device"
    }

    fun getPushToken(context: Context, token: String? = null): String {
        return token ?: "no_token"
    }
}
