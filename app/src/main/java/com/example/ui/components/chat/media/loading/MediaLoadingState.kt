package com.example.ui.components.chat.media.loading

import androidx.compose.runtime.Immutable

@Immutable
sealed class MediaLoadingState {
    object Idle : MediaLoadingState()
    object Loading : MediaLoadingState()
    object Success : MediaLoadingState()
    data class Error(val message: String? = null) : MediaLoadingState()
    object Retrying : MediaLoadingState()
}
