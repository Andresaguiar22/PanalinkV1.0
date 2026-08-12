package com.example.ui.reels.editor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Lightweight selection chrome; editing remains owned by the ViewModel. */
@Composable
fun ReelTransformOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    Box(
        modifier = modifier
            .fillMaxSize()
            .border(1.dp, Color.White.copy(alpha = 0.8f))
    ) {
        Box(Modifier.align(Alignment.TopStart).size(10.dp).border(2.dp, Color.White))
        Box(Modifier.align(Alignment.TopEnd).size(10.dp).border(2.dp, Color.White))
        Box(Modifier.align(Alignment.BottomStart).size(10.dp).border(2.dp, Color.White))
        Box(Modifier.align(Alignment.BottomEnd).size(10.dp).border(2.dp, Color.White))
    }
}
