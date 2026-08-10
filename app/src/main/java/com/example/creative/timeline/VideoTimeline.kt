package com.example.creative.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TimelineClip(
    val id: String,
    val mediaUriOrPath: String,
    val startOffsetMs: Long = 0L,
    val durationMs: Long = 10000L,
    val speed: Float = 1.0f
)

class TimelineController {
    var clips: List<TimelineClip> = emptyList()
        private set

    var currentPositionMs: Long = 0L

    fun setInitialClip(clip: TimelineClip) {
        clips = listOf(clip)
    }

    fun addClip(clip: TimelineClip) {
        clips = clips + clip
    }

    fun getTotalDurationMs(): Long {
        return clips.sumOf { (it.durationMs / it.speed).toLong() }
    }
}

@Composable
fun VideoTimeline(
    clips: List<TimelineClip>,
    currentPositionMs: Long,
    totalDurationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141418), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Timeline", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "${currentPositionMs / 1000}s / ${totalDurationMs / 1000}s",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color(0xFF22222B), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF333342), RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            clips.forEachIndexed { index, clip ->
                val weight = if (totalDurationMs > 0) clip.durationMs.toFloat() / totalDurationMs.toFloat() else 1f
                Box(
                    modifier = Modifier
                        .weight(weight.coerceAtLeast(0.1f))
                        .fillMaxHeight()
                        .padding(2.dp)
                        .background(
                            if (index % 2 == 0) Color(0xFF00E5FF).copy(alpha = 0.3f) else Color(0xFFFF4081).copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Clip ${index + 1}", color = Color.White, fontSize = 10.sp)
                }
            }
        }
    }
}
