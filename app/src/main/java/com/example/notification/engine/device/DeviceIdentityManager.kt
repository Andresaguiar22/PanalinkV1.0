package com.example.notification.engine.device

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.Keep
import com.example.notification.engine.analytics.NotificationAnalyticsEngine
import java.util.UUID

@Keep
class DeviceIdentityManager private constructor() {

    private var sharedPrefs: SharedPreferences? = null

    fun initialize(context: Context) {
        if (sharedPrefs == null) {
            sharedPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getDeviceId(context: Context? = null): String {
        context?.let { initialize(it) }
        val prefs = sharedPrefs
        if (prefs == null) {
            Log.w(TAG, "DeviceIdentityManager not initialized with context, generating volatile device ID")
            return "android_dev_${UUID.randomUUID()}"
        }

        val deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId.isNullOrBlank()) {
            val newId = "android_dev_${UUID.randomUUID()}"
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            Log.d(TAG, "Generated and persisted new stable Device ID: $newId")
            return newId
        }
        return deviceId
    }

    fun getFallbackToken(context: Context? = null): String {
        context?.let { initialize(it) }
        val prefs = sharedPrefs
        if (prefs == null) {
            return "device_fallback_${UUID.randomUUID()}"
        }

        val token = prefs.getString(KEY_FALLBACK_TOKEN, null)
        if (token.isNullOrBlank()) {
            val newToken = "device_fallback_${UUID.randomUUID()}"
            prefs.edit().putString(KEY_FALLBACK_TOKEN, newToken).apply()
            NotificationAnalyticsEngine.getInstance().recordNotificationIgnored() // Analytics fallback event
            Log.d(TAG, "Created and persisted stable fallback push token: $newToken")
            return newToken
        }
        return token
    }

    fun getPushToken(context: Context? = null, fcmToken: String? = null): String {
        if (!fcmToken.isNullOrBlank()) {
            context?.let { initialize(it) }
            sharedPrefs?.edit()?.putString(KEY_REAL_FCM_TOKEN, fcmToken)?.apply()
            return fcmToken
        }

        // Return stored FCM token if present, otherwise persistent fallback token
        val storedFcm = sharedPrefs?.getString(KEY_REAL_FCM_TOKEN, null)
        if (!storedFcm.isNullOrBlank()) {
            return storedFcm
        }

        return getFallbackToken(context)
    }

    fun isFallbackToken(token: String): Boolean {
        return token.startsWith("device_fallback_")
    }

    fun buildRegistration(context: Context, userId: String, fcmToken: String? = null): DeviceRegistration {
        val pushToken = getPushToken(context, fcmToken)
        val tokenType = if (isFallbackToken(pushToken)) TokenType.FALLBACK else TokenType.FCM
        return DeviceRegistration(
            deviceId = getDeviceId(context),
            userId = userId,
            pushToken = pushToken,
            tokenType = tokenType
        )
    }

    companion object {
        private const val TAG = "DeviceIdentityManager"
        private const val PREFS_NAME = "panalink_device_identity_prefs"
        private const val KEY_DEVICE_ID = "panalink_device_id"
        private const val KEY_FALLBACK_TOKEN = "panalink_fallback_push_token"
        private const val KEY_REAL_FCM_TOKEN = "panalink_real_fcm_token"

        @Volatile
        private var instance: DeviceIdentityManager? = null

        fun getInstance(): DeviceIdentityManager {
            return instance ?: synchronized(this) {
                instance ?: DeviceIdentityManager().also { instance = it }
            }
        }
    }
}
