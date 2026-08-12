package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.PanalinkDatabase
import com.example.data.database.PendingSocialActionEntity
import com.example.data.database.PendingSocialActionDao
import com.example.util.LogSanitizer
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConsolidationDataTest {

    private lateinit var db: PanalinkDatabase
    private lateinit var dao: PendingSocialActionDao

    @Before
    fun setupRoom() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context,
            PanalinkDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.pendingSocialActionDao()
    }

    @After
    fun tearDownRoom() {
        db.close()
    }

    @Test
    fun testLogSanitizerRedactsBearerAndJwt() {
        val input = "Header: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature token=secret_token_123"
        val sanitized = LogSanitizer.sanitize(input)

        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
        assertFalse(sanitized.contains("secret_token_123"))
        assertEquals("Header: Bearer [REDACTED_JWT] token=[REDACTED]", sanitized)
    }

    @Test
    fun testLogSanitizerHandlesNullOrEmpty() {
        assertEquals("", LogSanitizer.sanitize(null))
        assertEquals("", LogSanitizer.sanitize(""))
    }

    private fun action(
        id: String,
        targetId: String,
        actionType: String,
        userId: String = "user_456",
        isReel: Boolean = true
    ) = PendingSocialActionEntity(
        localActionId = id,
        userId = userId,
        targetId = targetId,
        actionType = actionType,
        payload = null,
        isReel = isReel
    )

    // Regression contract: LIKE/UNLIKE is expected to collapse to one
    // logical desired-state intent after the v43 refactor. Against v42,
    // each action has a distinct localActionId and therefore persists as
    // its own row; this test must fail with Expected: 1, Actual: 4.
    @Test
    fun current_behavior_does_not_collapse_interactions() = runTest {
        val targetId = "reel_A"

        dao.insertAction(action("1", targetId, "LIKE"))
        dao.insertAction(action("2", targetId, "UNLIKE"))
        dao.insertAction(action("3", targetId, "LIKE"))
        dao.insertAction(action("4", targetId, "UNLIKE"))

        val pending = dao.getPendingActions()

        assertEquals(1, pending.size)
    }
}
