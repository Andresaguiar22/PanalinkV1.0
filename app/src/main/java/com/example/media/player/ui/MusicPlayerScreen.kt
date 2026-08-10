package com.example.media.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.audio.AudioTrackEntity

/**
 * P6.7.3 - Music Player Screen
 * Full-screen professional audio player with gestures, animations, and advanced controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicPlayerScreen(
    viewModel: PlayerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.playerState.collectAsState()
    val track = state.currentTrack
    var showQueue by remember { mutableStateOf(false) }

    val gradientBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF1F2937), Color(0xFF111827))
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            PlayerTopBar(onClose = onClose)
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBrush)
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { track?.let { viewModel.toggleFavorite(it) } }
                        )
                    }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            if (delta > 50) viewModel.previousTrack()
                            else if (delta < -50) viewModel.nextTrack()
                        }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PlayerArtwork(
                    track = track,
                    modifier = Modifier.weight(1f)
                )

                TrackInfo(track = track, onFavoriteToggle = { track?.let { viewModel.toggleFavorite(it) } })

                PlayerProgressBar(
                    currentPositionMs = state.currentPositionMs,
                    durationMs = state.durationMs,
                    onSeek = { viewModel.seekTo(it) }
                )

                PlayerControls(
                    isPlaying = state.isPlaying,
                    isShuffle = state.isShuffle,
                    repeatMode = state.repeatMode,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.nextTrack() },
                    onPrevious = { viewModel.previousTrack() },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() }
                )

                PlayerBottomActions(
                    onOpenQueue = { showQueue = true },
                    playbackSpeed = state.playbackSpeed,
                    onSetSpeed = { viewModel.setPlaybackSpeed(it) }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            containerColor = Color(0xFF111827),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
        ) {
            PlayerQueueSheet(
                queue = state.queue,
                currentIndex = state.currentIndex,
                onTrackClick = { viewModel.playFromQueue(it) },
                onRemoveTrack = { index -> 
                    val trackToRemove = state.queue.getOrNull(index)
                    trackToRemove?.let { viewModel.removeFromQueue(it.id) }
                },
                onReorderTrack = { from, to -> /* Handle reorder */ },
                onClearQueue = { viewModel.clearQueue() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerTopBar(onClose: () -> Unit) {
    TopAppBar(
        title = { Text("Ahora Suena", color = Color.White, fontSize = 16.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = { /* More options */ }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
private fun TrackInfo(
    track: AudioTrackEntity?,
    onFavoriteToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                track?.title ?: "Sin título",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track?.artist ?: "Artista desconocido",
                color = Color(0xFF38BDF8),
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onFavoriteToggle) {
            Icon(
                if (track?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (track?.isFavorite == true) Color.Red else Color.White
            )
        }
    }
}

@Composable
private fun PlayerBottomActions(
    onOpenQueue: () -> Unit,
    playbackSpeed: Float,
    onSetSpeed: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = { 
            val nextSpeed = if (playbackSpeed >= 2f) 0.5f else playbackSpeed + 0.5f
            onSetSpeed(nextSpeed)
        }) {
            Text("${playbackSpeed}x", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
        }
        
        IconButton(onClick = { /* Share */ }) {
            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = Color.White)
        }

        IconButton(onClick = onOpenQueue) {
            Icon(Icons.Rounded.QueueMusic, contentDescription = "Queue", tint = Color.White)
        }
    }
}
