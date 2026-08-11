package com.example.identity.memory

import androidx.annotation.Keep
import com.example.identity.model.IdentityUiState
import java.util.concurrent.ConcurrentHashMap

@Keep
object IdentityMemoryCache {
    // Basic cache for UI identity states to avoid flickering during navigation
    val profiles = ConcurrentHashMap<String, IdentityUiState>()
}
