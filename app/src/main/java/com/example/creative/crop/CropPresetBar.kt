package com.example.creative.crop

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Reusable preset selector; intentionally has no dependency on ReelEditorScreen. */
@Composable
fun CropPresetBar(
    selected: CropAspectRatio,
    onSelected: (CropAspectRatio) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CropAspectRatio.entries.forEach { preset ->
            FilterChip(
                selected = preset == selected,
                onClick = { onSelected(preset) },
                label = { Text(preset.label) }
            )
        }
    }
}
