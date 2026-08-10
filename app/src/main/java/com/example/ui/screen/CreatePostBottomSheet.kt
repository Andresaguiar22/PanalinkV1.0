package com.example.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.viewmodel.CreatePostViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostBottomSheet(
    onDismiss: () -> Unit,
    viewModel: CreatePostViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showPostStudio by remember { mutableStateOf(false) }

    if (showPostStudio) {
        PostStudioScreen(
            onDismiss = {
                showPostStudio = false
                onDismiss()
            },
            initialUris = uiState.selectedMediaUris
        )
        return
    }
    
    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.addMediaUris(uris)
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            viewModel.addAudioUris(uris)
        }
    }

    var youtubeUrl by remember { mutableStateOf("") }
    var showYoutubeInput by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF020617),
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
                
                Text(
                    text = "Nuevo Post",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { showPostStudio = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .padding(end = 6.dp)
                    ) {
                        Text(
                            "Studio Pro ✨",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = { 
                            if (youtubeUrl.isNotBlank()) {
                                val cleanUrl = youtubeUrl.trim()
                                val prefix = if (uiState.content.isNotBlank()) "\n\n" else ""
                                viewModel.onContentChanged(uiState.content + prefix + cleanUrl)
                            }
                            val currentUserId = com.example.data.supabase.SupabaseClient.currentUser?.id ?: ""
                            viewModel.publishPost(currentUserId) { onDismiss() } 
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        enabled = uiState.content.isNotBlank() || uiState.selectedMediaUris.isNotEmpty() || youtubeUrl.isNotBlank(),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            "Publicar",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profile Picture + Input Area
            Row(modifier = Modifier.fillMaxWidth()) {
                val avatarUrl = com.example.data.supabase.SupabaseClient.currentUser?.userMetadata?.get("avatar_url")?.toString()?.replace("\"", "") ?: "https://ui-avatars.com/api/?name=Pana&background=0F172A&color=fff"
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    // Privacy Selector Toggle
                    Surface(
                        onClick = { 
                            val next = if (uiState.privacy == "PUBLIC") "PANAS" else "PUBLIC"
                            viewModel.onPrivacyChanged(next)
                        },
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (uiState.privacy == "PUBLIC") Icons.Default.Public else Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (uiState.privacy == "PUBLIC") Color(0xFF00E5FF) else Color(0xFFFFD600),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.privacy == "PUBLIC") "Público" else "Solo Panas",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    TextField(
                        value = uiState.content,
                        onValueChange = { if (it.length <= 500) viewModel.onContentChanged(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp),
                        placeholder = { 
                            Text(
                                "¿Qué está pasando, Pana?", 
                                color = Color.Gray, 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.Normal
                            ) 
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color(0xFF00E5FF)
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            if (uiState.selectedMediaUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(start = 52.dp, end = 4.dp)
                ) {
                    items(uiState.selectedMediaUris) { uri ->
                        val isAudio = uri.toString().contains("audio") || uri.toString().endsWith(".mp3") || uri.toString().endsWith(".m4a") || uri.toString().endsWith(".wav") || uri.toString().endsWith(".aac")
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                        ) {
                            if (isAudio) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF262629)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "Audio", tint = Color(0xFFD500F9), modifier = Modifier.size(32.dp))
                                }
                            } else {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeMediaUri(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                                    .padding(4.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (showYoutubeInput) {
                OutlinedTextField(
                    value = youtubeUrl,
                    onValueChange = { 
                        youtubeUrl = it
                        if (it.contains("youtube.com") || it.contains("youtu.be")) {
                            viewModel.fetchLinkPreview(it)
                        }
                    },
                    label = { Text("Enlace de Video de YouTube", color = Color(0xFFFF0000)) },
                    placeholder = { Text("https://www.youtube.com/watch?v=...", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF0000),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedLabelColor = Color(0xFFFF0000),
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            uiState.preview?.let { preview ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = preview.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(preview.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                            Text("YouTube", color = Color.Gray, fontSize = 12.sp)
                        }
                        IconButton(onClick = { /* Implementar eliminar preview en VM */ }) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = Color.Gray)
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Accessory Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    IconButton(onClick = { mediaPicker.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Galería Fotos", tint = Color(0xFF00E5FF))
                    }
                    IconButton(onClick = { mediaPicker.launch("video/*") }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Videos", tint = Color(0xFF00FF85))
                    }
                    IconButton(onClick = { audioPicker.launch("audio/*") }) {
                        Icon(Icons.Default.Mic, contentDescription = "Audio", tint = Color(0xFFD500F9))
                    }
                    IconButton(onClick = { showYoutubeInput = !showYoutubeInput }) {
                        Icon(Icons.Default.PlayCircle, contentDescription = "YouTube", tint = Color(0xFFFF0000))
                    }
                }
                
                Text("${uiState.content.length}/500", color = if (uiState.content.length > 450) Color.Red else Color.Gray, fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
