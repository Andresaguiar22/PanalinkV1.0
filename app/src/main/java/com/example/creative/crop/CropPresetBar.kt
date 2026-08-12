package com.example.creative.crop

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Reusable preset selector; intentionally has no dependency on ReelEditorScreen. */
@Composable
fun CropPresetBar(
    selected: CropAspectRatio,
    onSelected: (CropAspectRatio) -> Unit
) {
    Row(
        modifier = androidx.compose.ui.Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.dp(8f))
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
