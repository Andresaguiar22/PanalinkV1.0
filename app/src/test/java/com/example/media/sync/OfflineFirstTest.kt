package com.example.media.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.PanalinkDatabase
import com.example.media.playlist.PlaylistEntity
import com.example.media.playlist.PlaylistRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OfflineFirstTest {

    private lateinit var db: PanalinkDatabase
    private lateinit var playlistRepo: PlaylistRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, PanalinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        playlistRepo = PlaylistRepository(db.playlistDao(), db.collaboratorDao())
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `test local persistence works without network`() = runBlocking {
        // 1. Simulate adding data while "offline"
        val playlist = PlaylistEntity(
            id = "local-1",
            ownerId = "me",
            name = "Offline Favorites",
            isDirty = true
        )
        playlistRepo.createPlaylist(playlist)
        
        // 2. Verify it's retrieved from local DB
        val playlists = playlistRepo.getAllPlaylists().first()
        assertEquals(1, playlists.size)
        assertEquals("Offline Favorites", playlists[0].name)
        assertEquals(true, playlists[0].isDirty)
    }
}
