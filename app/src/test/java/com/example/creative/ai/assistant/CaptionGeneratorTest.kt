package com.example.creative.ai.assistant

import org.junit.Assert.*
import org.junit.Test

class CaptionGeneratorTest {

    @Test
    fun testGenerateCaptionsReturnsMultipleTonesAndHashtags() {
        val topic = "Lanzamiento de PanaLink V2"
        val captions = SmartCaptionGenerator.generateCaptions(topic, "Negocios")

        assertTrue("Debe retornar al menos 4 captions de tonos variados", captions.size >= 4)

        val tones = captions.map { it.tone }
        assertTrue("Debe incluir tono inspiracional", tones.contains(CaptionTone.INSPIRATIONAL))
        assertTrue("Debe incluir tono viral", tones.contains(CaptionTone.VIRAL))

        captions.forEach { caption ->
            assertTrue("El texto del caption debe contener el tema", caption.text.contains(topic))
            assertTrue("Debe sugerir hashtags", caption.suggestedHashtags.isNotEmpty())
        }
    }
}
