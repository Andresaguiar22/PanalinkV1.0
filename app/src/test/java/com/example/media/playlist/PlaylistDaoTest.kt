package com.example.media.playlist

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.PanalinkDatabase
import com.example.media.audio.AudioTrackEntity
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
class PlaylistDaoTest {

    private lateinit var db: PanalinkDatabase
    private lateinit var playlistDao: PlaylistDao

    @Before
    fun setUp() {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PanalinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        playlistDao = db.playlistDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testInsertAndGetPlaylist(): Unit = runBlocking {
        val playlist = PlaylistEntity(
            id = "p1",
            ownerId = "u1",
            name = "Verano 2024",
            description = "Mix de playa"
        )
        playlistDao.insertPlaylist(playlist)

        val retrieved = playlistDao.getPlaylistById("p1")
        assertNotNull(retrieved)
        assertEquals("Verano 2024", retrieved?.name)
        Unit
    }

    @Test
    fun testPlaylistTracksAndOrdering(): Unit = runBlocking {
        // Setup playlist and tracks
        val playlist = PlaylistEntity(id = "p1", ownerId = "u1", name = "Test Order")
        playlistDao.insertPlaylistPlaylist(playlist) // Fixed method name below if needed, wait, it's insertPlaylist

        // Insert some tracks in Audio Core
        val track1 = AudioTrackEntity(id = "t1", userId = "u1", title = "A", filePath = "/a.mp3")
        val track2 = AudioTrackEntity(id = "t2", userId = "u1", title = "B", filePath = "/b.mp3")
        db.audioDao().insertTracks(listOf(track1, track2))

        // Link with specific positions
        playlistDao.insertPlaylistTrack(PlaylistTrackEntity(id = "pt1", playlistId = "p1", trackId = "t2", position = 0))
        playlistDao.insertPlaylistTrack(PlaylistTrackEntity(id = "pt2", playlistId = "p1", trackId = "t1", position = 1))

        val tracks = playlistDao.getTracksForPlaylist("p1").first()
        assertEquals(2, tracks.size)
        assertEquals("t2", tracks[0].id) // Track 2 is first because position 0
        assertEquals("t1", tracks[1].id) // Track 1 is second because position 1
        Unit
    }

    // Helper because of method names
    private suspend fun PlaylistDao.insertPlaylistPlaylist(p: PlaylistEntity) = this.insertPlaylist(p)
}
