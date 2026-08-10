package com.example.media.playlist

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.PanalinkDatabase
import com.example.media.sync.FakeSupabaseApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistInvitationFlowTest {

    private lateinit var db: PanalinkDatabase
    private lateinit var invitationRepo: PlaylistInvitationRepository
    private lateinit var api: FakeSupabaseApi
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, PanalinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        api = FakeSupabaseApi()
        invitationRepo = PlaylistInvitationRepository(db.invitationDao(), api, "fake-key")
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun createInvitationOffline_isStoredWithDirtyFlag() = runBlocking {
        val invite = invitationRepo.createInvitationLocally(
            playlistId = "p1",
            senderId = "u1",
            receiverId = "u2",
            role = "EDITOR"
        )

        val stored = invitationRepo.getInvitationById(invite.id)
        assertNotNull(stored)
        assertEquals("PENDING", stored?.status)
        assertTrue(stored?.isDirty ?: false)
    }

    @Test
    fun syncUnsyncedInvitations_callsApiAndClearsDirty() = runBlocking {
        invitationRepo.createInvitationLocally("p1", "u1", "u2", "EDITOR")
        
        invitationRepo.syncLocalToRemote("Bearer token")

        val unsynced = db.invitationDao().getUnsyncedInvitations()
        assertTrue(unsynced.isEmpty())
    }

    @Test
    fun avoidDuplicateInvitations() = runBlocking {
        invitationRepo.createInvitationLocally("p1", "u1", "u2", "EDITOR")
        invitationRepo.createInvitationLocally("p1", "u1", "u2", "VIEWER")

        val all = invitationRepo.observeInvitationsForPlaylist("p1").first()
        assertEquals(1, all.size)
    }

    @Test
    fun acceptInvitation_callsRpcAndUpdatesLocalStatus() = runBlocking {
        val invite = invitationRepo.createInvitationLocally("p1", "u1", "u2", "EDITOR")
        
        val result = invitationRepo.acceptInvitation(invite.id, "Bearer token")
        
        assertTrue(result.isSuccess)
        val updated = invitationRepo.getInvitationById(invite.id)
        assertEquals("ACCEPTED", updated?.status)
    }

    @Test
    fun rejectInvitation_updatesStatusToRejected() = runBlocking {
        val invite = invitationRepo.createInvitationLocally("p1", "u1", "u2", "EDITOR")
        
        invitationRepo.rejectInvitation(invite.id, "Bearer token")
        
        val updated = invitationRepo.getInvitationById(invite.id)
        assertEquals("REJECTED", updated?.status)
    }
}
