package com.example.ui.reels.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReelTextStyleToolbar(
    visible: Boolean,
    fontSizeSp: Float,
    backgroundEnabled: Boolean,
    onFontSizeChange: (Float) -> Unit,
    onBackgroundToggle: () -> Unit,
    onAlignCenter: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    var localSize by remember(fontSizeSp) { mutableStateOf(fontSizeSp.coerceIn(12f, 96f)) }
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tamaño")
        Slider(
            value = localSize,
            onValueChange = {
                localSize = it
                onFontSizeChange(it)
            },
            valueRange = 12f..96f,
            modifier = Modifier.fillMaxWidth(0.3f)
        )
        FilterChip(selected = backgroundEnabled, onClick = onBackgroundToggle, label = { Text("Fondo") })
        TextButton(onClick = onAlignCenter) { Text("Centrar") }
        TextButton(onClick = onClose) { Text("Listo") }
    }
}
