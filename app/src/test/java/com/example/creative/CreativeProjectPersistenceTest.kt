package com.example.creative

import com.example.creative.persistence.CreativeProjectEntity
import org.junit.Assert.*
import org.junit.Test

class CreativeProjectPersistenceTest {

    @Test
    fun testCreativeProjectEntityMapping() {
        val entity = CreativeProjectEntity(
            id = "proj_persistence_1",
            sourceMedia = "/media/video.mp4",
            type = "REEL",
            layersJson = "[]",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            tempExportPath = "/media/export.mp4"
        )

        assertEquals("proj_persistence_1", entity.id)
        assertEquals("/media/video.mp4", entity.sourceMedia)
        assertEquals("REEL", entity.type)
        assertEquals("/media/export.mp4", entity.tempExportPath)
    }
}
