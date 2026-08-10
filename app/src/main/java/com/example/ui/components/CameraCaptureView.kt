package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun CameraCaptureView(
    mode: String, // "photo", "video", or "any"
    onMediaCaptured: (Uri, String) -> Unit, // Uri and type ("image" or "video")
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val requiredPermissions = remember {
        mutableStateListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }

    var permissionsGranted by remember {
        mutableStateOf(
            requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        permissionsGranted = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            launcher.launch(requiredPermissions.toTypedArray())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_capture_view_container")
    ) {
        if (permissionsGranted) {
            CameraPreviewAndControls(
                mode = mode,
                onMediaCaptured = onMediaCaptured,
                onDismiss = onDismiss
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Permisos de Cámara y Audio Requeridos",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Para capturar fotos y grabar videos reales en Panalink, necesitamos acceso a la cámara y al micrófono del dispositivo.",
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { launcher.launch(requiredPermissions.toTypedArray()) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF85))
                ) {
                    Text("Conceder Permisos 📸", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancelar", color = Color.White)
                }
            }
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun CameraPreviewAndControls(
    mode: String,
    onMediaCaptured: (Uri, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) }
    var currentRecording by remember { mutableStateOf<Recording?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    // Instanciar UseCases
    val preview = remember { Preview.Builder().build() }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(
                    Quality.HD,
                    FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)
                )
            )
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }

    // Controlar el inicio del camera provider
    LaunchedEffect(lensFacing) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        cameraProvider.unbindAll()

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
        val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            try {
                context.display?.rotation ?: android.view.Surface.ROTATION_0
            } catch (e: Exception) {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.rotation
            }
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.rotation
        }
        imageCapture.setTargetRotation(rotation)
        videoCapture.setTargetRotation(rotation)

        try {
            // Bind use cases
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                videoCapture
            )
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e: Exception) {
            Log.e("CameraCaptureView", "Error binding CameraX use cases", e)
        }
    }

    // Timer de grabación
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingDuration = 0L
            while (isRecording) {
                delay(1000)
                recordingDuration += 1
            }
        }
    }

    // Liberar cámara al salir (onDispose)
    DisposableEffect(Unit) {
        onDispose {
            currentRecording?.stop()
            currentRecording = null
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraCaptureView", "Error releasing camera provider on dispose", e)
            }
        }
    }

    var showEffectsMenu by remember { mutableStateOf(false) }
    var showMusicMenu by remember { mutableStateOf(false) }
    var selectedEffect by remember { mutableStateOf("Ninguno") }
    var selectedSpeed by remember { mutableStateOf(1.0f) }
    var selectedMusic by remember { mutableStateOf<String?>(null) }
    val effects = listOf("Ninguno", "TikTok Beauty", "Filtro VHS", "Cyberpunk", "Blanco y Negro")
    val speeds = listOf(0.3f, 0.5f, 1.0f, 2.0f, 3.0f)
    val musicTracks = listOf("Sin Música", "Phonk Viral", "Lofi Chill", "Reggaeton Beat")

    // ExoPlayer for background music during recording
    val exoPlayer = remember {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            volume = 0.5f
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(selectedMusic, isRecording) {
        if (selectedMusic != null && selectedMusic != "Sin Música") {
            // Simulated music loading. In a real app, load actual URI
            if (isRecording) {
                // In a real app, ExoPlayer would play the music while recording for Audio Mixing
                // exoPlayer.play() 
            } else {
                // exoPlayer.pause()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Live camera preview view
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        
        // Color Filter Overlay for Effects
        if (selectedEffect != "Ninguno") {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val color = when (selectedEffect) {
                    "TikTok Beauty" -> Color(0xFFFFB6C1).copy(alpha = 0.2f) // Soft pink glow
                    "Filtro VHS" -> Color(0xFF00FFFF).copy(alpha = 0.15f) // Cyan tint
                    "Cyberpunk" -> Color(0xFF8A2BE2).copy(alpha = 0.3f) // Purple/neon tint
                    "Blanco y Negro" -> Color.Black.copy(alpha = 0.5f) // Desaturated look
                    else -> Color.Transparent
                }
                val blendMode = when (selectedEffect) {
                    "Blanco y Negro" -> androidx.compose.ui.graphics.BlendMode.Saturation
                    "Filtro VHS" -> androidx.compose.ui.graphics.BlendMode.ColorBurn
                    else -> androidx.compose.ui.graphics.BlendMode.Overlay
                }
                drawRect(color = color, blendMode = blendMode)
            }
        }

        // Gradient overlay for visual clarity of controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.5f)
                        )
                    )
                )
        )

        // Sidebar Controls
        if (!isRecording) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Effects Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showEffectsMenu = !showEffectsMenu },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.FaceRetouchingNatural, contentDescription = "Efectos", tint = if (selectedEffect != "Ninguno") Color(0xFFE040FB) else Color.White)
                    }
                    Text("Efectos", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Music Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showMusicMenu = !showMusicMenu },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = "Música", tint = if (selectedMusic != null && selectedMusic != "Sin Música") Color(0xFF00FF85) else Color.White)
                    }
                    Text("Música", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                // Speed Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { 
                            val nextIndex = (speeds.indexOf(selectedSpeed) + 1) % speeds.size
                            selectedSpeed = speeds[nextIndex]
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Text("${selectedSpeed}x", color = if (selectedSpeed != 1.0f) Color(0xFFFFD700) else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Velocidad", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        // Effects Menu Overlay
        if (showEffectsMenu) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 140.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Filtros AR & Belleza ✨", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(effects) { effect ->
                            val active = selectedEffect == effect
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (active) Color(0xFFE040FB) else Color.White.copy(alpha = 0.2f))
                                    .clickable { selectedEffect = effect }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(effect, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        
        // Music Menu Overlay
        if (showMusicMenu) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 140.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("Selecciona una Pista 🎵", color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(musicTracks) { track ->
                            val active = selectedMusic == track
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (active) Color(0xFF00FF85) else Color.White.copy(alpha = 0.2f))
                                    .clickable { selectedMusic = track }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(track, color = if (active) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Top Bar Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("camera_close_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
            }

            if (isRecording) {
                // Recording indicator banner
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = if (mode == "photo") "FOTO 📸" else if (mode == "video") "VIDEO 📹" else "ESTUDIO PANALINK",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            IconButton(
                onClick = {
                    if (!isRecording) {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }
                },
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    .testTag("camera_flip_button")
            ) {
                Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Cambiar Cámara", tint = Color.White)
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Capture Button Outer Circle
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .border(4.dp, Color.White, CircleShape)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                // Inner button
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (isRecording) Color.Red else Color(0xFF00FF85))
                        .clickable {
                            if (mode == "photo") {
                                // Take Photo
                                val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                                val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                    try {
                                        context.display?.rotation ?: android.view.Surface.ROTATION_0
                                    } catch (e: Exception) {
                                        @Suppress("DEPRECATION")
                                        windowManager.defaultDisplay.rotation
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    windowManager.defaultDisplay.rotation
                                }
                                imageCapture.setTargetRotation(rotation)

                                val file = File(context.cacheDir, "captured_photo_${System.currentTimeMillis()}.jpg")
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                                imageCapture.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            val savedUri = Uri.fromFile(file)
                                            onMediaCaptured(savedUri, "image")
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("CameraCaptureView", "Error saving captured photo", exception)
                                        }
                                    }
                                )
                            } else {
                                // Record Video
                                if (isRecording) {
                                    currentRecording?.stop()
                                    currentRecording = null
                                    isRecording = false
                                } else {
                                    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
                                    val rotation = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                        try {
                                            context.display?.rotation ?: android.view.Surface.ROTATION_0
                                        } catch (e: Exception) {
                                            @Suppress("DEPRECATION")
                                            windowManager.defaultDisplay.rotation
                                        }
                                    } else {
                                        @Suppress("DEPRECATION")
                                        windowManager.defaultDisplay.rotation
                                    }
                                    videoCapture.setTargetRotation(rotation)

                                    val file = File(context.cacheDir, "captured_video_${System.currentTimeMillis()}.mp4")
                                    val fileOutputOptions = FileOutputOptions.Builder(file).build()

                                    val recordAudioGranted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED

                                    var recordingBuilder = videoCapture.output.prepareRecording(context, fileOutputOptions)
                                    if (recordAudioGranted) {
                                        recordingBuilder = recordingBuilder.withAudioEnabled()
                                    }

                                    isRecording = true
                                    currentRecording = recordingBuilder.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
                                        if (recordEvent is VideoRecordEvent.Finalize) {
                                            isRecording = false
                                            if (!recordEvent.hasError()) {
                                                val savedUri = Uri.fromFile(file)
                                                onMediaCaptured(savedUri, "video")
                                            } else {
                                                Log.e("CameraCaptureView", "Video recording error: ${recordEvent.error}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        .testTag("camera_trigger_button")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isRecording) "Toca para detener la grabación" 
                       else if (mode == "photo") "Toca para tomar foto" 
                       else "Toca para grabar video",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
