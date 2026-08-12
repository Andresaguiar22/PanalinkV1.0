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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun smartMerge_likeUnlikeLikeUnlike_leavesOneFinalIntent() = runTest {
        val targetId = "reel_A"

        dao.insertAction(action("1", targetId, "LIKE"))
        dao.insertAction(action("2", targetId, "UNLIKE"))
        dao.insertAction(action("3", targetId, "LIKE"))
        dao.insertAction(action("4", targetId, "UNLIKE"))

        val pending = dao.getPendingActions()

        assertEquals(1, pending.size)
        assertEquals("LIKE", pending.single().actionFamily)
        assertFalse(pending.single().desiredState!!)
        assertEquals("UNLIKE", pending.single().actionType)
        assertEquals("1", pending.single().localActionId)
        assertEquals(4L, pending.single().revision)
    }

    @Test
    fun smartMerge_preservesIdentity_andAdvancesRevision() = runTest {
        val targetId = "reel_identity"

        dao.insertAction(action("original", targetId, "LIKE"))
        val first = dao.getPendingActions().single()

        dao.insertAction(action("ignored-new-id", targetId, "UNLIKE"))
        val second = dao.getPendingActions().single()

        assertEquals("original", second.localActionId)
        assertNotEquals(first.revision, second.revision)
        assertEquals(first.revision + 1, second.revision)
        assertEquals(false, second.desiredState)
    }

    @Test
    fun staleSnapshot_cannotDelete_newerSameStateIntent() = runTest {
        val targetId = "reel_occ"

        dao.insertAction(action("worker", targetId, "LIKE"))
        val stale = dao.getPendingActions().single()

        // New user intent returns to the same state. desiredState alone would
        // be insufficient; revision must distinguish the two snapshots.
        dao.insertAction(action("new-intent", targetId, "UNLIKE"))
        dao.insertAction(action("newer-intent", targetId, "LIKE"))
        val current = dao.getPendingActions().single()

        val deleted = dao.deleteIfStillCurrent(
            id = stale.localActionId,
            family = stale.actionFamily!!,
            desiredState = stale.desiredState!!,
            revision = stale.revision
        )

        assertEquals(0, deleted)
        assertTrue(current.revision > stale.revision)
        assertEquals(1, dao.getPendingActions().size)
        assertEquals(true, dao.getPendingActions().single().desiredState)
    }

    @Test
    fun hybridQueue_collapsesState_butPreservesEvents() = runTest {
        val targetId = "reel_hybrid"

        dao.insertAction(action("like-1", targetId, "LIKE"))
        dao.insertAction(action("like-2", targetId, "UNLIKE"))
        dao.insertAction(action("share-1", targetId, "SHARE"))
        dao.insertAction(action("share-2", targetId, "SHARE"))

        val pending = dao.getPendingActions()

        assertEquals(3, pending.size)
        assertEquals(1, pending.count { it.actionFamily == "LIKE" })
        assertEquals(2, pending.count { it.actionType == "SHARE" })
    }
}
