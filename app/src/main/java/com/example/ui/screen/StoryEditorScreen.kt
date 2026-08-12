package com.example.ui.screen

import androidx.compose.runtime.Composable
import com.example.ui.story.StoryStudioScreen
import com.example.ui.viewmodel.StatesViewModel

/**
 * Compatibility entry point used by existing navigation.
 * The old multi-panel creative editor is intentionally removed; all Story
 * creation now goes through the focused Story Studio.
 */
@Composable
fun StoryEditorScreen(
    viewModel: StatesViewModel,
    onBack: () -> Unit
) {
    StoryStudioScreen(
        onBack = onBack
    )
}
