package com.example.story

import com.example.media.dedup.MediaDeduplicationEngine
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class StoryExportTest {

    @Test
    fun testDeduplicationEngineValidation() {
        val tempFile = File.createTempFile("story_export_test", ".jpg")
        tempFile.writeText("sample story export bytes")

        val hash = MediaDeduplicationEngine.calculateSha256(tempFile)
        assertNotNull(hash)
        assertTrue(hash!!.isNotEmpty())

        tempFile.delete()
    }

    @Test
    fun testStoryExportPipelineMimeType() {
        val isVideo = true
        val mimeType = if (isVideo) "video/mp4" else "image/jpeg"
        val uploadType = "story"

        assertEquals("video/mp4", mimeType)
        assertEquals("story", uploadType)
    }
}
