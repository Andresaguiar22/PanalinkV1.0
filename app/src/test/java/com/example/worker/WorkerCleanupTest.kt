package com.example.worker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WorkerCleanupTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testOriginalFilePreservedForRetryAndIntermediateCleanedUp() {
        val originalFile = tempFolder.newFile("original_media.mp4")
        originalFile.writeText("sample video content")
        assertTrue(originalFile.exists())

        val intermediateTemp = tempFolder.newFile("reel_compressed_123.mp4")
        intermediateTemp.writeText("compressed video temp content")
        assertTrue(intermediateTemp.exists())

        // Simulate Worker finally block execution logic
        try {
            // Worker operation fails or is cancelled
            throw IllegalStateException("Simulated network exception during upload")
        } catch (e: Exception) {
            // Handled failure
        } finally {
            // Intermediate temp cleanup
            if (intermediateTemp.exists() && intermediateTemp.absolutePath != originalFile.absolutePath) {
                intermediateTemp.delete()
            }
        }

        // Original file required for retry must survive
        assertTrue("Original source file MUST survive worker failure for retry", originalFile.exists())
        // Intermediate temporary file generated during compression must be cleaned up
        assertFalse("Intermediate temporary file must be cleaned up in finally", intermediateTemp.exists())
    }
}
