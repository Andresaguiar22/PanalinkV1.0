package com.example.creative.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CreativeState(
    val currentProject: CreativeProject? = null,
    val selectedLayerId: String? = null,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val filterName: String = "none",
    val isDrawingMode: Boolean = false,
    val strokeColorHex: String = "#FF0000",
    val strokeWidthDp: Float = 4f
)

class CreativeHistoryManager {
    private val history = mutableListOf<CreativeProject>()
    private var currentIndex = -1

    fun pushState(project: CreativeProject) {
        if (currentIndex < history.size - 1) {
            history.subList(currentIndex + 1, history.size).clear()
        }
        history.add(project)
        currentIndex = history.size - 1
    }

    fun canUndo(): Boolean = currentIndex > 0

    fun canRedo(): Boolean = currentIndex < history.size - 1

    fun undo(): CreativeProject? {
        if (canUndo()) {
            currentIndex--
            return history[currentIndex]
        }
        return null
    }

    fun redo(): CreativeProject? {
        if (canRedo()) {
            currentIndex++
            return history[currentIndex]
        }
        return null
    }

    fun getCurrent(): CreativeProject? {
        return if (currentIndex in 0 until history.size) history[currentIndex] else null
    }
}
