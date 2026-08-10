package com.example.media.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.media.playlist.PlaylistEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistMetadataEditor(
    playlist: PlaylistEntity,
    onSave: (String, String, String?) -> Unit,
    onOpenCoverStudio: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(playlist.name) }
    var description by remember { mutableStateOf(playlist.description ?: "") }
    var coverPath by remember { mutableStateOf(playlist.coverPath) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Playlist") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = null) } },
                actions = { Button(onClick = { onSave(name, description, coverPath) }) { Text("Guardar") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Gray)
                    .clickable { onOpenCoverStudio() }
            ) {
                if (coverPath != null) {
                    AsyncImage(model = coverPath, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.align(Alignment.Center))
                }
                Text("Cambiar Portada", color = Color.White, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).background(Color.Black.copy(alpha = 0.5f)).fillMaxWidth().padding(4.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
        }
    }
}
