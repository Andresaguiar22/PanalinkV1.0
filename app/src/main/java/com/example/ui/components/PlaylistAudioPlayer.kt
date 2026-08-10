package com.example.ui.components

import android.media.MediaPlayer
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.net.URI
import java.net.URLDecoder

fun extractFilename(url: String): String {
    return try {
        val path = URI(url).path
        val name = path.substringAfterLast('/')
        val decoded = URLDecoder.decode(name, "UTF-8")
        if (decoded.contains("-")) decoded.substringAfter("-") else decoded
    } catch (e: Exception) {
        "Pista de Audio"
    }
}

fun formatAudioTime(ms: Int): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun PlaylistAudioPlayer(audioUrls: List<String>) {
    if (audioUrls.isEmpty()) return
    
    val context = LocalContext.current
    var currentTrackIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    
    var durationMs by remember { mutableIntStateOf(0) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    
    val currentUrl = audioUrls.getOrNull(currentTrackIndex) ?: audioUrls.first()
    
    DisposableEffect(currentUrl) {
        val mp = MediaPlayer().apply {
            setDataSource(context, Uri.parse(currentUrl))
            prepareAsync()
            setOnPreparedListener { 
                durationMs = it.duration
                if (isPlaying) {
                    start()
                }
            }
            setOnCompletionListener {
                if (currentTrackIndex < audioUrls.size - 1) {
                    currentTrackIndex++
                } else {
                    isPlaying = false
                    currentPositionMs = 0
                }
            }
        }
        mediaPlayer = mp
        
        onDispose {
            try {
                mp.stop()
                mp.release()
            } catch (e: Exception) {}
            mediaPlayer = null
            durationMs = 0
            currentPositionMs = 0
        }
    }
    
    LaunchedEffect(isPlaying, mediaPlayer) {
        while (isActive && isPlaying && mediaPlayer != null) {
            try {
                currentPositionMs = mediaPlayer?.currentPosition ?: 0
            } catch (e: Exception) {}
            delay(100)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E22))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        // Player Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album Art or Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFD500F9), Color(0xFF7C4DFF))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Track Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = extractFilename(currentUrl),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (audioUrls.size > 1) "Pista ${currentTrackIndex + 1} de ${audioUrls.size}" else "Audio",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Progress Bar
        val progress = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
        
        // Custom Waveform/Progress animation (Spotify-like line)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .background(Color(0xFF1DB954)) // Spotify Green
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Time Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatAudioTime(currentPositionMs),
                color = Color.Gray,
                fontSize = 11.sp
            )
            Text(
                text = formatAudioTime(durationMs),
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    if (currentTrackIndex > 0) {
                        currentTrackIndex--
                        isPlaying = true
                    }
                },
                enabled = currentTrackIndex > 0
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Anterior",
                    tint = if (currentTrackIndex > 0) Color.White else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable {
                        if (isPlaying) {
                            mediaPlayer?.pause()
                            isPlaying = false
                        } else {
                            mediaPlayer?.start()
                            isPlaying = true
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            IconButton(
                onClick = {
                    if (currentTrackIndex < audioUrls.size - 1) {
                        currentTrackIndex++
                        isPlaying = true
                    }
                },
                enabled = currentTrackIndex < audioUrls.size - 1
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Siguiente",
                    tint = if (currentTrackIndex < audioUrls.size - 1) Color.White else Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Playlist (if multiple)
        if (audioUrls.size > 1) {
            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = Color.White.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {
                audioUrls.forEachIndexed { index, url ->
                    val isCurrent = index == currentTrackIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                currentTrackIndex = index
                                isPlaying = true
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = if (isCurrent) Color(0xFF1DB954) else Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.width(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = extractFilename(url),
                            color = if (isCurrent) Color(0xFF1DB954) else Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (isCurrent && isPlaying) {
                            // Small animation indicator
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color(0xFF1DB954),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
