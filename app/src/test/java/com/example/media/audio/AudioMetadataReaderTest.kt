package com.example.media.audio

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AudioMetadataReaderTest {

    @Test
    fun testExtractMetadataFromFileFallback(): Unit = runBlocking {
        val context: android.content.Context = ApplicationProvider.getApplicationContext()
        val testFile = File(context.cacheDir, "cancion_demo.mp3")
        testFile.writeText("audio content bytes")

        val metadata = AudioMetadataReader.extractMetadata(context, testFile)

        assertNotNull(metadata)
        assertEquals("cancion_demo", metadata.title)
        assertEquals("Artista Desconocido", metadata.artist)
        assertEquals("Álbum Desconocido", metadata.album)

        testFile.delete()
        Unit
    }
}
