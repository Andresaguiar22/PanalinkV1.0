package com.example.ui.reels

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable

// This file is intentionally kept compatible with the existing Reels UI implementation.
// The only compile fix in this revision is the padding overload at the progress bar.

@Composable
private fun ReelsProgressPaddingFix(
    durationMs: Long,
    positionMs: Long,
    formatTimeV2: (Long) -> String,
) {
    if (durationMs > 0L) {
        val fraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTimeV2(positionMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                Text(formatTimeV2(durationMs), color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.labelSmall)
            }
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier.fillMaxWidth().height(3.dp)
            )
        }
    }
}
