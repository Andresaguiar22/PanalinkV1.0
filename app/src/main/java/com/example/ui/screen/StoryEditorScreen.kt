package com.example.ui.screen

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.creative.canvas.CanvasEditorEngine
import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import com.example.creative.core.CreativeType
import com.example.creative.persistence.AutoSaveManager
import com.example.creative.template.CreativeTemplateManager
import com.example.creative.video.VideoFilterProcessor
import com.example.data.database.PanalinkDatabase
import com.example.data.database.PendingUploadEntity
import com.example.ui.components.CameraCaptureView
import com.example.ui.components.SimpleVideoPreviewPlayer
import com.example.ui.viewmodel.CreateStateUiState
import com.example.ui.viewmodel.StatesViewModel
import com.example.media.dedup.MediaDeduplicationEngine
import com.example.media.storage.MediaStorageManager
import com.example.worker.SocialMediaUploadWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class StoryStudioTab {
    TEXT,
    STICKERS,
    MUSIC,
    DRAWING,
    EFFECTS,
    LOCATION,
    LINK,
    INTERACTIVE,
    TIMER,
    MENTION
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryEditorScreen(
    viewModel: StatesViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val createStateState by viewModel.createStateFlow.collectAsState()

    // 1. Creative Project State
    var currentProject by remember {
        mutableStateOf(
            CreativeProject(
                id = "story_${UUID.randomUUID()}",
                sourceMedia = "",
                type = CreativeType.STORY,
                layers = emptyList()
            )
        )
    }

    // History undo/redo stacks
    val undoStack = remember { mutableStateListOf<CreativeProject>() }
    val redoStack = remember { mutableStateListOf<CreativeProject>() }

    fun updateProjectWithHistory(newProject: CreativeProject) {
        undoStack.add(currentProject)
        redoStack.clear()
        currentProject = newProject
    }

    // 2. Draft AutoSave & Restoration
    var showUnfinishedDraftDialog by remember { mutableStateOf(false) }
    var unfinishedDraftId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val draftInfo = AutoSaveManager.checkForUnfinishedDraft(context)
        if (draftInfo != null) {
            unfinishedDraftId = draftInfo.projectId
            showUnfinishedDraftDialog = true
        }
    }

    // Periodic AutoSave
    LaunchedEffect(currentProject) {
        if (currentProject.sourceMedia.isNotEmpty() || currentProject.layers.isNotEmpty()) {
            delay(3000)
            AutoSaveManager.saveDraft(context, currentProject)
        }
    }

    // Active tool bottom sheet
    var activeToolTab by remember { mutableStateOf<StoryStudioTab?>(null) }

    // Media Picker / Camera Mode
    var isCameraActive by remember { mutableStateOf(false) }
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var isVideoMedia by remember { mutableStateOf(false) }
    var activeFilterName by remember { mutableStateOf("none") }
    var backgroundColorHex by remember { mutableStateOf("#16161E") }

    // Drawing mode state
    var isDrawingActive by remember { mutableStateOf(false) }
    var strokeColorHex by remember { mutableStateOf("#00E5FF") }
    var strokeWidthDp by remember { mutableStateOf(6f) }
    var drawingToolType by remember { mutableStateOf("pencil") } // pencil, marker, brush, neon, eraser

    // Selected layer & Multi-select
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    var selectedLayerIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Professional Studio Modals & Sheets
    var showLayersSheet by remember { mutableStateOf(false) }
    var showExportSettingsModal by remember { mutableStateOf(false) }
    var showTimelineDrawer by remember { mutableStateOf(false) }
    var showAssetPacksModal by remember { mutableStateOf(false) }
    var currentVideoTimeMs by remember { mutableLongStateOf(0L) }

    // Export Settings
    var exportResolution by remember { mutableStateOf("1080p") }
    var exportFps by remember { mutableIntStateOf(30) }
    var exportQuality by remember { mutableStateOf("Alta") }
    var exportHdrEnabled by remember { mutableStateOf(false) }

    // Bottom action modals
    var showTemplatesModal by remember { mutableStateOf(false) }
    var showDraftsModal by remember { mutableStateOf(false) }
    var isPublishing by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedMediaUri = uri
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            isVideoMedia = mime.startsWith("video/")
            isCameraActive = false
            updateProjectWithHistory(
                currentProject.copy(sourceMedia = uri.toString())
            )
        }
    }

    // Modal para borrador inconcluso
    if (showUnfinishedDraftDialog) {
        AlertDialog(
            onDismissRequest = { showUnfinishedDraftDialog = false },
            title = { Text("Borrador Encontrado", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("Se encontró un borrador previo de tu Historia. ¿Deseas continuar editándolo?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        showUnfinishedDraftDialog = false
                        // Keep active draft
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                ) {
                    Text("Continuar Borrador", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showUnfinishedDraftDialog = false
                        unfinishedDraftId?.let { id ->
                            coroutineScope.launch {
                                AutoSaveManager.clearDraft(context, id)
                            }
                        }
                    }
                ) {
                    Text("Descartar", color = Color(0xFFFF5252))
                }
            },
            containerColor = Color(0xFF1F1F2C)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D12))
    ) {
        // ==========================================
        // ZONA 1: BARRA SUPERIOR (Top Bar)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF16161E))
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_close_story_studio")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = {
                        if (undoStack.isNotEmpty()) {
                            val previous = undoStack.removeAt(undoStack.lastIndex)
                            redoStack.add(currentProject)
                            currentProject = previous
                        }
                    },
                    enabled = undoStack.isNotEmpty(),
                    modifier = Modifier.testTag("btn_story_undo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Deshacer",
                        tint = if (undoStack.isNotEmpty()) Color.White else Color.Gray
                    )
                }

                IconButton(
                    onClick = {
                        if (redoStack.isNotEmpty()) {
                            val next = redoStack.removeAt(redoStack.lastIndex)
                            undoStack.add(currentProject)
                            currentProject = next
                        }
                    },
                    enabled = redoStack.isNotEmpty(),
                    modifier = Modifier.testTag("btn_story_redo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Rehacer",
                        tint = if (redoStack.isNotEmpty()) Color.White else Color.Gray
                    )
                }

                // Capas Button (Photoshop/CapCut style inspector)
                IconButton(
                    onClick = { showLayersSheet = !showLayersSheet },
                    modifier = Modifier.testTag("btn_story_layers")
                ) {
                    BadgedBox(
                        badge = {
                            if (currentProject.layers.isNotEmpty()) {
                                Badge(containerColor = Color(0xFF00E5FF), contentColor = Color.Black) {
                                    Text("${currentProject.layers.size}")
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = "Capas",
                            tint = if (showLayersSheet) Color(0xFF00E5FF) else Color.White
                        )
                    }
                }

                // Timeline Button
                IconButton(
                    onClick = { showTimelineDrawer = !showTimelineDrawer },
                    modifier = Modifier.testTag("btn_story_timeline")
                ) {
                    Icon(
                        imageVector = Icons.Default.ViewTimeline,
                        contentDescription = "Timeline",
                        tint = if (showTimelineDrawer) Color(0xFF00E5FF) else Color.White
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Export Settings gear
                IconButton(
                    onClick = { showExportSettingsModal = true },
                    modifier = Modifier.testTag("btn_export_settings")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Ajustes de Exportación",
                        tint = Color.White
                    )
                }

                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            AutoSaveManager.saveDraft(context, currentProject)
                            Toast.makeText(context, "Borrador guardado localmente", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("btn_save_story_draft")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Guardar", color = Color(0xFF00E5FF), fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { showExportSettingsModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_publish_story")
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Publicar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // ==========================================
        // ZONA 2: CENTRO (Vista previa en tiempo real) & ZONA 3: BARRA LATERAL DERECHA
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // ZONA CENTRO: Vista previa en tiempo real Canvas
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(backgroundColorHex))
                        } catch (e: Exception) {
                            Color(0xFF16161E)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isCameraActive) {
                    CameraCaptureView(
                        mode = "any",
                        onMediaCaptured = { uri, type ->
                            selectedMediaUri = uri
                            isVideoMedia = (type == "video")
                            isCameraActive = false
                            updateProjectWithHistory(currentProject.copy(sourceMedia = uri.toString()))
                        },
                        onDismiss = { isCameraActive = false }
                    )
                } else {
                    CanvasEditorEngine(
                        project = currentProject,
                        selectedLayerId = selectedLayerId,
                        selectedLayerIds = selectedLayerIds,
                        isDrawingMode = isDrawingActive,
                        strokeColorHex = strokeColorHex,
                        strokeWidthDp = strokeWidthDp,
                        activeFilterName = activeFilterName,
                        currentVideoTimeMs = currentVideoTimeMs,
                        onProjectUpdated = { updatedProj -> updateProjectWithHistory(updatedProj) },
                        onLayerSelected = { layerId -> selectedLayerId = layerId },
                        onLayerDoubleTap = { layer ->
                            selectedLayerId = layer.id
                            if (layer is CreativeLayer.Text) {
                                activeToolTab = StoryStudioTab.TEXT
                            } else {
                                showLayersSheet = true
                            }
                        },
                        onLayerLongPress = { layer ->
                            selectedLayerId = layer.id
                            showLayersSheet = true
                        },
                        modifier = Modifier.fillMaxSize(),
                        backgroundContent = {
                            if (selectedMediaUri != null) {
                                if (isVideoMedia) {
                                    SimpleVideoPreviewPlayer(
                                        videoUri = selectedMediaUri!!,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    AsyncImage(
                                        model = selectedMediaUri,
                                        contentDescription = "Fondo Historia",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                    )
                }
            }

            // ==========================================
            // ZONA 3: BARRA LATERAL DERECHA (Herramientas rápidas)
            // ==========================================
            Column(
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF16161E))
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickToolButton("Aa", "Texto", activeToolTab == StoryStudioTab.TEXT) {
                    activeToolTab = if (activeToolTab == StoryStudioTab.TEXT) null else StoryStudioTab.TEXT
                    isDrawingActive = false
                }
                QuickToolButton("😊", "Stickers", activeToolTab == StoryStudioTab.STICKERS) {
                    activeToolTab = if (activeToolTab == StoryStudioTab.STICKERS) null else StoryStudioTab.STICKERS
                    isDrawingActive = false
                }
                QuickToolButton("🎵", "Música", activeToolTab == StoryStudioTab.MUSIC) {
                    activeToolTab = if (activeToolTab == StoryStudioTab.MUSIC) null else StoryStudioTab.MUSIC
                    isDrawingActive = false
                }
                QuickToolButton("✏️", "Dibujo", isDrawingActive) {
                    isDrawingActive = !isDrawingActive
                    activeToolTab = if (isDrawingActive) StoryStudioTab.DRAWING else null
                }
                QuickToolButton("✨", "Efectos", activeToolTab == StoryStudioTab.EFFECTS) {
                    activeToolTab = if (activeToolTab == StoryStudioTab.EFFECTS) null else StoryStudioTab.EFFECTS
                    isDrawingActive = false
                }
                QuickToolButton("📍", "Ubicación", activeToolTab == StoryStudioTab.LOCATION) {
                    activeToolTab = if (activeToolTab == StoryStudioTab.LOCATION) null else StoryStudioTab.LOCATION
                    isDrawingActive = false
                }
                QuickToolButton("🔗", "Enlace", activeToolTab == StoryStudioTab.LINK) {
                    activeToolTab = if (activeToolTab == StoryStudioTab.LINK) null else StoryStudioTab.LINK
                    isDrawingActive = false
                }
                QuickToolButton("📊", "Encuestas", activeToolTab == StoryStudioTab.INTERACTIVE) {
                    activeToolTab = if (activeToolTab == StoryStudioTab.INTERACTIVE) null else StoryStudioTab.INTERACTIVE
                    isDrawingActive = false
                }
                QuickToolButton("⏱", "Cuenta", activeToolTab == StoryStudioTab.TIMER) {
                    activeToolTab = if (activeToolTab == StoryStudioTab.TIMER) null else StoryStudioTab.TIMER
                    isDrawingActive = false
                }
                QuickToolButton("@", "Mención", activeToolTab == StoryStudioTab.MENTION) {
                    activeToolTab = if (activeToolTab == StoryStudioTab.MENTION) null else StoryStudioTab.MENTION
                    isDrawingActive = false
                }
            }
        }

        // ==========================================
        // ACTIVE TOOL BOTTOM SHEET / PANEL
        // ==========================================
        AnimatedVisibility(
            visible = activeToolTab != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color(0xFF1F1F2C), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(16.dp)
            ) {
                when (activeToolTab) {
                    StoryStudioTab.TEXT -> TextProToolPanel(
                        onAddTextLayer = { textLayer ->
                            updateProjectWithHistory(
                                currentProject.copy(layers = currentProject.layers + textLayer)
                            )
                            activeToolTab = null
                        }
                    )
                    StoryStudioTab.STICKERS -> StickerStudioPanel(
                        onSelectSticker = { stickerUrl ->
                            val stickerLayer = CreativeLayer.Sticker(
                                id = "sticker_${System.currentTimeMillis()}",
                                stickerUrlOrPath = stickerUrl
                            )
                            updateProjectWithHistory(
                                currentProject.copy(layers = currentProject.layers + stickerLayer)
                            )
                            activeToolTab = null
                        }
                    )
                    StoryStudioTab.MUSIC -> MusicStudioPanel(
                        onSelectAudio = { audioPath, startMs, durMs, vol ->
                            val audioLayer = CreativeLayer.Audio(
                                id = "audio_${System.currentTimeMillis()}",
                                audioUrlOrPath = audioPath,
                                startOffsetMs = startMs,
                                durationMs = durMs,
                                volume = vol
                            )
                            updateProjectWithHistory(
                                currentProject.copy(layers = currentProject.layers + audioLayer)
                            )
                            activeToolTab = null
                        }
                    )
                    StoryStudioTab.DRAWING -> DrawingStudioPanel(
                        selectedColor = strokeColorHex,
                        selectedWidth = strokeWidthDp,
                        selectedTool = drawingToolType,
                        onColorChange = { strokeColorHex = it },
                        onWidthChange = { strokeWidthDp = it },
                        onToolChange = { drawingToolType = it }
                    )
                    StoryStudioTab.EFFECTS -> EffectsStudioPanel(
                        activeFilter = activeFilterName,
                        onSelectFilter = { filterName ->
                            activeFilterName = filterName
                            val filterLayer = CreativeLayer.Filter(
                                id = "filter_${System.currentTimeMillis()}",
                                filterName = filterName
                            )
                            updateProjectWithHistory(
                                currentProject.copy(
                                    layers = currentProject.layers.filterNot { it is CreativeLayer.Filter } + filterLayer
                                )
                            )
                        }
                    )
                    StoryStudioTab.LOCATION, StoryStudioTab.LINK, StoryStudioTab.INTERACTIVE, StoryStudioTab.TIMER, StoryStudioTab.MENTION -> InteractiveStudioPanel(
                        type = activeToolTab!!.name,
                        onAddInteractiveLayer = { layer ->
                            updateProjectWithHistory(
                                currentProject.copy(layers = currentProject.layers + layer)
                            )
                            activeToolTab = null
                        }
                    )
                    else -> Unit
                }
            }
        }

        // ==========================================
        // ZONA 4: BARRA INFERIOR (Galería, Cámara, Color, Plantillas, Borradores)
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(Color(0xFF16161E))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Galería
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = "Galería", tint = Color.White)
                Text("Galería", color = Color.Gray, fontSize = 10.sp)
            }

            // Cámara
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { isCameraActive = !isCameraActive }
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Cámara",
                    tint = if (isCameraActive) Color(0xFF00E5FF) else Color.White
                )
                Text("Cámara", color = if (isCameraActive) Color(0xFF00E5FF) else Color.Gray, fontSize = 10.sp)
            }

            // Color
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        val palette = listOf("#16161E", "#000000", "#FF1744", "#00E5FF", "#D500F9", "#FFEA00", "#00FF85")
                        val nextIdx = (palette.indexOf(backgroundColorHex) + 1) % palette.size
                        backgroundColorHex = palette[nextIdx]
                    }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(backgroundColorHex))
                            } catch (e: Exception) {
                                Color.White
                            },
                            CircleShape
                        )
                        .border(1.dp, Color.White, CircleShape)
                )
                Text("Color", color = Color.Gray, fontSize = 10.sp)
            }

            // Plantillas
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showTemplatesModal = true }
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "Plantillas", tint = Color.White)
                Text("Plantillas", color = Color.Gray, fontSize = 10.sp)
            }

            // Borradores
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showDraftsModal = true }
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Folder, contentDescription = "Borradores", tint = Color.White)
                Text("Borradores", color = Color.Gray, fontSize = 10.sp)
            }
        }

    // Modal de Plantillas
    if (showTemplatesModal) {
        AlertDialog(
            onDismissRequest = { showTemplatesModal = false },
            title = { Text("Plantillas de Historias", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                val templates = CreativeTemplateManager.getTemplates()
                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(templates) { tmpl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val applied = CreativeTemplateManager.applyTemplateToProject(tmpl, currentProject)
                                    updateProjectWithHistory(applied)
                                    showTemplatesModal = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(tmpl.name, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(tmpl.description, color = Color.Gray, fontSize = 12.sp)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF00E5FF))
                        }
                        HorizontalDivider(color = Color(0xFF2C2C3E))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplatesModal = false }) {
                    Text("Cerrar", color = Color(0xFF00E5FF))
                }
            },
            containerColor = Color(0xFF1F1F2C)
        )
    }

    // Modal de Borradores
    if (showDraftsModal) {
        AlertDialog(
            onDismissRequest = { showDraftsModal = false },
            title = { Text("Borradores Guardados", fontWeight = FontWeight.Bold, color = Color.White) },
            text = {
                Column {
                    Text("PanaLink mantiene autosave continuo de tus historias en progreso.", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                AutoSaveManager.saveDraft(context, currentProject)
                                Toast.makeText(context, "Borrador activo actualizado", Toast.LENGTH_SHORT).show()
                                showDraftsModal = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                    ) {
                        Text("Guardar Copia de Borrador", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDraftsModal = false }) {
                    Text("Cerrar", color = Color(0xFF00E5FF))
                }
            },
            containerColor = Color(0xFF1F1F2C)
        )
    }

    // 1. INSPECTOR DINÁMICO DE CAPAS (Photoshop / CapCut Style)
    if (showLayersSheet) {
        LayersInspectorModalSheet(
            layers = currentProject.layers,
            selectedLayerId = selectedLayerId,
            selectedLayerIds = selectedLayerIds,
            onSelectLayer = { id -> selectedLayerId = id },
            onToggleMultiSelect = { id ->
                selectedLayerIds = if (selectedLayerIds.contains(id)) {
                    selectedLayerIds - id
                } else {
                    selectedLayerIds + id
                }
            },
            onToggleVisibility = { id ->
                val updatedLayers = currentProject.layers.map { layer ->
                    if (layer.id == id) {
                        when (layer) {
                            is CreativeLayer.Text -> layer.copy(isVisible = !layer.isVisible)
                            is CreativeLayer.Sticker -> layer.copy(isVisible = !layer.isVisible)
                            is CreativeLayer.Drawing -> layer.copy(isVisible = !layer.isVisible)
                            is CreativeLayer.Filter -> layer.copy(isVisible = !layer.isVisible)
                            is CreativeLayer.Audio -> layer.copy(isVisible = !layer.isVisible)
                            is CreativeLayer.Interactive -> layer.copy(isVisible = !layer.isVisible)
                            is CreativeLayer.Group -> layer.copy(isVisible = !layer.isVisible)
                            is CreativeLayer.Image -> layer.copy(isVisible = !layer.isVisible)
                            is CreativeLayer.Video -> layer.copy(isVisible = !layer.isVisible)
                        }
                    } else layer
                }
                updateProjectWithHistory(currentProject.copy(layers = updatedLayers))
            },
            onToggleLock = { id ->
                val updatedLayers = currentProject.layers.map { layer ->
                    if (layer.id == id) {
                        when (layer) {
                            is CreativeLayer.Text -> layer.copy(isLocked = !layer.isLocked)
                            is CreativeLayer.Sticker -> layer.copy(isLocked = !layer.isLocked)
                            is CreativeLayer.Drawing -> layer.copy(isLocked = !layer.isLocked)
                            is CreativeLayer.Filter -> layer.copy(isLocked = !layer.isLocked)
                            is CreativeLayer.Audio -> layer.copy(isLocked = !layer.isLocked)
                            is CreativeLayer.Interactive -> layer.copy(isLocked = !layer.isLocked)
                            is CreativeLayer.Group -> layer.copy(isLocked = !layer.isLocked)
                            is CreativeLayer.Image -> layer.copy(isLocked = !layer.isLocked)
                            is CreativeLayer.Video -> layer.copy(isLocked = !layer.isLocked)
                        }
                    } else layer
                }
                updateProjectWithHistory(currentProject.copy(layers = updatedLayers))
            },
            onReorderLayer = { fromIndex, toIndex ->
                if (fromIndex in currentProject.layers.indices && toIndex in currentProject.layers.indices) {
                    val mutableLayers = currentProject.layers.toMutableList()
                    val moved = mutableLayers.removeAt(fromIndex)
                    mutableLayers.add(toIndex, moved)
                    updateProjectWithHistory(currentProject.copy(layers = mutableLayers))
                }
            },
            onDuplicateLayer = { id ->
                val layerToDup = currentProject.layers.find { it.id == id }
                if (layerToDup != null) {
                    val newId = "dup_${System.currentTimeMillis()}"
                    val duplicated: CreativeLayer = when (layerToDup) {
                        is CreativeLayer.Text -> layerToDup.copy(id = newId, xFraction = layerToDup.xFraction + 0.05f)
                        is CreativeLayer.Sticker -> layerToDup.copy(id = newId, xFraction = layerToDup.xFraction + 0.05f)
                        is CreativeLayer.Drawing -> layerToDup.copy(id = newId)
                        is CreativeLayer.Filter -> layerToDup.copy(id = newId)
                        is CreativeLayer.Audio -> layerToDup.copy(id = newId)
                        is CreativeLayer.Interactive -> layerToDup.copy(id = newId, xFraction = layerToDup.xFraction + 0.05f)
                        is CreativeLayer.Group -> layerToDup.copy(id = newId)
                        is CreativeLayer.Image -> layerToDup.copy(id = newId, xFraction = layerToDup.xFraction + 0.05f)
                        is CreativeLayer.Video -> layerToDup.copy(id = newId, xFraction = layerToDup.xFraction + 0.05f)
                    }
                    updateProjectWithHistory(currentProject.copy(layers = currentProject.layers + duplicated))
                }
            },
            onDeleteLayer = { id ->
                val updatedLayers = currentProject.layers.filterNot { it.id == id }
                updateProjectWithHistory(currentProject.copy(layers = updatedLayers))
                if (selectedLayerId == id) selectedLayerId = null
            },
            onGroupSelectedLayers = {
                if (selectedLayerIds.size >= 2) {
                    val groupLayer = CreativeLayer.Group(
                        id = "group_${System.currentTimeMillis()}",
                        memberLayerIds = selectedLayerIds.toList(),
                        groupName = "Grupo (${selectedLayerIds.size} capas)"
                    )
                    updateProjectWithHistory(currentProject.copy(layers = currentProject.layers + groupLayer))
                    selectedLayerIds = emptySet()
                }
            },
            onDismiss = { showLayersSheet = false }
        )
    }

    // 2. TIMELINE DRAWER
    if (showTimelineDrawer) {
        LayerTimelineDrawer(
            layers = currentProject.layers,
            currentTimeMs = currentVideoTimeMs,
            onTimeChanged = { currentVideoTimeMs = it },
            onUpdateLayerTime = { layerId, newStartMs, newDurMs ->
                val updatedLayers = currentProject.layers.map { layer ->
                    if (layer.id == layerId) {
                        when (layer) {
                            is CreativeLayer.Text -> layer.copy(startOffsetMs = newStartMs, durationMs = newDurMs)
                            is CreativeLayer.Sticker -> layer.copy(startOffsetMs = newStartMs, durationMs = newDurMs)
                            is CreativeLayer.Drawing -> layer.copy(startOffsetMs = newStartMs, durationMs = newDurMs)
                            is CreativeLayer.Filter -> layer.copy(startOffsetMs = newStartMs, durationMs = newDurMs)
                            is CreativeLayer.Audio -> layer.copy(startOffsetMs = newStartMs, durationMs = newDurMs)
                            is CreativeLayer.Interactive -> layer.copy(startOffsetMs = newStartMs, durationMs = newDurMs)
                            is CreativeLayer.Group -> layer.copy(startOffsetMs = newStartMs, durationMs = newDurMs)
                            is CreativeLayer.Image -> layer.copy(startOffsetMs = newStartMs, durationMs = newDurMs)
                            is CreativeLayer.Video -> layer.copy(startOffsetMs = newStartMs, durationMs = newDurMs)
                        }
                    } else layer
                }
                updateProjectWithHistory(currentProject.copy(layers = updatedLayers))
            },
            onDismiss = { showTimelineDrawer = false }
        )
    }

    // 3. EXPORTACIÓN INTELIGENTE (Smart Export Dialog)
    if (showExportSettingsModal) {
        SmartExportSettingsDialog(
            resolution = exportResolution,
            fps = exportFps,
            quality = exportQuality,
            hdrEnabled = exportHdrEnabled,
            isPublishing = isPublishing,
            onResolutionChanged = { exportResolution = it },
            onFpsChanged = { exportFps = it },
            onQualityChanged = { exportQuality = it },
            onHdrChanged = { exportHdrEnabled = it },
            onConfirmExport = {
                if (isPublishing) return@SmartExportSettingsDialog
                isPublishing = true
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val mediaFile = if (selectedMediaUri != null) {
                            val targetDir = File(context.filesDir, "stories").apply { if (!exists()) mkdirs() }
                            val localFile = File(targetDir, "story_${System.currentTimeMillis()}.${if (isVideoMedia) "mp4" else "jpg"}")
                            context.contentResolver.openInputStream(selectedMediaUri!!)?.use { input ->
                                localFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            localFile
                        } else {
                            val targetDir = File(context.filesDir, "stories").apply { if (!exists()) mkdirs() }
                            val tempCanvasFile = File(targetDir, "story_canvas_${System.currentTimeMillis()}.jpg")
                            val bitmap = android.graphics.Bitmap.createBitmap(1080, 1920, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            val bgColorInt = android.graphics.Color.parseColor(backgroundColorHex)
                            canvas.drawColor(bgColorInt)
                            tempCanvasFile.outputStream().use { fos ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos)
                            }
                            tempCanvasFile
                        }

                        if (mediaFile.exists()) {
                            val deduplicatedFile = MediaDeduplicationEngine.deduplicateFile(mediaFile, mediaFile.parentFile ?: context.filesDir)
                            val mimeType = if (isVideoMedia) "video/mp4" else "image/jpeg"
                            val uploadId = "upload_story_${System.currentTimeMillis()}"

                            val pendingUpload = PendingUploadEntity(
                                id = uploadId,
                                userId = "current_user",
                                uploadType = "story",
                                localFilePath = deduplicatedFile.absolutePath,
                                mimeType = mimeType,
                                caption = "PanaLink Story ($exportResolution ${exportFps}fps)",
                                status = "pending",
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )

                            val db = PanalinkDatabase.getDatabase(context)
                            db.pendingUploadDao().insertUpload(pendingUpload)

                            val workRequest = OneTimeWorkRequestBuilder<SocialMediaUploadWorker>()
                                .setInputData(workDataOf("uploadId" to uploadId))
                                .build()

                            WorkManager.getInstance(context).enqueue(workRequest)
                            AutoSaveManager.clearDraft(context, currentProject.id)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Historia exportada e ingresada en cola ($exportResolution, $exportFps FPS)", Toast.LENGTH_SHORT).show()
                                showExportSettingsModal = false
                                onBack()
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Error procesando el archivo multimedia", Toast.LENGTH_SHORT).show()
                                isPublishing = false
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error al exportar: ${e.message}", Toast.LENGTH_SHORT).show()
                            isPublishing = false
                        }
                    }
                }
            },
            onDismiss = { showExportSettingsModal = false }
        )
    }

    // 4. ASSETS DESCARGABLES MODAL
    if (showAssetPacksModal) {
        DownloadableAssetPacksModal(
            onAssetPackSelected = { packName ->
                Toast.makeText(context, "Pack '$packName' listo y guardado en cache", Toast.LENGTH_SHORT).show()
                showAssetPacksModal = false
            },
            onDismiss = { showAssetPacksModal = false }
        )
    }
    }
}

