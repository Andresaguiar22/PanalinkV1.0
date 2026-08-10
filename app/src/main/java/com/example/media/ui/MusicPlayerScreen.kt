package com.example.media.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.media.player.PanaMusicPlayerManager
import com.example.media.player.PlayerState
import com.example.media.player.RepeatMode

/**
 * P6.7 - Music Player Screen (Poweramp / Spotify Style)
 * Audio player UI featuring high quality album cover art, wave progress seeker, shuffle, repeat, playback speed, and sharing to PanaLink.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    playerManager: PanaMusicPlayerManager,
    onBackClick: () -> Unit,
    onShareTrackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerState by playerManager.playerState.collectAsState()
    val track = playerState.currentTrack

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reproduciendo", color = Color.White, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Cerrar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onShareTrackClick) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir en PanaLink", tint = Color(0xFF38BDF8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF0F172A),
        modifier = modifier
    ) { padding ->
        if (track == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay ningún audio seleccionado", color = Color.Gray)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                // Album Art Frame
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E293B)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!track.coverPath.isNullOrEmpty()) {
                        AsyncImage(
                            model = track.coverPath,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(100.dp)
                        )
                    }
                }

                // Track Info
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = track.title,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${track.artist} • ${track.album}",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }

                // Progress Bar / Seeker
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = playerState.currentPositionMs.toFloat(),
                        onValueChange = { playerManager.seekTo(it.toLong()) },
                        valueRange = 0f..(playerState.durationMs.coerceAtLeast(1L).toFloat()),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatMs(playerState.currentPositionMs), color = Color.Gray, fontSize = 12.sp)
                        Text(formatMs(playerState.durationMs), color = Color.Gray, fontSize = 12.sp)
                    }
                }

                // Main Player Controls (Shuffle, Prev, Play/Pause, Next, Repeat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { playerManager.toggleShuffle() }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Aleatorio",
                            tint = if (playerState.isShuffle) Color(0xFF38BDF8) else Color.Gray
                        )
                    }

                    IconButton(onClick = { playerManager.previousTrack() }) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Anterior",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Play/Pause Fab Button
                    FloatingActionButton(
                        onClick = { playerManager.togglePlayPause() },
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pausar" else "Reproducir",
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = { playerManager.nextTrack() }) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Siguiente",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(onClick = { playerManager.toggleRepeat() }) {
                        Icon(
                            imageVector = when (playerState.repeatMode) {
                                RepeatMode.ONE -> Icons.Default.RepeatOne
                                else -> Icons.Default.Repeat
                            },
                            contentDescription = "Repetir",
                            tint = if (playerState.repeatMode != RepeatMode.NONE) Color(0xFF38BDF8) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}
