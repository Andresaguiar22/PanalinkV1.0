package com.example.creative.post

/**
 * P6.6.2 - Post Studio State
 * Represents UI state and draft progress for the Post Studio editor.
 */
data class PostStudioState(
    val project: PostStudioProject = PostStudioProject(),
    val selectedPageIndex: Int = 0,
    val selectedLayerId: String? = null,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val isPublishing: Boolean = false,
    val errorMessage: String? = null
)
