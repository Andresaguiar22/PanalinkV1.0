package com.example.ui.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.launch
import com.example.data.repository.UploadRepository
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateChannelDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, avatarUrl: String, coverUrl: String, visibility: String, isReadonly: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf("public") } // public or private
    var isReadonly by remember { mutableStateOf(false) }
    
    var step by remember { mutableStateOf(1) } // 1: Info, 2: Appearance, 3: Settings

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        content = {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF111B21),
                tonalElevation = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Nuevo Canal 🔥",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Step Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            val currentStep = index + 1
                            Box(
                                modifier = Modifier
                                    .size(if (step == currentStep) 12.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (step == currentStep) Color(0xFFD500F9) else Color.Gray.copy(alpha = 0.3f))
                            )
                            if (index < 2) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    when (step) {
                        1 -> {
                            // Step 1: Basic Info
                            Text("Paso 1: Información básica", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Nombre del Canal") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFD500F9),
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                ),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Descripción breve") },
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFFD500F9),
                                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                                ),
                                maxLines = 3
                            )
                        }
                        2 -> {
                            // Step 2: Appearance
                            Text("Paso 2: Apariencia visual", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            val coroutineScope = rememberCoroutineScope()
                            var isUploadingCover by remember { mutableStateOf(false) }
                            var isUploadingAvatar by remember { mutableStateOf(false) }

                            val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                                if (uri != null) {
                                    coroutineScope.launch {
                                        isUploadingCover = true
                                        try {
                                            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                                            if (bytes != null) {
                                                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                                                val result = com.example.data.repository.UploadRepository().uploadVideo(
                                                    mediaBytes = bytes,
                                                    mediaMimeType = mime,
                                                    caption = "cover",
                                                    userId = SupabaseClient.currentUser?.id ?: "",
                                                    fileNamePrefix = "channel_cover"
                                                )
                                                if (result.isSuccess) {
                                                    coverUrl = result.getOrNull()?.url ?: ""
                                                } else {
                                                    android.widget.Toast.makeText(context, "Error al subir portada", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } finally {
                                            isUploadingCover = false
                                        }
                                    }
                                }
                            }

                            val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
                                if (uri != null) {
                                    coroutineScope.launch {
                                        isUploadingAvatar = true
                                        try {
                                            val bytes = context.contentResolver.openInputStream(uri)?.readBytes()
                                            if (bytes != null) {
                                                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                                                val result = com.example.data.repository.UploadRepository().uploadVideo(
                                                    mediaBytes = bytes,
                                                    mediaMimeType = mime,
                                                    caption = "avatar",
                                                    userId = SupabaseClient.currentUser?.id ?: "",
                                                    fileNamePrefix = "channel_avatar"
                                                )
                                                if (result.isSuccess) {
                                                    avatarUrl = result.getOrNull()?.url ?: ""
                                                } else {
                                                    android.widget.Toast.makeText(context, "Error al subir avatar", android.widget.Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } finally {
                                            isUploadingAvatar = false
                                        }
                                    }
                                }
                            }

                            // Portada Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF202C33))
                                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { coverPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (coverUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = coverUrl,
                                        contentDescription = "Portada",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                                } else if (isUploadingCover) {
                                    CircularProgressIndicator(color = Color(0xFFD500F9))
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Añadir Portada", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Avatar Preview
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF202C33))
                                        .border(2.dp, Color(0xFFD500F9), CircleShape)
                                        .clickable { avatarPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (avatarUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                                    } else if (isUploadingAvatar) {
                                        CircularProgressIndicator(color = Color(0xFFD500F9), modifier = Modifier.size(24.dp))
                                    } else {
                                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Foto de Perfil del Canal", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text("Toca el círculo para subir una foto", color = Color.Gray, fontSize = 12.sp)
                                }
                            }
                        }
                        3 -> {
                            // Step 3: Settings
                            Text("Paso 3: Configuración", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Visibilidad
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF202C33))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Visibilidad", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (visibility == "public") "Cualquiera puede encontrar el canal" else "Solo por invitación",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = visibility == "public",
                                    onCheckedChange = { visibility = if (it) "public" else "private" },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFD500F9),
                                        checkedTrackColor = Color(0xFFD500F9).copy(alpha = 0.5f)
                                    )
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Solo lectura
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF202C33))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Solo Lectura", color = Color.White, fontWeight = FontWeight.Bold)
                                    Text(
                                        if (isReadonly) "Solo tú puedes publicar" else "Los miembros pueden interactuar",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = isReadonly,
                                    onCheckedChange = { isReadonly = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFFD500F9),
                                        checkedTrackColor = Color(0xFFD500F9).copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (step > 1) {
                            TextButton(onClick = { step-- }) {
                                Text("Atrás", color = Color.Gray)
                            }
                        } else {
                            TextButton(onClick = onDismiss) {
                                Text("Cancelar", color = Color.Gray)
                            }
                        }

                        if (step < 3) {
                            Button(
                                onClick = { if (name.isNotEmpty()) step++ },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD500F9))
                            ) {
                                Text("Siguiente")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (name.isNotEmpty()) {
                                        onCreate(name, description, avatarUrl, coverUrl, visibility, isReadonly)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD500F9))
                            ) {
                                Text("Crear Canal 🚀")
                            }
                        }
                    }
                }
            }
        }
    )
}
