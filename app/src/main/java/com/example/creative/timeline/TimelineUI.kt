package com.example.creative.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creative.core.CreativeLayer

/**
 * P6.5A - Professional Multi-Track Timeline UI Composable
 * CapCut / Premiere style multi-track visual timeline for Reel Studio and other editors.
 */

@Composable
fun MultiTrackTimelineUI(
    tracks: List<CreativeTrack>,
    layers: List<CreativeLayer>,
    currentTimeMs: Long,
    totalDurationMs: Long,
    selectedTrackId: String?,
    selectedLayerId: String?,
    onSeek: (Long) -> Unit,
    onSelectTrack: (String) -> Unit,
    onSelectLayer: (String) -> Unit,
    onToggleMuteTrack: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121218)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header with Playhead Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timeline, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Timeline Multipista (${currentTimeMs / 1000f}s / ${totalDurationMs / 1000f}s)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Snapping 🧲", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Playhead Scrubber Slider
            Slider(
                value = currentTimeMs.toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..totalDurationMs.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E5FF),
                    activeTrackColor = Color(0xFF00E5FF)
                )
            )

            // Multi-Track List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Video & Audio Tracks
                items(tracks) { track ->
                    val isSelected = (track.id == selectedTrackId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) Color(0xFF222238) else Color(0xFF1A1A24),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectTrack(track.id) }
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onToggleMuteTrack(track.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (track.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Mute",
                                tint = if (track.isMuted) Color.Red else Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = track.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(90.dp),
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Visual Clip Block
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(22.dp)
                                .background(
                                    when (track) {
                                        is CreativeTrack.VideoTrack -> Color(0xFF00E5FF).copy(alpha = 0.8f)
                                        is CreativeTrack.AudioTrack -> Color(0xFFE040FB).copy(alpha = 0.8f)
                                        is CreativeTrack.VoiceTrack -> Color(0xFF00FF85).copy(alpha = 0.8f)
                                        else -> Color(0xFFFFD54F).copy(alpha = 0.8f)
                                    },
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = " Clip (${track.durationMs / 1000}s)",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Overlay Layers Track (Text / Sticker / Drawing)
                items(layers) { layer ->
                    val isSelected = (layer.id == selectedLayerId)
                    val titleName = when (layer) {
                        is CreativeLayer.Text -> "Txt: \"${layer.text}\""
                        is CreativeLayer.Sticker -> "Sticker"
                        is CreativeLayer.Drawing -> "Dibujo"
                        is CreativeLayer.Filter -> "Filtro"
                        is CreativeLayer.Audio -> "Audio"
                        is CreativeLayer.Interactive -> layer.interactiveType
                        is CreativeLayer.Group -> layer.groupName
                        is CreativeLayer.Image -> "Imagen"
                        is CreativeLayer.Video -> "Video"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) Color(0xFF28283E) else Color(0xFF1E1E2A),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectLayer(layer.id) }
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = titleName,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.width(90.dp),
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .background(Color(0xFF33334A), RoundedCornerShape(4.dp))
                        ) {
                            val startPct = (layer.startOffsetMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                            val durPct = (layer.durationMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f - startPct)

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(durPct)
                                    .background(Color(0xFFFF4081), RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}
