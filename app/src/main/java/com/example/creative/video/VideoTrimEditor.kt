package com.example.creative.video

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun VideoTrimEditor(
    totalDurationMs: Long,
    startTrimMs: Long,
    endTrimMs: Long,
    onTrimChanged: (startMs: Long, endMs: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var range by remember(startTrimMs, endTrimMs, totalDurationMs) {
        val maxVal = if (totalDurationMs > 0) totalDurationMs.toFloat() else 10000f
        val startVal = startTrimMs.toFloat().coerceIn(0f, maxVal)
        val endVal = endTrimMs.toFloat().coerceIn(startVal, maxVal)
        mutableStateOf(startVal..endVal)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF18181F), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cortar Vídeo",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${(range.start / 1000).toInt()}s - ${(range.endInclusive / 1000).toInt()}s (${((range.endInclusive - range.start) / 1000).toInt()}s)",
                color = Color(0xFF00E5FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF2A2A36), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF3F3F52), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            val maxVal = if (totalDurationMs > 0) totalDurationMs.toFloat() else 10000f
            RangeSlider(
                value = range,
                onValueChange = { newRange ->
                    range = newRange
                    onTrimChanged(newRange.start.toLong(), newRange.endInclusive.toLong())
                },
                valueRange = 0f..maxVal,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E5FF),
                    activeTrackColor = Color(0xFF00E5FF),
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}