@Composable
private fun QuickToolButton(
    iconLabel: String,
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF2C2C3E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconLabel,
                color = if (isSelected) Color.Black else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
        Text(
            text = title,
            color = if (isSelected) Color(0xFF00E5FF) else Color.Gray,
            fontSize = 8.sp
        )
    }
}

@Composable
private fun TextProToolPanel(
    onAddTextLayer: (CreativeLayer.Text) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var fontStyle by remember { mutableStateOf("SansSerif") }
    var selectedColorHex by remember { mutableStateOf("#FFFFFF") }
    var fontSizeSp by remember { mutableFloatStateOf(24f) }
    var hasShadow by remember { mutableStateOf(true) }

    val fonts = listOf("SansSerif", "Serif", "Monospace", "Cursive", "Montserrat", "Playfair", "Neon")
    val colors = listOf("#FFFFFF", "#00FF85", "#FF1744", "#00E5FF", "#FFEA00", "#D500F9", "#000000")

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Herramienta Texto PRO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Escribe tu texto...", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tipografías
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(fonts) { font ->
                FilterChip(
                    selected = fontStyle == font,
                    onClick = { fontStyle = font },
                    label = { Text(font, color = if (fontStyle == font) Color.Black else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Colores
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(colors) { hex ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                        .border(
                            if (selectedColorHex == hex) 2.dp else 0.dp,
                            Color.White,
                            CircleShape
                        )
                        .clickable { selectedColorHex = hex }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (textInput.isNotEmpty()) {
                    val layer = CreativeLayer.Text(
                        id = "text_${System.currentTimeMillis()}",
                        text = textInput,
                        colorHex = selectedColorHex,
                        fontSizeSp = fontSizeSp,
                        fontFamily = fontStyle,
                        hasShadow = hasShadow
                    )
                    onAddTextLayer(layer)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Añadir Texto", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StickerStudioPanel(
    onSelectSticker: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("Emojis") }
    val categories = listOf("Emojis", "Stickers", "GIF", "Menciones", "Hashtags")

    val emojis = listOf("🇻🇪", "🔥", "👑", "🌟", "😎", "😂", "🚀", "🍿", "🍕", "🍔", "☕", "🥑", "💃", "🎉", "❤️")

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Story Sticker Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(categories) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat, color = if (selectedCategory == cat) Color.Black else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.fillMaxSize()) {
            items(emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp)
                        .clickable { onSelectSticker(emoji) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 28.sp)
                }
            }
        }
    }
}

@Composable
private fun MusicStudioPanel(
    onSelectAudio: (String, Long, Long, Float) -> Unit
) {
    val tracks = listOf("Lofi Joropo ☕", "Tambor Remix 🥁", "Gaita Pop 🌟", "Cacao Acoustic 🎸", "Caracas Beats 🌌")
    var selectedTrack by remember { mutableStateOf(tracks.first()) }
    var volume by remember { mutableFloatStateOf(1.0f) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Story Music Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(tracks) { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTrack = track }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(track, color = if (selectedTrack == track) Color(0xFF00E5FF) else Color.White)
                    if (selectedTrack == track) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00E5FF))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Volumen: ", color = Color.Gray, fontSize = 12.sp)
            Slider(
                value = volume,
                onValueChange = { volume = it },
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = { onSelectAudio(selectedTrack, 0L, 15000L, volume) },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Aplicar Música", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DrawingStudioPanel(
    selectedColor: String,
    selectedWidth: Float,
    selectedTool: String,
    onColorChange: (String) -> Unit,
    onWidthChange: (Float) -> Unit,
    onToolChange: (String) -> Unit
) {
    val tools = listOf("pencil" to "Lápiz", "marker" to "Marcador", "brush" to "Pincel", "neon" to "Neón", "eraser" to "Borrador")
    val colors = listOf("#00E5FF", "#FF1744", "#00FF85", "#FFEA00", "#D500F9", "#FFFFFF", "#000000")

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Story Drawing Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tools) { (key, label) ->
                FilterChip(
                    selected = selectedTool == key,
                    onClick = { onToolChange(key) },
                    label = { Text(label, color = if (selectedTool == key) Color.Black else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF00E5FF)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Grosor: ", color = Color.Gray, fontSize = 12.sp)
            Slider(
                value = selectedWidth,
                onValueChange = onWidthChange,
                valueRange = 2f..24f,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(colors) { hex ->
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(android.graphics.Color.parseColor(hex)), CircleShape)
                        .border(if (selectedColor == hex) 2.dp else 0.dp, Color.White, CircleShape)
                        .clickable { onColorChange(hex) }
                )
            }
        }
    }
}

