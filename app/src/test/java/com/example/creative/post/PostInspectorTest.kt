package com.example.creative.post

import com.example.creative.core.CreativeLayer
import org.junit.Assert.*
import org.junit.Test

class PostInspectorTest {

    @Test
    fun testUpdateTextLayerProperties() {
        val layer = CreativeLayer.Text(
            id = "txt_1",
            text = "Original",
            scale = 1.0f,
            rotation = 0f,
            opacity = 1.0f
        )

        val updatedLayer = layer.copy(
            text = "Modified",
            scale = 1.5f,
            rotation = 45f,
            opacity = 0.8f
        )

        assertEquals("Modified", updatedLayer.text)
        assertEquals(1.5f, updatedLayer.scale, 0.01f)
        assertEquals(45f, updatedLayer.rotation, 0.01f)
        assertEquals(0.8f, updatedLayer.opacity, 0.01f)
    }

    @Test
    fun testUpdateImageLayerFilterAndTransform() {
        val layer = CreativeLayer.Image(
            id = "img_1",
            imageUriOrPath = "/storage/test.jpg",
            filterName = "Normal"
        )

        val updated = layer.copy(
            filterName = "Vivid",
            scale = 1.2f,
            xFraction = 0.6f
        )

        assertEquals("Vivid", updated.filterName)
        assertEquals(1.2f, updated.scale, 0.01f)
        assertEquals(0.6f, updated.xFraction, 0.01f)
    }
}
