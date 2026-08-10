package com.example.media.audio

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.PanalinkDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioLibraryManagerTest {

    private lateinit var db: PanalinkDatabase
    private lateinit var libraryManager: AudioLibraryManager

    @Before
    fun setUp() {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PanalinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = AudioRepository(db.audioDao())
        libraryManager = AudioLibraryManager(repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAddEditAndFavoriteTrack() = runBlocking {
        val track = AudioTrackEntity(
            id = "t1",
            userId = "u1",
            title = "Cancion Original",
            artist = "Artista 1",
            album = "Album 1",
            filePath = "/sdcard/music/song.mp3",
            fileHash = "abc123hash"
        )

        libraryManager.addTrack(track)

        val retrieved = libraryManager.getTrackById("t1")
        assertNotNull(retrieved)
        assertEquals("Cancion Original", retrieved?.title)

        // Edit metadata
        libraryManager.editTrackMetadata("t1", "Nuevo Titulo", "Nuevo Artista", "Nuevo Album")
        val updated = libraryManager.getTrackById("t1")
        assertEquals("Nuevo Titulo", updated?.title)
        assertEquals("Nuevo Artista", updated?.artist)

        // Toggle favorite
        libraryManager.toggleFavorite("t1", false)
        val favs = libraryManager.favoriteTracks.first()
        assertEquals(1, favs.size)
        assertEquals("t1", favs[0].id)
    }
}
