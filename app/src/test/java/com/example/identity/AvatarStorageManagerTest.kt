package com.example.identity

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.identity.model.AvatarDownloadResult
import com.example.identity.storage.AvatarStorageManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AvatarStorageManagerTest {

    private lateinit var context: Context
    private lateinit var storageManager: AvatarStorageManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        storageManager = AvatarStorageManager(context)
    }

    @Test
    fun testSaveAndCheckAvatar() = runBlocking {
        val userId = "test_user_123"
        val bytes = "fake_image_data".toByteArray()

        val result = storageManager.saveAvatar(userId, bytes)
        assertTrue(result is AvatarDownloadResult.Success)

        val exists = storageManager.avatarExists(userId)
        assertTrue(exists)

        val deleted = storageManager.deleteAvatar(userId)
        assertTrue(deleted)

        val existsAfterDelete = storageManager.avatarExists(userId)
        assertFalse(existsAfterDelete)
    }
}
