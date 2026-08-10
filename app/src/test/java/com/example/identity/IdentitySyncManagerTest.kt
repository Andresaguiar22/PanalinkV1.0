package com.example.identity

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.identity.model.ProfileSyncState
import com.example.identity.repository.IdentityRepository
import com.example.identity.storage.AvatarStorageManager
import com.example.identity.storage.CoverStorageManager
import com.example.identity.sync.IdentitySyncManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IdentitySyncManagerTest {

    private lateinit var context: Context
    private lateinit var syncManager: IdentitySyncManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val repo = IdentityRepository(context)
        val avatars = AvatarStorageManager(context)
        val covers = CoverStorageManager(context)
        syncManager = IdentitySyncManager(context, repo, avatars, covers)
    }

    @Test
    fun testSyncProfile() = runBlocking {
        // Simple test to ensure the sync manager doesn't crash and returns a state
        val state = syncManager.syncProfile("some_user_id")
        assertEquals(ProfileSyncState.SUCCESS, state)
    }
}
