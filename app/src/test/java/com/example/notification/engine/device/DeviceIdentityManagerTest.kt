package com.example.notification.engine.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceIdentityManagerTest {

    @Test
    fun testIsFallbackTokenIdentification() {
        val identityManager = DeviceIdentityManager.getInstance()
        
        val fallbackToken = "device_fallback_12345-abcde-67890"
        val fcmToken = "fcm_token_real_xyz_98765"

        assertTrue("Should detect fallback token", identityManager.isFallbackToken(fallbackToken))
        assertFalse("Should not mark real FCM token as fallback", identityManager.isFallbackToken(fcmToken))
    }

    @Test
    fun testDeviceRegistrationModel() {
        val registration = DeviceRegistration(
            deviceId = "android_dev_test_id",
            userId = "user_test_123",
            pushToken = "device_fallback_test_token",
            tokenType = TokenType.FALLBACK
        )

        assertNotNull(registration)
        assertEquals("android_dev_test_id", registration.deviceId)
        assertEquals("user_test_123", registration.userId)
        assertEquals(TokenType.FALLBACK, registration.tokenType)
        assertEquals("android", registration.platform)
        assertTrue(registration.isActive)
    }
}
