package com.example.media.playlist

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.PanalinkDatabase
import com.example.data.model.RemoteMusicPlaylistCollaborator
import com.example.media.sync.MusicSocialMapper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class CollaboratorRoomTest {

    private lateinit var db: PanalinkDatabase
    private lateinit var collaboratorDao: CollaboratorDao
    private val mapper = MusicSocialMapper

    @Before
    fun setUp() {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PanalinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        collaboratorDao = db.collaboratorDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testUpsertAndIdempotency(): Unit = runBlocking {
        val collab = PlaylistCollaboratorEntity(
            id = "c1",
            playlistId = "p1",
            userId = "u1",
            role = "EDITOR",
            updatedAt = System.currentTimeMillis(),
            isDirty = true
        )

        // First insert
        collaboratorDao.upsertCollaborator(collab)
        
        // Second insert (duplicate)
        collaboratorDao.upsertCollaborator(collab)

        val list = collaboratorDao.getCollaboratorsForPlaylist("p1")
        assertEquals(1, list.size)
        assertEquals("EDITOR", list[0].role)
        Unit
    }

    @Test
    fun testUpdateRole(): Unit = runBlocking {
        val collab = PlaylistCollaboratorEntity(
            id = "c1",
            playlistId = "p1",
            userId = "u1",
            role = "VIEWER",
            updatedAt = 1000L,
            isDirty = false
        )
        collaboratorDao.upsertCollaborator(collab)

        collaboratorDao.updateRole("c1", "EDITOR", 2000L, true)

        val updated = collaboratorDao.getCollaborator("p1", "u1")
        assertNotNull(updated)
        assertEquals("EDITOR", updated?.role)
        assertEquals(2000L, updated?.updatedAt)
        assertTrue(updated?.isDirty == true)
        Unit
    }

    @Test
    fun testObserveCollaborators(): Unit = runBlocking {
        val collab = PlaylistCollaboratorEntity(
            id = "c1",
            playlistId = "p1",
            userId = "u1",
            role = "EDITOR",
            updatedAt = System.currentTimeMillis()
        )
        collaboratorDao.upsertCollaborator(collab)

        val flow = collaboratorDao.observeCollaborators("p1")
        val result = flow.first()
        assertEquals(1, result.size)
        assertEquals("u1", result[0].userId)
        Unit
    }

    @Test
    fun testMappingRemoteToLocal(): Unit = runBlocking {
        val remote = RemoteMusicPlaylistCollaborator(
            id = "rem1",
            playlist_id = "p1",
            user_id = "u2",
            role = "VIEWER",
            created_at = "2024-01-01T10:00:00.000Z"
        )

        val local = mapper.toLocal(remote)
        assertEquals("rem1", local.id)
        assertEquals("p1", local.playlistId)
        assertEquals("u2", local.userId)
        assertEquals("VIEWER", local.role)
        assertFalse(local.isDirty)
        Unit
    }

    @Test
    fun testMappingLocalToRemote(): Unit = runBlocking {
        val local = PlaylistCollaboratorEntity(
            id = "loc1",
            playlistId = "p2",
            userId = "u3",
            role = "EDITOR",
            updatedAt = 1715000000000L, // Some timestamp
            isDirty = true
        )

        val remote = mapper.toRemote(local)
        assertEquals("loc1", remote.id)
        assertEquals("p2", remote.playlist_id)
        assertEquals("u3", remote.user_id)
        assertEquals("EDITOR", remote.role)
        assertNotNull(remote.created_at)
        Unit
    }
}
