package com.example.creative.ai

import com.example.creative.core.CreativeProject
import com.example.creative.core.CreativeType
import org.junit.Assert.*
import org.junit.Test

class SmartEditorAssistantTest {

    @Test
    fun testAnalyzeProjectSuggestions() {
        val project = CreativeProject(
            id = "proj_test_ai",
            sourceMedia = "/media/sample.mp4",
            type = CreativeType.REEL,
            layers = emptyList()
        )

        val suggestions = SmartEditorAssistant.analyzeProject(project)
        assertTrue(suggestions.isNotEmpty())

        val filterSuggestion = suggestions.find { it.actionType == SuggestionActionType.APPLY_FILTER }
        assertNotNull(filterSuggestion)
        assertEquals("cinematic", filterSuggestion?.payload)

        val musicSuggestion = suggestions.find { it.actionType == SuggestionActionType.ADD_MUSIC }
        assertNotNull(musicSuggestion)
    }
}
