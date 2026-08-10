package com.example.features.stickers.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Videocam
import android.widget.Toast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StickerEditorScreen(
    onBack: () -> Unit,
    onStickerCreated: (String) -> Unit,
    viewModel: StickerEditorViewModel = viewModel()
) {
    val context = LocalContext.current
    val isProcessing by viewModel.isProcessing.collectAsState()
    val processedImageUri by viewModel.processedImageUri.collectAsState()
    val successUrl by viewModel.successUrl.collectAsState()

    var emoji by remember { mutableStateOf("😀") }

    LaunchedEffect(successUrl) {
        successUrl?.let {
            onStickerCreated(it)
            viewModel.reset()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.processImage(context, it) }
    }
    
    
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.processVideo(context, it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let {
            val tempFile = java.io.File(context.cacheDir, "camera_sticker_${java.util.UUID.randomUUID()}.jpg")
            java.io.FileOutputStream(tempFile).use { out ->
                it.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            }
            viewModel.processImage(context, Uri.fromFile(tempFile))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Sticker", fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF111B21),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF111B21)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                CircularProgressIndicator(color = Color(0xFF00A884))
            } else if (processedImageUri != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(250.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2A3942))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = processedImageUri,
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                        if (processedImageUri?.path?.endsWith(".mp4") == true) {
                            Icon(Icons.Default.Videocam, contentDescription = "Sticker Animado", tint = Color.White, modifier = Modifier.size(48.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = emoji,
                        onValueChange = { emoji = it },
                        label = { Text("Emoji asociado (Opcional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00A884),
                            focusedLabelColor = Color(0xFF00A884)
                        ),
                        modifier = Modifier.fillMaxWidth(0.7f)
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Button(
                        onClick = { viewModel.saveSticker(context, emoji) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text("Guardar Sticker", color = Color.White)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { viewModel.reset() }) {
                        Text("Descartar", color = Color.Red)
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Selecciona una imagen", color = Color.White, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(
                            onClick = { galleryLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3942))
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = "Imagen")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Imagen")
                        }
                        Button(
                            onClick = { videoLauncher.launch("video/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3942))
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = "Vídeo")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Vídeo")
                        }
                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3942))
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = "Cámara")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cámara")
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                }
            }
        }
    }
}
