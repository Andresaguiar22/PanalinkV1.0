package com.example.creative.reels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Timeline-only UI. It emits actions instead of mutating editor state. */
@Composable
fun ReelTimelineBar(
    state: ReelTimelineState,
    onSelectClip: (String) -> Unit,
    onAddClip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            state.clips.forEach { clip ->
                val selected = clip.id == state.selectedClipId
                Box(
                    modifier = Modifier
                        .height(58.dp)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { onSelectClip(clip.id) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (clip.mimeType.startsWith("video/")) "🎥" else "🖼️")
                        Text(text = "${clip.effectiveDurationMs / 1000}s")
                    }
                }
            }

            Box(
                modifier = Modifier
                    .height(58.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .clickable(onClick = onAddClip)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "+")
            }
        }
    }
}
