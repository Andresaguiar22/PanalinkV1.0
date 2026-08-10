package com.example.media.audio

import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.PanalinkDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AudioImportManagerTest {

    private lateinit var db: PanalinkDatabase
    private lateinit var repository: AudioRepository
    private lateinit var importManager: AudioImportManager

    @Before
    fun setUp() {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, PanalinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AudioRepository(db.audioDao())
        importManager = AudioImportManager(context, repository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testImportAudioFromUriAndDeduplication(): Unit = runBlocking {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        val sampleFile = File(context.cacheDir, "sample_track.mp3")
        sampleFile.writeBytes("SAMPLE_AUDIO_BYTES_12345".toByteArray())

        val uri = Uri.fromFile(sampleFile)
        val track1 = importManager.importAudioFromUri(uri, "user_1")

        assertNotNull(track1)
        assertEquals("user_1", track1?.userId)
        assertNotNull(track1?.fileHash)

        // Re-importing same file content should deduplicate and return same hash
        val track2 = importManager.importAudioFromUri(uri, "user_1")
        assertNotNull(track2)
        assertEquals(track1?.fileHash, track2?.fileHash)

        sampleFile.delete()
        Unit
    }
}
