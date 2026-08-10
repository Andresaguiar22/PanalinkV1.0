package com.example.media.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.media.audio.AudioTrackEntity
import com.example.media.ui.components.TrackItem
import com.example.media.playlist.PlaylistEntity

/**
 * P6.7 - Playlist Detail Screen
 * Displays playlist cover, tracks list, play all button, and sharing functionality within PanaLink.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlist: PlaylistEntity,
    songs: List<AudioTrackEntity>,
    userRole: com.example.media.playlist.PlaylistMemberRole = com.example.media.playlist.PlaylistMemberRole.VIEWER,
    onBackClick: () -> Unit,
    onPlayAllClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onPlayTrackClick: (AudioTrackEntity) -> Unit,
    onSharePlaylistClick: () -> Unit,
    onCollaboratorsClick: () -> Unit,
    onGenerateCoverClick: () -> Unit,
    onRemoveTrackClick: (AudioTrackEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState()
    var showOptionsSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedTrack by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<AudioTrackEntity?>(null) }

    Scaffold(
        containerColor = Color(0xFF0F172A),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    if (userRole.canGenerateAI()) {
                        IconButton(onClick = onGenerateCoverClick) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "IA Cover", tint = Color(0xFFFF007A))
                        }
                    }
                    IconButton(onClick = onCollaboratorsClick) {
                        Icon(Icons.Default.Group, contentDescription = "Colaboradores", tint = Color(0xFF10B981))
                    }
                    if (userRole.canShare()) {
                        IconButton(onClick = onSharePlaylistClick) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir", tint = Color(0xFF38BDF8))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = modifier
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding()),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Hero Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF1E293B))
                            .clickable(enabled = userRole.canEditMetadata()) { onGenerateCoverClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!playlist.coverPath.isNullOrEmpty()) {
                            AsyncImage(
                                model = playlist.coverPath,
                                contentDescription = playlist.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(80.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = playlist.name,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    
                    Text(
                        text = playlist.description ?: "Playlist de PanaLink",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when(userRole) {
                                com.example.media.playlist.PlaylistMemberRole.OWNER -> "Tú (Propietario)"
                                com.example.media.playlist.PlaylistMemberRole.EDITOR -> "Editor"
                                com.example.media.playlist.PlaylistMemberRole.VIEWER -> "Espectador"
                            },
                            color = Color.Gray, 
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${songs.size} canciones", color = Color.Gray, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            IconButton(onClick = { /* Favorite */ }) {
                                Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = Color.White)
                            }
                            IconButton(onClick = { /* Download */ }) {
                                Icon(Icons.Default.Download, contentDescription = null, tint = Color.White)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onShuffleClick) {
                                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = Color.White)
                            }
                            FloatingActionButton(
                                onClick = onPlayAllClick,
                                containerColor = Color(0xFF38BDF8),
                                contentColor = Color.Black,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Reproducir Todo", modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }

            // Tracks List
            items(songs) { track ->
                TrackItem(
                    track = track,
                    onClick = { onPlayTrackClick(track) },
                    onTrackOptionsClick = {
                        selectedTrack = it
                        showOptionsSheet = true
                    }
                )
            }
        }
    }

    if (showOptionsSheet && selectedTrack != null) {
        ModalBottomSheet(
            onDismissRequest = { showOptionsSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1E293B)
        ) {
            TrackOptionsBottomSheet(
                track = selectedTrack!!,
                userRole = userRole,
                onPlayNext = { /* TODO */ },
                onAddToPlaylist = { /* TODO */ },
                onFavorite = { /* TODO */ },
                onEditMetadata = { /* TODO */ },
                onShare = { /* TODO */ },
                onDelete = { onRemoveTrackClick(selectedTrack!!) },
                onDismiss = { showOptionsSheet = false }
            )
        }
    }
}
