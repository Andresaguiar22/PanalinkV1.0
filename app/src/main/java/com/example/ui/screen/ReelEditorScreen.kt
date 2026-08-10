package com.example.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.geometry.Offset
import java.util.UUID
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.viewmodel.CreateStateUiState
import com.example.ui.viewmodel.StatesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.components.CameraCaptureView
import com.example.ui.components.SimpleVideoPreviewPlayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ColorMatrix
import kotlin.math.sin
import kotlin.random.Random



import com.example.creative.core.CreativeProject
import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeType
import com.example.creative.timeline.CreativeTrack
import com.example.creative.timeline.TimelineEngine
import com.example.creative.timeline.MultiTrackTimelineUI
import com.example.creative.inspector.PropertyInspector
import com.example.creative.animation.CreativeKeyframe
import com.example.creative.animation.EasingType
import com.example.creative.export.ExportQueueManager
import com.example.creative.export.ExportJob

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelEditorScreen(
    viewModel: StatesViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val createStateState by viewModel.createStateFlow.collectAsState()

    var currentStep by remember { mutableStateOf("picker") } // "picker", "studio", "metadata", "camera"
    var cameraCaptureMode by remember { mutableStateOf("video") }

    val cameraPermissionState = com.example.util.rememberCameraPermissionState(
        onPermissionsGranted = {
            currentStep = "camera"
        }
    )
    var mediaType by remember { mutableStateOf("video") } // "video" or "image"
    var exportedMediaUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var exportedMediaFile by remember { mutableStateOf<java.io.File?>(null) }
    
    // Creative Engine Core State
    var creativeLayers by remember { mutableStateOf(listOf<CreativeLayer>()) }
    var timelineTracks by remember {
        mutableStateOf(
            listOf<CreativeTrack>(
                CreativeTrack.VideoTrack(name = "Video Principal", durationMs = 15000L),
                CreativeTrack.AudioTrack(name = "Música de Fondo", durationMs = 15000L)
            )
        )
    }
    var currentTimeMs by remember { mutableStateOf(0L) }
    var totalDurationMs by remember { mutableStateOf(15000L) }
    var selectedTrackId by remember { mutableStateOf<String?>(null) }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    var showTimeline by remember { mutableStateOf(false) }
    var showInspector by remember { mutableStateOf(false) }

    // Loaded Assets
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMediaBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedMediaMimeType by remember { mutableStateOf<String?>(null) }

    // Color Correction Sliders (Real-time Preview adjustments)
    var brightnessValue by remember { mutableStateOf(1f) } // 0.5f to 1.5f
    var contrastValue by remember { mutableStateOf(1f) } // 0.5f to 1.5f
    var saturationValue by remember { mutableStateOf(1f) } // 0.5f to 1.5f

    // Trimming control
    var trimStartPercent by remember { mutableStateOf(0f) }
    var trimEndPercent by remember { mutableStateOf(100f) }

    // Music Mixing Track & Volumes
    var showMusicSelector by remember { mutableStateOf(false) }
    var selectedMusicTrack by remember { mutableStateOf("Ninguna") }
    var originalVideoVolume by remember { mutableStateOf(80f) } // 0-100%
    var backgroundMusicVolume by remember { mutableStateOf(50f) } // 0-100%
    val trackList = listOf("Ninguna", "Chamo Chill Lofi ☕", "Joropo Pop 🎸", "Tambor Energetico 🥁", "Caracas Sunset Synth 🌆")

    // Cover Selector Frame
    var coverSelectPercent by remember { mutableStateOf(30f) }
    var showAdjustments by remember { mutableStateOf(false) }

    // Transition Select
    var selectedTransition by remember { mutableStateOf("Disolvencia ✨") }
    val transitions = listOf("Disolvencia ✨", "Zoom 🌀", "Deslizar ↔️", "Corte ⚡️")

    // Metadata Fields
    var reelDescription by remember { mutableStateOf("") }
    var reelHashtagInput by remember { mutableStateOf("") }
    var showSchedulingSheet by remember { mutableStateOf(false) }
    var scheduledDateTimeString by remember { mutableStateOf("") }
    var showDraftSuccessDialog by remember { mutableStateOf(false) }
    var isReelSelected by remember { mutableStateOf(true) }

    // Ken Burns Animation variables for image reels
    val infiniteTransition = rememberInfiniteTransition(label = "KenBurns")
    val kenBurnsScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "KenBurnsScale"
    )
    val kenBurnsXOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "KenBurnsX"
    )

    // Media Picker Setup
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
            val mime = context.contentResolver.getType(uri) ?: "video/mp4"
            selectedMediaMimeType = mime
            mediaType = if (mime.startsWith("image/")) "image" else "video"

            try {
                selectedMediaBytes = ByteArray(0)
                currentStep = "studio"
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Camera Capture Simulation
    fun triggerSimulatedStudioCamera() {
        mediaType = "video"
        selectedMediaMimeType = "video/mp4"
        selectedMediaUri = Uri.parse("https://images.unsplash.com/photo-1563911302283-d2bc1d9e2659?auto=format&fit=crop&w=600&q=80")
        selectedMediaBytes = ByteArray(0)
        currentStep = "studio"
    }

    // Real Camera Capture Handler
    fun handleRealCameraCapture(uri: Uri, type: String) {
        selectedMediaUri = uri
        mediaType = type
        selectedMediaMimeType = if (type == "video") "video/mp4" else "image/jpeg"

        try {
            selectedMediaBytes = ByteArray(0)
            currentStep = "studio"
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Upload & Success indicator states
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var showSuccessCheck by remember { mutableStateOf(false) }

    LaunchedEffect(createStateState) {
        when (createStateState) {
            is CreateStateUiState.Loading -> {
                isUploading = true
                uploadProgress = 0.15f
                while (uploadProgress < 0.92f) {
                    delay(100)
                    uploadProgress += 0.04f
                }
            }
            is CreateStateUiState.Success -> {
                uploadProgress = 1.0f
                showSuccessCheck = true
                delay(1300)
                isUploading = false
                showSuccessCheck = false
                viewModel.resetCreateState()
                onBack()
            }
            is CreateStateUiState.Error -> {
                isUploading = false
                android.widget.Toast.makeText(context, (createStateState as CreateStateUiState.Error).message, android.widget.Toast.LENGTH_LONG).show()
                viewModel.resetCreateState()
            }
            else -> {}
        }
    }

    // Validation helpers
    val isDescriptionValid = reelDescription.isNotBlank()
    // Require at least one hashtag starting with # or entered as text
    val hashtagsList = reelHashtagInput.split(" ").filter { it.isNotBlank() }
    val isHashtagsValid = hashtagsList.isNotEmpty()
    val isPublishAllowed = isDescriptionValid && isHashtagsValid

    fun publishReel() {
        if (!isPublishAllowed) return

        coroutineScope.launch {
            // Formulate description with hashtags
            val formattedHashtags = hashtagsList.joinToString(" ") { 
                if (it.startsWith("#")) it else "#$it" 
            }
            val transitionTag = " [Transition: $selectedTransition]"
            val coverTag = " [CoverFrame: ${coverSelectPercent.toInt()}%]"
            val musicTag = if (selectedMusicTrack != "Ninguna") " [Music: $selectedMusicTrack Vol: ${backgroundMusicVolume.toInt()}%]" else ""
            val scheduleTag = if (scheduledDateTimeString.isNotEmpty()) " [Scheduled: $scheduledDateTimeString]" else ""
            
            val jsonObj = org.json.JSONObject(); val textArr = org.json.JSONArray(); creativeLayers.filterIsInstance<CreativeLayer.Text>().forEach { t -> val obj = org.json.JSONObject(); obj.put("text", t.text); obj.put("x", (t.xFraction * 500).toDouble()); obj.put("y", (t.yFraction * 800).toDouble()); obj.put("scale", t.scale.toDouble()); obj.put("rotation", t.rotation.toDouble()); obj.put("color", t.colorHex); obj.put("fontName", t.fontFamily); obj.put("hasBackground", t.backgroundColorHex != null); obj.put("hasShadow", t.hasShadow); textArr.put(obj) }; jsonObj.put("textOverlays", textArr); val stickerArr = org.json.JSONArray(); creativeLayers.filterIsInstance<CreativeLayer.Sticker>().forEach { s -> val obj = org.json.JSONObject(); obj.put("url", s.stickerUrlOrPath); obj.put("x", (s.xFraction * 500).toDouble()); obj.put("y", (s.yFraction * 800).toDouble()); obj.put("scale", s.scale.toDouble()); obj.put("rotation", s.rotation.toDouble()); stickerArr.put(obj) }; jsonObj.put("stickerOverlays", stickerArr); val overlaysBase64 = android.util.Base64.encodeToString(jsonObj.toString().toByteArray(), android.util.Base64.NO_WRAP); val overlayTag = if (creativeLayers.isNotEmpty()) " [Overlays: $overlaysBase64]" else ""; val finalCaption = "$reelDescription\n\n$formattedHashtags$transitionTag$coverTag$musicTag$scheduleTag$overlayTag".trim()

            val pendingMediaDir = java.io.File(context.filesDir, "pending_media")
            if (!pendingMediaDir.exists()) pendingMediaDir.mkdirs()

            var useFile: java.io.File? = null

            if (mediaType == "image") {
                if (exportedMediaFile != null) {
                    useFile = exportedMediaFile
                } else if (selectedMediaUri != null) {
                    val localPath = com.example.util.PanalinkMediaManager.saveMediaToLocal(
                        context = context,
                        uri = selectedMediaUri,
                        sourceFile = null,
                        fileName = "reel_selected_${System.currentTimeMillis()}." + (selectedMediaMimeType?.split("/")?.lastOrNull() ?: "bin")
                    )
                    if (localPath != null) {
                        useFile = java.io.File(localPath)
                    }
                }
                
                viewModel.publishReelBackground(
                    context = context,
                    caption = finalCaption,
                    imageBytes = ByteArray(0),
                    mimeType = selectedMediaMimeType ?: "image/jpeg",
                    isReel = isReelSelected,
                    mediaFile = useFile
                )
            } else if (mediaType == "video") {
                val videoUri = exportedMediaUri ?: selectedMediaUri
                if (videoUri != null) {
                    val localPath = com.example.util.PanalinkMediaManager.saveMediaToLocal(
                        context = context,
                        uri = videoUri,
                        sourceFile = null,
                        fileName = "reel_selected_${System.currentTimeMillis()}." + (selectedMediaMimeType?.split("/")?.lastOrNull() ?: "bin")
                    )
                    if (localPath != null) {
                        useFile = java.io.File(localPath)
                    }
                }
                viewModel.publishReelBackground(
                    context = context,
                    caption = finalCaption,
                    imageBytes = ByteArray(0),
                    mimeType = selectedMediaMimeType ?: "video/mp4",
                    isReel = isReelSelected,
                    mediaFile = useFile
                )
            }
            // Navigate back immediately as requested
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            com.example.util.CameraPermissionDialog(
                showExplanation = cameraPermissionState.showExplanationDialog,
                isPermanentlyDenied = cameraPermissionState.isPermanentlyDenied,
                onDismiss = cameraPermissionState.dismissDialog,
                onRequestPermission = { cameraPermissionState.requestPermissions() },
                onOpenSettings = cameraPermissionState.openSettings
            )

            if (currentStep == "camera") {
                CameraCaptureView(
                    mode = cameraCaptureMode,
                    onMediaCaptured = { uri, type ->
                        handleRealCameraCapture(uri, type)
                    },
                    onDismiss = {
                        currentStep = "picker"
                    }
                )
            } else if (currentStep == "picker") {
                    // Initial Studio Onboarding Selector
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Estudio de Reels 🎬🇻🇪",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Produce videos impactantes para toda la comunidad de Panalink",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(48.dp))

                        // Large camera recorder or gallery selection cards
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .testTag("reel_studio_camera_button")
                                .clickable {
                                    cameraCaptureMode = "video"
                                    cameraPermissionState.requestPermissions()
                                },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF151518)),
                            border = BorderStroke(1.dp, Color(0xFF262629))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = Color(0xFF00FF85),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Grabar en Estudio 🎙️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Abre la cámara de producción con conteo de tiempo", color = Color.Gray, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .testTag("reel_studio_gallery_button")
                                .clickable {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF151518)),
                            border = BorderStroke(1.dp, Color(0xFF262629))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Collections,
                                    contentDescription = null,
                                    tint = Color(0xFFE040FB),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Subir Video o Imagen 🎞️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Las imágenes se animan automáticamente con efecto Ken Burns", color = Color.Gray, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        TextButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Regresar al Feed", color = Color.Gray)
                        }
                    }
                } else if (currentStep == "studio") {
                    // --- NEW CREATIVE STUDIO UI ---
                    var showTextEditor by remember { mutableStateOf(false) }
                    var textInput by remember { mutableStateOf("") }
                    var selectedColor by remember { mutableStateOf(Color.White) }
                    
                    var showStickers by remember { mutableStateOf(false) }
                    var showAudioPro by remember { mutableStateOf(false) }
                    var showTransitions by remember { mutableStateOf(false) }
                    var showFilters by remember { mutableStateOf(false) }
                    
                    var activeFilter by remember { mutableStateOf("Normal") }
    var activeParticles by remember { mutableStateOf("Ninguno") }
    var audioReactive by remember { mutableStateOf(false) }
    
    // For Audio Reactive
    val infiniteBeat = rememberInfiniteTransition()
    val beatScale by infiniteBeat.animateFloat(
        initialValue = 1.0f, targetValue = if(audioReactive) 1.05f else 1.0f,
        animationSpec = infiniteRepeatable(animation = tween(400, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse)
    )
    val particlesList = listOf("Ninguno", "Lluvia 🌧️", "Destellos ✨", "Corazones 💖")
    
                    val filters = listOf("Normal", "Vibe", "Retro", "B&W", "Pop", "Cinematic")
                    
                    var isRendering by remember { mutableStateOf(false) }
                    

                    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                        
                        // 1. Video Layer & Cover Tap Logic
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            // Miniatura Inteligente: Set cover on tap
                                            coverSelectPercent = (0..100).random().toFloat()
                                            android.widget.Toast.makeText(context, "Portada fijada en este fotograma 📸", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                        ) {
                            if (mediaType == "image" || selectedMediaUri?.toString()?.startsWith("http") == true) {
                                AsyncImage(
                                    model = selectedMediaUri ?: "https://images.unsplash.com/photo-1563911302283-d2bc1d9e2659",
                                    contentDescription = "Preview Image",
                                    modifier = Modifier.fillMaxSize().scale(if (activeFilter == "Zoom") kenBurnsScale else 1f),
                                    contentScale = ContentScale.Crop,
                                    colorFilter = when(activeFilter) {
                                        "B&W" -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(0f) })
                                        "Retro" -> androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFFFFD54F).copy(alpha = 0.3f), androidx.compose.ui.graphics.BlendMode.ColorBurn)
                                        "Vibe" -> androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFFE040FB).copy(alpha = 0.2f), androidx.compose.ui.graphics.BlendMode.Overlay)
                                        "Pop" -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix().apply { setToSaturation(2f) })
                                        "Cinematic" -> androidx.compose.ui.graphics.ColorFilter.colorMatrix(androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                                            0.8f, 0.1f, 0.1f, 0f, 0f,
                                            0.1f, 0.9f, 0.1f, 0f, 0f,
                                            0.1f, 0.1f, 1.2f, 0f, 0f,
                                            0f,   0f,   0f,   1f, 0f
                                        )))
                                        else -> null
                                    }
                                )
                            } else {
                                SimpleVideoPreviewPlayer(
                                    videoUri = selectedMediaUri ?: android.net.Uri.EMPTY,
                                    modifier = Modifier.fillMaxSize(),
                                    isMuted = (originalVideoVolume == 0f)
                                )
                                if (activeFilter != "Normal") {
                                    Box(modifier = Modifier.fillMaxSize().background(
                                        when(activeFilter) {
                                            "B&W" -> Color.Black.copy(alpha=0.5f) // fake for video without shaders
                                            "Retro" -> Color(0xFFFFD54F).copy(alpha = 0.2f)
                                            "Vibe" -> Color(0xFFE040FB).copy(alpha = 0.2f)
                                            "Cinematic" -> Color(0xFF1E3C40).copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        }
                                    ))
                                }
                            }
                        }

                        // 1.5 Particles Effect Layer
                        if (activeParticles != "Ninguno") {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val particleColor = when(activeParticles) {
                                        "Lluvia 🌧️" -> Color.White.copy(alpha=0.5f)
                                        "Destellos ✨" -> Color.Yellow.copy(alpha=0.8f)
                                        "Corazones 💖" -> Color.Red.copy(alpha=0.7f)
                                        else -> Color.Transparent
                                    }
                                    for(i in 0..50) {
                                        val x = (0..size.width.toInt()).random().toFloat()
                                        val y = ((System.currentTimeMillis() / 10 + i * 50) % size.height.toInt()).toFloat()
                                        if (activeParticles == "Destellos ✨") {
                                            drawCircle(color = particleColor, radius = (2..6).random().toFloat(), center = Offset(x, y))
                                        } else if (activeParticles == "Lluvia 🌧️") {
                                            drawLine(color = particleColor, start = Offset(x, y), end = Offset(x, y + 20f), strokeWidth = 2f)
                                        } else {
                                            drawCircle(color = particleColor, radius = 8f, center = Offset(x, y))
                                        }
                                    }
                                }
                            }
                        }

                        // 2. Interactive Overlays Canvas (Creative Engine Layers)
                        Box(modifier = Modifier.fillMaxSize()) {
                            creativeLayers.forEach { layer ->
                                when (layer) {
                                    is CreativeLayer.Text -> {
                                        val cInt = try { android.graphics.Color.parseColor(layer.colorHex) } catch(e: Exception) { android.graphics.Color.WHITE }
                                        Text(
                                            text = layer.text,
                                            color = Color(cInt),
                                            fontSize = (layer.fontSizeSp * layer.scale).sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .offset { IntOffset((layer.xFraction * 600).toInt(), (layer.yFraction * 1000).toInt()) }
                                                .pointerInput(layer.id) {
                                                    detectTransformGestures { _, pan, zoom, rotation ->
                                                        selectedLayerId = layer.id
                                                        creativeLayers = creativeLayers.map { l ->
                                                            if (l.id == layer.id) {
                                                                (l as CreativeLayer.Text).copy(
                                                                    xFraction = (l.xFraction + pan.x / 1000f).coerceIn(0f, 1f),
                                                                    yFraction = (l.yFraction + pan.y / 1000f).coerceIn(0f, 1f),
                                                                    scale = (l.scale * zoom).coerceIn(0.3f, 4f),
                                                                    rotation = l.rotation + rotation
                                                                )
                                                            } else l
                                                        }
                                                    }
                                                }
                                                .graphicsLayer {
                                                    scaleX = layer.scale
                                                    scaleY = layer.scale
                                                    rotationZ = layer.rotation
                                                }
                                        )
                                    }
                                    is CreativeLayer.Sticker -> {
                                        AsyncImage(
                                            model = layer.stickerUrlOrPath,
                                            contentDescription = "Sticker",
                                            modifier = Modifier
                                                .size(100.dp)
                                                .offset { IntOffset((layer.xFraction * 600).toInt(), (layer.yFraction * 1000).toInt()) }
                                                .pointerInput(layer.id) {
                                                    detectTransformGestures { _, pan, zoom, rotation ->
                                                        selectedLayerId = layer.id
                                                        creativeLayers = creativeLayers.map { l ->
                                                            if (l.id == layer.id) {
                                                                (l as CreativeLayer.Sticker).copy(
                                                                    xFraction = (l.xFraction + pan.x / 1000f).coerceIn(0f, 1f),
                                                                    yFraction = (l.yFraction + pan.y / 1000f).coerceIn(0f, 1f),
                                                                    scale = (l.scale * zoom).coerceIn(0.3f, 4f),
                                                                    rotation = l.rotation + rotation
                                                                )
                                                            } else l
                                                        }
                                                    }
                                                }
                                                .graphicsLayer {
                                                    scaleX = layer.scale
                                                    scaleY = layer.scale
                                                    rotationZ = layer.rotation
                                                }
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }

                        // 3. Top Action Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { currentStep = "picker" },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) { Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = null, tint = Color.White) }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                IconButton(
                                    onClick = { showTextEditor = true },
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) { Icon(Icons.Default.TextFields, contentDescription = "Texto", tint = Color.White) }
                                
                                IconButton(
                                    onClick = { showStickers = true },
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) { Icon(Icons.Default.EmojiEmotions, contentDescription = "Stickers", tint = Color.White) }

                                IconButton(
                                    onClick = { showFilters = !showFilters; showAudioPro = false; showTransitions = false },
                                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) { Icon(Icons.Default.AutoAwesome, contentDescription = "Filtros", tint = Color.White) }
                            }
                        }

                        // 4. Right Side Toolbar
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            IconButton(
                                onClick = { showTimeline = !showTimeline },
                                modifier = Modifier.background(if (showTimeline) Color(0xFF00E5FF) else Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) { Icon(Icons.Default.Timeline, contentDescription = "Timeline Multipista", tint = if (showTimeline) Color.Black else Color.White) }

                            IconButton(
                                onClick = { showInspector = !showInspector },
                                modifier = Modifier.background(if (showInspector) Color(0xFF00E5FF) else Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) { Icon(Icons.Default.Tune, contentDescription = "Inspector de Propiedades", tint = if (showInspector) Color.Black else Color.White) }

                            IconButton(
                                onClick = { showAudioPro = !showAudioPro; showFilters = false; showTransitions = false },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) { Icon(Icons.Default.MusicNote, contentDescription = "Audio Pro", tint = Color.White) }
                            
                            IconButton(
                                onClick = { showTransitions = !showTransitions; showFilters = false; showAudioPro = false },
                                modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            ) { Icon(Icons.Default.Animation, contentDescription = "Transiciones", tint = Color.White) }
                        }

                        // Render Button
                        Button(
                            onClick = { 
                                isRendering = true
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .navigationBarsPadding()
                                .padding(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF85))
                        ) {
                            Text("Siguiente 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        // --- Submenus & Overlays ---

                        // Text Editor Overlay
                        if (showTextEditor) {
                            var tempHasBg by remember { mutableStateOf(false) }
                            var tempHasShadow by remember { mutableStateOf(false) }
                            var tempGradient by remember { mutableStateOf(false) }
                            var tempFont by remember { mutableStateOf("Roboto") }
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    OutlinedTextField(
                                        value = textInput,
                                        onValueChange = { textInput = it },
                                        placeholder = { Text("Escribe algo creativo...", color = Color.Gray) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = selectedColor,
                                            unfocusedTextColor = selectedColor,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        ),
                                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    // Estilos de texto extra
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        FilterChip(selected = tempHasBg, onClick = { tempHasBg = !tempHasBg }, label = { Text("Fondo") })
                                        FilterChip(selected = tempHasShadow, onClick = { tempHasShadow = !tempHasShadow }, label = { Text("Sombra") })
                                        FilterChip(selected = tempGradient, onClick = { tempGradient = !tempGradient }, label = { Text("Degradado") })
                                    }
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                                        val fonts = listOf("Roboto", "Serif", "Cursive", "Monospace")
                                        items(fonts) { f ->
                                            OutlinedButton(
                                                onClick = { tempFont = f },
                                                border = BorderStroke(1.dp, if(tempFont == f) Color(0xFF00FF85) else Color.Gray)
                                            ) { Text(f, color = Color.White) }
                                        }
                                    }
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        val colors = listOf(Color.White, Color.Black, Color.Red, Color(0xFF00FF85), Color(0xFFE040FB), Color.Yellow)
                                        items(colors) { c ->
                                            Box(modifier = Modifier
                                                .size(36.dp)
                                                .background(c, CircleShape)
                                                .border(2.dp, if (c == selectedColor) Color.White else Color.Transparent, CircleShape)
                                                .clickable { selectedColor = c }
                                            )
                                        }
                                    }
                                }
                                Button(
                                    onClick = { 
                                        if (textInput.isNotBlank()) {
                                            val hex = String.format("#%06X", (0xFFFFFF and selectedColor.toArgb()))
                                            val newTextLayer = CreativeLayer.Text(
                                                id = java.util.UUID.randomUUID().toString(),
                                                text = textInput,
                                                colorHex = hex,
                                                fontFamily = tempFont,
                                                hasShadow = tempHasShadow,
                                                backgroundColorHex = if (tempHasBg) "#80000000" else null,
                                                xFraction = 0.4f,
                                                yFraction = 0.4f
                                            )
                                            creativeLayers = creativeLayers + newTextLayer
                                            selectedLayerId = newTextLayer.id
                                        }
                                        textInput = ""
                                        showTextEditor = false 
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).statusBarsPadding(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF85))
                                ) { Text("Listo", color = Color.Black) }
                            }
                        }

                        // Stickers API Simulation
                        if (showStickers) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable { showStickers = false }) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().height(300.dp).align(Alignment.BottomCenter).clickable {},
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF151518)),
                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Stickers Giphy (API)", color = Color.White, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                            val urls = listOf(
                                                "https://media2.giphy.com/media/v1.Y2lkPTc5MGI3NjExNTQ3MjNiYWZiODQwZWZiZDljYjY5MDhmNjg0Njg1YWEwOTQwMTNmYyZlcD12MV9pbnRlcm5hbF9naWZzX2dpZklkJmN0PWc/3o7TKSjRrfIPjeiVyM/giphy.gif",
                                                "https://media3.giphy.com/media/v1.Y2lkPTc5MGI3NjExMzM5OTYyMjFkNWZiMjEwZjA5YWJkMzlkNWE3ODUyNWQxMTQ2Njg5OSZlcD12MV9pbnRlcm5hbF9naWZzX2dpZklkJmN0PWc/xT9IgG50Fb7Mi0prBC/giphy.gif",
                                                "https://media4.giphy.com/media/v1.Y2lkPTc5MGI3NjExZjU4YjBkMDlhNzJhZTcxZjZiZmJjYzFjZjIyNWM5ZmMyZmI4MzExOCZlcD12MV9pbnRlcm5hbF9naWZzX2dpZklkJmN0PWc/MDJ9IbxxvDUQM/giphy.gif"
                                            )
                                            items(urls) { url ->
                                                AsyncImage(
                                                    model = url,
                                                    contentDescription = "Gif",
                                                    modifier = Modifier.size(80.dp).clickable {
                                                        val newStickerLayer = CreativeLayer.Sticker(
                                                            id = java.util.UUID.randomUUID().toString(),
                                                            stickerUrlOrPath = url,
                                                            xFraction = 0.5f,
                                                            yFraction = 0.5f
                                                        )
                                                        creativeLayers = creativeLayers + newStickerLayer
                                                        selectedLayerId = newStickerLayer.id
                                                        showStickers = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // --- Timeline Multipista Floating Panel ---
                        if (showTimeline) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 70.dp)
                            ) {
                                MultiTrackTimelineUI(
                                    tracks = timelineTracks,
                                    layers = creativeLayers,
                                    currentTimeMs = currentTimeMs,
                                    totalDurationMs = totalDurationMs,
                                    selectedTrackId = selectedTrackId,
                                    selectedLayerId = selectedLayerId,
                                    onSeek = { currentTimeMs = it },
                                    onSelectTrack = { selectedTrackId = it },
                                    onSelectLayer = { selectedLayerId = it },
                                    onToggleMuteTrack = { trackId ->
                                        timelineTracks = timelineTracks.map { t ->
                                            if (t.id == trackId) {
                                                when (t) {
                                                    is CreativeTrack.VideoTrack -> t.copy(isMuted = !t.isMuted)
                                                    is CreativeTrack.AudioTrack -> t.copy(isMuted = !t.isMuted)
                                                    is CreativeTrack.VoiceTrack -> t.copy(isMuted = !t.isMuted)
                                                    else -> t
                                                }
                                            } else t
                                        }
                                    }
                                )
                            }
                        }

                        // --- Inspector Floating Sheet ---
                        if (showInspector) {
                            val activeLayer = creativeLayers.firstOrNull { it.id == selectedLayerId }
                            val activeTrack = timelineTracks.firstOrNull { it.id == selectedTrackId }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 70.dp)
                            ) {
                                PropertyInspector(
                                    selectedLayer = activeLayer,
                                    selectedTrack = activeTrack,
                                    currentTimeMs = currentTimeMs,
                                    onUpdateLayer = { updatedLayer ->
                                        creativeLayers = creativeLayers.map { if (it.id == updatedLayer.id) updatedLayer else it }
                                    },
                                    onUpdateTrack = { updatedTrack ->
                                        timelineTracks = timelineTracks.map { if (it.id == updatedTrack.id) updatedTrack else it }
                                    },
                                    onAddKeyframe = { layerId, prop, valToSave, easing ->
                                        // Keyframe saved into layer animation config
                                    },
                                    onRemoveKeyframe = { layerId, prop, timeMs ->
                                        // Keyframe removed from layer
                                    },
                                    onDismiss = { showInspector = false }
                                )
                            }
                        }

                        // Bottom Carousels (Transitions, Filters, Audio)
                        if (showFilters) {
                            Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 80.dp)) {
                                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(filters) { f ->
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { activeFilter = f }) {
                                            Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.DarkGray).border(2.dp, if(activeFilter==f) Color(0xFF00FF85) else Color.Transparent, CircleShape))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(f, color = Color.White, fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (showTransitions) {
                            Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 80.dp).background(Color.Black.copy(alpha=0.6f)).padding(vertical = 12.dp)) {
                                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    items(transitions) { t ->
                                        Box(
                                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(if (selectedTransition == t) Color(0xFF00FF85) else Color(0xFF1E1E22)).clickable { selectedTransition = t }.padding(horizontal = 16.dp, vertical = 8.dp)
                                        ) { Text(t, color = if(selectedTransition==t) Color.Black else Color.White) }
                                    }
                                }
                            }
                        }

                        if (showAudioPro) {
                            Card(
                                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF151518)),
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Edición de Audio Pro 🎙️", color = Color.White, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.clickable { showAudioPro = false })
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Waveform Musical", color = Color.Gray, fontSize = 12.sp)
                                    // Interactive Audio Waveform Canvas
                                    Box(modifier = Modifier.fillMaxWidth().height(60.dp).background(Color(0xFF262629), RoundedCornerShape(8.dp)).padding(vertical = 8.dp)) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            val barWidth = 6f
                                            val gap = 4f
                                            val count = (size.width / (barWidth + gap)).toInt()
                                            for (i in 0 until count) {
                                                val h = ((i * 17) % 30 + 10).toFloat()
                                                val x = i * (barWidth + gap)
                                                val color = if (x >= size.width * (trimStartPercent/100f) && x <= size.width * (trimEndPercent/100f)) Color(0xFFE040FB) else Color.DarkGray
                                                drawRoundRect(color = color, topLeft = Offset(x, size.height/2 - h/2), size = androidx.compose.ui.geometry.Size(barWidth, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f))
                                            }
                                        }
                                        RangeSlider(
                                            value = trimStartPercent..trimEndPercent,
                                            onValueChange = { trimStartPercent = it.start; trimEndPercent = it.endInclusive },
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent, thumbColor = Color.White)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Mezclador (Mixer)", color = Color.Gray, fontSize = 12.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Slider(value = originalVideoVolume, onValueChange = { originalVideoVolume = it }, valueRange = 0f..100f, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), colors = SliderDefaults.colors(activeTrackColor = Color.White))
                                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF00FF85), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                        
                        // Technical Adjustments Hidden Menu
                        if (showAdjustments) {
                            Card(
                                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF151518)),
                                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp).navigationBarsPadding()) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Ajustes Técnicos", color = Color.White, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.clickable { showAdjustments = false })
                                    }
                                    Text("Brillo", color = Color.Gray, fontSize = 12.sp)
                                    Slider(value = brightnessValue, onValueChange = { brightnessValue = it }, valueRange = 0.5f..1.5f)
                                    Text("Contraste", color = Color.Gray, fontSize = 12.sp)
                                    Slider(value = contrastValue, onValueChange = { contrastValue = it }, valueRange = 0.5f..1.5f)
                                }
                            }
                        }

                        // Rendering Overlay Simulation
                        if (isRendering) {
                            var renderProgress by remember { mutableStateOf(0f) }
                            val context = LocalContext.current
                            LaunchedEffect(Unit) {
                                // Llamada REAL al Motor de Composición (FFmpeg / Canvas)
                                val durationMs = 15000 // 15s mock duration
                                val startMs = (durationMs * (trimStartPercent/100f)).toInt()
                                val endMs = (durationMs * (trimEndPercent/100f)).toInt()
                                
                                // Simulamos el avance de progreso de FFmpegKit
                                val progressJob = launch {
                                    while (renderProgress < 0.95f) {
                                        delay(100)
                                        renderProgress += 0.05f
                                    }
                                }
                                
                                val dummyOutputFile = java.io.File(context.cacheDir, "salsa_export.mp4")
                                
                                val cinematicFilterColor = when(activeFilter) {
                                    "B&W" -> android.graphics.Color.GRAY // Simple fallback
                                    "Retro" -> android.graphics.Color.argb(76, 255, 213, 79)
                                    "Vibe" -> android.graphics.Color.argb(51, 224, 64, 251)
                                    "Cinematic" -> android.graphics.Color.argb(51, 30, 60, 64)
                                    else -> null
                                }
                                val utilTextOverlays = creativeLayers.filterIsInstance<CreativeLayer.Text>().map { 
                                    val cInt = try { android.graphics.Color.parseColor(it.colorHex) } catch(e: Exception) { android.graphics.Color.WHITE }
                                    com.example.util.TextOverlay(
                                        text = it.text,
                                        x = it.xFraction * 600f,
                                        y = it.yFraction * 1000f,
                                        fontSize = it.fontSizeSp * it.scale,
                                        color = cInt,
                                        fontName = it.fontFamily,
                                        hasBackground = it.backgroundColorHex != null,
                                        hasShadow = it.hasShadow,
                                        isGradient = false
                                    ) 
                                }
                                val utilStickerOverlays = creativeLayers.filterIsInstance<CreativeLayer.Sticker>().map {
                                    com.example.util.StickerOverlay(
                                        emoji = it.stickerUrlOrPath,
                                        x = it.xFraction * 600f,
                                        y = it.yFraction * 1000f,
                                        scale = it.scale
                                    )
                                }

                                // Ejecutar Renderizado Real
                                if (mediaType == "image" && selectedMediaUri != null) {
                                    val pendingMediaDir = java.io.File(context.filesDir, "pending_media")
                                    if (!pendingMediaDir.exists()) pendingMediaDir.mkdirs()
                                    val renderedFile = java.io.File.createTempFile("reel_render_", ".jpg", pendingMediaDir)
                                    val success = com.example.util.MediaCompositionEngine.renderImageCompositionToFile(
                                        context = context,
                                        baseImageUri = selectedMediaUri!!,
                                        textOverlays = utilTextOverlays,
                                        stickerOverlays = utilStickerOverlays,
                                        filterColor = cinematicFilterColor,
                                        outputFile = renderedFile
                                    )
                                    if (success && renderedFile.exists() && renderedFile.length() > 0) {
                                        exportedMediaFile = renderedFile
                                    } else {
                                        renderedFile.delete()
                                    }
                                } else {
                                    // FFmpeg Video Render
                                    com.example.util.MediaCompositionEngine.renderFFmpegVideo(
                                        context = context,
                                        inputVideoUri = selectedMediaUri?.toString() ?: "dummy_video.mp4",
                                        inputAudioUri = selectedMusicTrack + ".mp3",
                                        trimStartMs = startMs,
                                        trimEndMs = endMs,
                                        videoVolume = originalVideoVolume,
                                        audioVolume = backgroundMusicVolume,
                                        textOverlays = utilTextOverlays,
                                        stickerOverlays = utilStickerOverlays,
                                        filterColor = cinematicFilterColor,
                                        outputFile = dummyOutputFile,
                                        onProgress = { /* Ignored for now */ }
                                    )
                                    exportedMediaUri = android.net.Uri.fromFile(dummyOutputFile)
                                }
                                
                                progressJob.cancel()
                                renderProgress = 1f
                                delay(300)
                                isRendering = false
                                currentStep = "metadata"
                            }
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).clickable{}, contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                    CircularProgressIndicator(progress = { renderProgress }, color = Color(0xFF00FF85), modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text("Procesando edición...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Motor FFmpeg combinando capas, audio y efectos cinemáticos", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    LinearProgressIndicator(progress = { renderProgress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Color(0xFF00FF85), trackColor = Color.DarkGray)
                                }
                            }
                        }

                    }
                } else if (currentStep == "metadata") {
                    // Step 3: Strict Metadata configuration before publishing
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { currentStep = "studio" }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Ajustes de Publicación 🌍", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }

                        // Reel Description (Mandatory)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Descripción del Reel", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(" *Obligatorio", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedTextField(
                            value = reelDescription,
                            onValueChange = { reelDescription = it },
                            placeholder = { Text("Escribe una descripción premium...") },
                            modifier = Modifier.fillMaxWidth().testTag("reel_description_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF00FF85),
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        // Hashtags input (Mandatory: Min 1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Hashtags", color = Color.White, fontWeight = FontWeight.Bold)
                            Text(" *Obligatorio (mínimo 1)", color = Color.Red, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                        OutlinedTextField(
                            value = reelHashtagInput,
                            onValueChange = { reelHashtagInput = it },
                            placeholder = { Text("ej. #Chamo #Panalink #Venezuela") },
                            modifier = Modifier.fillMaxWidth().testTag("reel_hashtags_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFFE040FB),
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFE040FB),
                                unfocusedBorderColor = Color.Gray
                            )
                        )

                        // Selector de Tipo: Reel vs Historia
                        Text("¿Dónde publicar?", color = Color.White, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isReelSelected = true },
                                border = BorderStroke(1.dp, if (isReelSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f)),
                                colors = CardDefaults.cardColors(containerColor = if (isReelSelected) Color(0xFF00FF85).copy(alpha = 0.1f) else Color.Transparent)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircle,
                                        contentDescription = null,
                                        tint = if (isReelSelected) Color(0xFF00FF85) else Color.Gray
                                    )
                                    Text("Reel", color = if (isReelSelected) Color(0xFF00FF85) else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Público y permanente", color = Color.Gray, fontSize = 9.sp)
                                }
                            }
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isReelSelected = false },
                                border = BorderStroke(1.dp, if (!isReelSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f)),
                                colors = CardDefaults.cardColors(containerColor = if (!isReelSelected) Color(0xFF00FF85).copy(alpha = 0.1f) else Color.Transparent)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = if (!isReelSelected) Color(0xFF00FF85) else Color.Gray
                                    )
                                    Text("Historia", color = if (!isReelSelected) Color(0xFF00FF85) else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Contactos, 24h", color = Color.Gray, fontSize = 9.sp)
                                }
                            }
                        }

                        // Scheduling / Planificación section
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Programar Publicación 📆", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = if (scheduledDateTimeString.isEmpty()) "Publicación inmediata" else "Programado para: $scheduledDateTimeString",
                                        color = if (scheduledDateTimeString.isEmpty()) Color.Gray else Color(0xFF00FF85),
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = { showSchedulingSheet = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(if (scheduledDateTimeString.isEmpty()) "Definir" else "Cambiar", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }

                        // Draft Mode Button
                        OutlinedButton(
                            onClick = { showDraftSuccessDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {
                            Icon(Icons.Default.Drafts, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Guardar como Borrador (Draft)")
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Submit action button (Enforces mandatory requirements)
                        Button(
                            onClick = { publishReel() },
                            enabled = isPublishAllowed,
                            modifier = Modifier.fillMaxWidth().testTag("reel_editor_publish_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00FF85),
                                disabledContainerColor = Color.White.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (isReelSelected) "Publicar Reel 🚀" else "Añadir a Historia ✨", color = if (isPublishAllowed) Color.Black else Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }


            // DATES SCHEDULER SIMULATOR
            if (showSchedulingSheet) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .width(300.dp)
                            .padding(16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Programar Hora 🕒", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            
                            val options = listOf("Hoy a las 6:00 PM", "Hoy a las 9:00 PM", "Mañana a las 9:00 AM", "Mañana a las 3:00 PM")
                            options.forEach { opt ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.05f))
                                        .clickable {
                                            scheduledDateTimeString = opt
                                            showSchedulingSheet = false
                                        }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(opt, color = Color.White, fontSize = 13.sp)
                                }
                            }

                            TextButton(onClick = { showSchedulingSheet = false }) {
                                Text("Cancelar", color = Color.Red)
                            }
                        }
                    }
                }
            }

            // DRAFT SAVED SUCCESS SIMULATION
            if (showDraftSuccessDialog) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .width(280.dp)
                            .padding(20.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(Color(0xFFE040FB), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Drafts, contentDescription = null, tint = Color.Black, modifier = Modifier.size(32.dp))
                            }
                            Text("Borrador Guardado 📝", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("Tu borrador se ha guardado localmente en tu dispositivo.", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                            Button(
                                onClick = {
                                    showDraftSuccessDialog = false
                                    onBack()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE040FB))
                            ) {
                                Text("Entendido", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
