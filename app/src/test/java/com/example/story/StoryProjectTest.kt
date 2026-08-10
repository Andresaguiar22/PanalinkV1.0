package com.example.story

import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import com.example.creative.core.CreativeType
import org.junit.Assert.*
import org.junit.Test

class StoryProjectTest {

    @Test
    fun testStoryProjectInitialization() {
        val storyProject = CreativeProject(
            id = "story_test_1",
            sourceMedia = "sample_story.jpg",
            type = CreativeType.STORY
        )

        assertEquals("story_test_1", storyProject.id)
        assertEquals(CreativeType.STORY, storyProject.type)
        assertEquals("sample_story.jpg", storyProject.sourceMedia)
        assertTrue(storyProject.layers.isEmpty())
    }

    @Test
    fun testStoryProjectDraftRecovery() {
        val originalProject = CreativeProject(
            id = "draft_story_123",
            sourceMedia = "draft_media.mp4",
            type = CreativeType.STORY,
            layers = listOf(
                CreativeLayer.Text(id = "txt_1", text = "Borrador de historia")
            )
        )

        assertNotNull(originalProject)
        assertEquals(1, originalProject.layers.size)
        val recoveredText = (originalProject.layers.first() as CreativeLayer.Text).text
        assertEquals("Borrador de historia", recoveredText)
    }
}
