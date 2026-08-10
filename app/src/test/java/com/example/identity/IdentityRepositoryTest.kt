package com.example.identity

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.PanalinkDatabase
import com.example.data.model.Profile
import com.example.identity.model.CachedProfile
import com.example.identity.model.ProfileUpdateResult
import com.example.identity.repository.IdentityRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IdentityRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: PanalinkDatabase
    private lateinit var repository: IdentityRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PanalinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = IdentityRepository(context)
        // Note: For a proper isolated test, we would inject this specific DB instance into the Repository
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testSaveAndGetProfile() = runBlocking {
        val profile = Profile(
            id = "user_1",
            displayName = "User One",
            avatarUrl = null
        )
        val cached = CachedProfile(
            profile = profile,
            avatarLocalPath = "/files/avatars/users/user_1.webp",
            isDirty = true
        )

        // Save
        val result = repository.saveProfile(cached)
        assertTrue(result is ProfileUpdateResult.Success)

        // Get
        val retrieved = repository.getProfile("user_1")
        assertNotNull(retrieved)
        assertEquals("User One", retrieved?.profile?.displayName)
        assertEquals("/files/avatars/users/user_1.webp", retrieved?.avatarLocalPath)
        assertTrue(retrieved?.isDirty == true)
    }
}
