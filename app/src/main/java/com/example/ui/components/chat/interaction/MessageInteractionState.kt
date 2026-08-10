package com.example.ui.components.chat.interaction

import androidx.compose.runtime.Immutable

/**
 * MessageInteractionState
 * Immutable state model for tracking chat bubble visual and gesture interaction state.
 */
@Immutable
data class MessageInteractionState(
    val isPressed: Boolean = false,
    val isSelected: Boolean = false,
    val isMenuOpen: Boolean = false,
    val activeReaction: String? = null,
    val isSwiping: Boolean = false
)