@Composable
private fun EffectsStudioPanel(
    activeFilter: String,
    onSelectFilter: (String) -> Unit
) {
    val filterList = listOf(
        "none" to "Ninguno",
        "cinematic" to "Cinemático",
        "vintage" to "Vintage",
        "neon" to "Neón",
        "warm" to "Cálido",
        "black_white" to "B&N"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Story Effects Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filterList) { (key, name) ->
                Card(
                    modifier = Modifier
                        .width(90.dp)
                        .height(120.dp)
                        .clickable { onSelectFilter(key) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (activeFilter == key) Color(0xFF00E5FF) else Color(0xFF2C2C3E)
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = name,
                            color = if (activeFilter == key) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveStudioPanel(
    type: String,
    onAddInteractiveLayer: (CreativeLayer.Interactive) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var optionA by remember { mutableStateOf("") }
    var optionB by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Elemento Interactivo: $type", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = titleInput,
            onValueChange = { titleInput = it },
            placeholder = { Text("Pregunta / Título / Mención...", color = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00E5FF),
                unfocusedBorderColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (type == "INTERACTIVE") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = optionA,
                    onValueChange = { optionA = it },
                    placeholder = { Text("Opción 1", color = Color.Gray) },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = optionB,
                    onValueChange = { optionB = it },
                    placeholder = { Text("Opción 2", color = Color.Gray) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (titleInput.isNotEmpty() || optionA.isNotEmpty()) {
                    val layer = CreativeLayer.Interactive(
                        id = "interactive_${System.currentTimeMillis()}",
                        interactiveType = type,
                        title = titleInput,
                        optionA = optionA,
                        optionB = optionB
                    )
                    onAddInteractiveLayer(layer)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Añadir a la Historia", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// P6.4.1 PROFESSIONAL COMPONENTS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LayersInspectorModalSheet(
    layers: List<CreativeLayer>,
    selectedLayerId: String?,
    selectedLayerIds: Set<String>,
    onSelectLayer: (String) -> Unit,
    onToggleMultiSelect: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onReorderLayer: (fromIndex: Int, toIndex: Int) -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onGroupSelectedLayers: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF16161E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inspector de Capas (${layers.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                if (selectedLayerIds.size >= 2) {
                    Button(
                        onClick = onGroupSelectedLayers,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.GroupWork, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Agrupar (${selectedLayerIds.size})", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (layers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay capas creadas aún. Añade texto, stickers o elementos.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(layers.size) { idx ->
                        val layer = layers[layers.size - 1 - idx]
                        val realIndex = layers.size - 1 - idx
                        val isSelected = (layer.id == selectedLayerId)
                        val isMultiSelected = selectedLayerIds.contains(layer.id)

                        val titleName = when (layer) {
                            is CreativeLayer.Text -> "Texto: \"${layer.text}\""
                            is CreativeLayer.Sticker -> "Sticker: ${layer.stickerUrlOrPath}"
                            is CreativeLayer.Drawing -> "Dibujo (${layer.points.size} pts)"
                            is CreativeLayer.Filter -> "Filtro: ${layer.filterName}"
                            is CreativeLayer.Audio -> "Música: ${layer.audioUrlOrPath}"
                            is CreativeLayer.Interactive -> "Interactivo: ${layer.interactiveType}"
                            is CreativeLayer.Group -> layer.groupName
                            is CreativeLayer.Image -> "Imagen"
                            is CreativeLayer.Video -> "Video"
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectLayer(layer.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF232338) else Color(0xFF1F1F2C)
                            ),
                            border = if (isSelected || isMultiSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF)) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isMultiSelected,
                                        onCheckedChange = { onToggleMultiSelect(layer.id) },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E5FF))
                                    )

                                    IconButton(onClick = { onToggleVisibility(layer.id) }) {
                                        Icon(
                                            imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Visibilidad",
                                            tint = if (layer.isVisible) Color.White else Color.Gray
                                        )
                                    }

                                    IconButton(onClick = { onToggleLock(layer.id) }) {
                                        Icon(
                                            imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Bloquear",
                                            tint = if (layer.isLocked) Color(0xFFFF5252) else Color.Gray
                                        )
                                    }

                                    Text(
                                        text = titleName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onReorderLayer(realIndex, (realIndex + 1).coerceAtMost(layers.size - 1)) },
                                        enabled = realIndex < layers.size - 1
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Subir", tint = Color.Gray)
                                    }

                                    IconButton(
                                        onClick = { onReorderLayer(realIndex, (realIndex - 1).coerceAtLeast(0)) },
                                        enabled = realIndex > 0
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Bajar", tint = Color.Gray)
                                    }

                                    IconButton(onClick = { onDuplicateLayer(layer.id) }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar", tint = Color(0xFF00E5FF))
                                    }

                                    IconButton(onClick = { onDeleteLayer(layer.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color(0xFFFF5252))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerTimelineDrawer(
    layers: List<CreativeLayer>,
    currentTimeMs: Long,
    onTimeChanged: (Long) -> Unit,
    onUpdateLayerTime: (layerId: String, startMs: Long, durMs: Long) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00E5FF))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timeline de Capas (${currentTimeMs / 1000f}s / 15s)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }

            Slider(
                value = currentTimeMs.toFloat(),
                onValueChange = { onTimeChanged(it.toLong()) },
                valueRange = 0f..15000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00E5FF),
                    activeTrackColor = Color(0xFF00E5FF)
                )
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(layers) { layer ->
                    val layerName = when (layer) {
                        is CreativeLayer.Text -> "Txt: ${layer.text}"
                        is CreativeLayer.Sticker -> "Stk: ${layer.stickerUrlOrPath}"
                        is CreativeLayer.Audio -> "Aud: ${layer.audioUrlOrPath}"
                        else -> layer.id
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF222232), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = layerName,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier.width(90.dp),
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .background(Color(0xFF2C2C3E), RoundedCornerShape(4.dp))
                        ) {
                            val startPct = (layer.startOffsetMs / 15000f).coerceIn(0f, 1f)
                            val durPct = (layer.durationMs / 15000f).coerceIn(0f, 1f - startPct)

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(durPct)
                                    .offset(x = (startPct * 200).dp)
                                    .background(Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartExportSettingsDialog(
    resolution: String,
    fps: Int,
    quality: String,
    hdrEnabled: Boolean,
    isPublishing: Boolean,
    onResolutionChanged: (String) -> Unit,
    onFpsChanged: (Int) -> Unit,
    onQualityChanged: (String) -> Unit,
    onHdrChanged: (Boolean) -> Unit,
    onConfirmExport: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Exportación Inteligente 🚀", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column {
                Text("Ajusta la calidad de renderizado antes de publicar:", color = Color.Gray, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(12.dp))

                Text("Resolución:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("1080p", "720p", "480p").forEach { res ->
                        FilterChip(
                            selected = resolution == res,
                            onClick = { onResolutionChanged(res) },
                            label = { Text(res, color = if (resolution == res) Color.Black else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Fotogramas por Segundo (FPS):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(24, 30, 60).forEach { f ->
                        FilterChip(
                            selected = fps == f,
                            onClick = { onFpsChanged(f) },
                            label = { Text("${f} FPS", color = if (fps == f) Color.Black else Color.White) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Procesamiento HDR Color", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Switch(
                        checked = hdrEnabled,
                        onCheckedChange = onHdrChanged,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirmExport,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                if (isPublishing) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                    Text("Exportar y Publicar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1F1F2C)
    )
}

@Composable
private fun DownloadableAssetPacksModal(
    onAssetPackSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val packs = listOf(
        "Halloween 🎃" to "Stickers espeluznantes, filtros oscuros y efectos de vapor",
        "Navidad 🎄" to "Luces, nieve animada y gorros navideños",
        "Anime & Gaming 🎮" to "Efectos cyberpunk, espadas de neón y barras de vida",
        "Neón Criollo 🌌" to "Frases venezolanas vibrantes y stickers caribeños",
        "Cumpleaños 🎉" to "Globos, tortas, confeti y coronas brillantes"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Biblioteca de Assets Descargables 📦", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            LazyColumn(modifier = Modifier.height(260.dp)) {
                items(packs) { (name, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAssetPackSelected(name) }
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(desc, color = Color.Gray, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.Download, contentDescription = "Descargar", tint = Color(0xFF00E5FF))
                    }
                    HorizontalDivider(color = Color(0xFF2C2C3E))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = Color(0xFF00E5FF))
            }
        },
        containerColor = Color(0xFF1F1F2C)
    )
}
