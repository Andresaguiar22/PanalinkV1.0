package com.example.story

import com.example.creative.core.CreativeLayer
import org.junit.Assert.*
import org.junit.Test

class StoryLayerTest {

    @Test
    fun testTextLayerProperties() {
        val textLayer = CreativeLayer.Text(
            id = "txt_pro_1",
            text = "PanaLink Story Studio V3",
            colorHex = "#00E5FF",
            fontSizeSp = 32f,
            fontFamily = "Montserrat",
            hasShadow = true
        )

        assertEquals("txt_pro_1", textLayer.id)
        assertEquals("PanaLink Story Studio V3", textLayer.text)
        assertEquals("#00E5FF", textLayer.colorHex)
        assertEquals("Montserrat", textLayer.fontFamily)
        assertTrue(textLayer.hasShadow)
    }

    @Test
    fun testInteractiveLayerProperties() {
        val interactiveLayer = CreativeLayer.Interactive(
            id = "inter_1",
            interactiveType = "POLL",
            title = "¿Te gusta PanaLink V2.0?",
            optionA = "¡Sí!",
            optionB = "¡Me encanta!"
        )

        assertEquals("inter_1", interactiveLayer.id)
        assertEquals("POLL", interactiveLayer.interactiveType)
        assertEquals("¿Te gusta PanaLink V2.0?", interactiveLayer.title)
        assertEquals("¡Sí!", interactiveLayer.optionA)
        assertEquals("¡Me encanta!", interactiveLayer.optionB)
    }
}
