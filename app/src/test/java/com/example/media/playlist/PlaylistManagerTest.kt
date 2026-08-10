package com.example.media.playlist

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.PanalinkDatabase
import com.example.media.audio.AudioRepository
import com.example.media.audio.AudioTrackEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PlaylistManagerTest {

    private lateinit var db: PanalinkDatabase
    private lateinit var manager: PlaylistManager

    @Before
    fun setUp() {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PanalinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val playlistRepo = PlaylistRepository(db.playlistDao(), db.collaboratorDao())
        val audioRepo = AudioRepository(db.audioDao())
        manager = PlaylistManager(playlistRepo, audioRepo)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testCreateAndDuplicatePlaylist(): Unit = runBlocking {
        val p = manager.createNewPlaylist("u1", "Original")
        
        // Add a track first
        val track = AudioTrackEntity(id = "t1", userId = "u1", title = "S", filePath = "/s.mp3")
        db.audioDao().insertTrack(track)
        PlaylistRepository(db.playlistDao(), db.collaboratorDao()).addTrackToPlaylist(p.id, "t1")

        val duplicated = manager.duplicatePlaylist(p.id, "Copia")
        assertNotNull(duplicated)
        assertEquals("Copia", duplicated?.name)
        
        val tracks = PlaylistRepository(db.playlistDao(), db.collaboratorDao()).getTracksForPlaylist(duplicated!!.id).first()
        assertEquals(1, tracks.size)
        assertEquals("t1", tracks[0].id)
        Unit
    }

    @Test
    fun testDetectDeletedTracks(): Unit = runBlocking {
        val p = manager.createNewPlaylist("u1", "Ghost Tracks")
        
        // This file doesn't exist
        val track = AudioTrackEntity(id = "t_ghost", userId = "u1", title = "Ghost", filePath = "/non/existent/file.mp3")
        db.audioDao().insertTrack(track)
        PlaylistRepository(db.playlistDao(), db.collaboratorDao()).addTrackToPlaylist(p.id, "t_ghost")

        val validTracks = manager.getValidTracks(p.id).first()
        assertTrue(validTracks.isEmpty()) // Should be empty because file doesn't exist
        Unit
    }
}
