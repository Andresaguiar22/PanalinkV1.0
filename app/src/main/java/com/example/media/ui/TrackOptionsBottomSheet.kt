package com.example.media.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.media.audio.AudioTrackEntity

@Composable
fun TrackOptionsBottomSheet(
    track: AudioTrackEntity,
    userRole: com.example.media.playlist.PlaylistMemberRole = com.example.media.playlist.PlaylistMemberRole.VIEWER,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onFavorite: () -> Unit,
    onEditMetadata: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(track.title, color = Color.White, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
        
        OptionItem(Icons.Default.PlayArrow, "Reproducir siguiente", onClick = { onPlayNext(); onDismiss() })
        
        // Todos pueden agregar a SUS propias playlists
        OptionItem(Icons.Default.PlaylistAdd, "Agregar a mis playlists", onClick = { onAddToPlaylist(); onDismiss() })
        
        OptionItem(if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", onClick = { onFavorite(); onDismiss() })
        
        if (userRole.canEditMetadata()) {
            OptionItem(Icons.Default.Edit, "Editar metadatos de pista", onClick = { onEditMetadata(); onDismiss() })
        }
        
        OptionItem(Icons.Default.Share, "Compartir pista", onClick = { onShare(); onDismiss() })
        
        if (userRole.canManageTracks()) {
            OptionItem(Icons.Default.Delete, "Quitar de esta playlist", color = Color.Red, onClick = { onDelete(); onDismiss() })
        }
    }
}

@Composable
private fun OptionItem(icon: ImageVector, label: String, color: Color = Color.White, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, color = color, fontSize = 16.sp)
    }
}
