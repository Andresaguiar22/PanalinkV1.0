package com.example.ui.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.supabase.SupabaseClient
import com.example.ui.viewmodel.ProfileViewModel

@Composable
fun ReelsGrid(viewModel: ProfileViewModel, onNavigateToReel: (String) -> Unit) {
    val reelsState by viewModel.reelsState.collectAsState()
    val currentUid = SupabaseClient.currentUser?.id ?: ""
    var reelToDelete by remember { mutableStateOf<com.example.data.model.UserState?>(null) }
    
    if (reelsState.isSuccess) {
        val reels = reelsState.getOrNull() ?: emptyList()
        if (reels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Aún no has publicado Reels",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            val chunkedReels = reels.chunked(3)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                chunkedReels.forEach { rowReels ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (i in 0 until 3) {
                            if (i < rowReels.size) {
                                val reel = rowReels[i]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.75f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E1E24))
                                        .clickable { onNavigateToReel(reel.state.id) }
                                        .pointerInput(reel.state.id) {
                                            detectTapGestures(
                                                onLongPress = {
                                                    reelToDelete = reel.state
                                                }
                                            )
                                        }
                                ) {
                                    AsyncImage(
                                        model = reel.state.mediaUrl,
                                        contentDescription = "Reel",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                                )
                                            )
                                    )

                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = (reel.state.viewsCount ?: 0).toString(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    } else {
        Text("Error al cargar reels: ${reelsState.exceptionOrNull()?.message}", color = Color.Red, modifier = Modifier.padding(16.dp))
    }

    if (reelToDelete != null) {
        AlertDialog(
            onDismissRequest = { reelToDelete = null },
            title = { Text("¿Borrar Reel?", color = Color.White) },
            text = { Text("Esta acción no se puede deshacer.", color = Color.White.copy(alpha = 0.7f)) },
            containerColor = Color(0xFF1E2D35),
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteReel(reelToDelete!!.id, currentUid)
                    reelToDelete = null
                }) {
                    Text("Borrar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { reelToDelete = null }) {
                    Text("Cancelar", color = Color.White)
                }
            }
        )
    }
}
