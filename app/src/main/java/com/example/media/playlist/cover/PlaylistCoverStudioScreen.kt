package com.example.media.playlist.cover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.creative.canvas.CanvasEditorEngine
import com.example.creative.ui.StudioLayout

/**
 * P6.7.4 - Playlist Cover Studio Screen
 * Professional UI for designing playlist covers with real-time preview and layer control.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistCoverStudioScreen(
    viewModel: PlaylistCoverViewModel,
    onBack: () -> Unit,
    onFinish: (String) -> Unit
) {
    val project by viewModel.project.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Playlist Cover Studio", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { viewModel.exportAndApply(onFinish) }) {
                            Icon(Icons.Default.Check, contentDescription = "Listo", tint = Color(0xFF38BDF8))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111827))
            )
        },
        bottomBar = {
            CoverStudioControls(
                onAddText = { /* viewModel.addLayer(...) */ },
                onAddImage = { /* Open picker */ },
                onAddSticker = { /* Open stickers */ },
                onUndo = { /* viewModel.undo() */ }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            project?.let { proj ->
                // This would normally be the CanvasEditorEngine in a Composable wrapper
                Box(
                    modifier = Modifier
                        .size(350.dp)
                        .background(Color.DarkGray)
                ) {
                    Text(
                        "Preview del Canvas", 
                        color = Color.White, 
                        modifier = Modifier.align(Alignment.Center)
                    )
                    // Integration with CanvasEditorEngine goes here
                }
            } ?: CircularProgressIndicator()
        }
    }
}

@Composable
fun CoverStudioControls(
    onAddText: () -> Unit,
    onAddImage: () -> Unit,
    onAddSticker: () -> Unit,
    onUndo: () -> Unit
) {
    Surface(
        color = Color(0xFF1F2937),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = onAddText) {
                Icon(Icons.Default.TextFields, contentDescription = "Texto", tint = Color.White)
            }
            IconButton(onClick = onAddImage) {
                Icon(Icons.Default.Image, contentDescription = "Imagen", tint = Color.White)
            }
            IconButton(onClick = onAddSticker) {
                Icon(Icons.Default.EmojiEmotions, contentDescription = "Sticker", tint = Color.White)
            }
            IconButton(onClick = onUndo) {
                Icon(Icons.Default.Undo, contentDescription = "Deshacer", tint = Color.White)
            }
        }
    }
}
