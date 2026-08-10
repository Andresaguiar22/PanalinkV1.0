package com.example.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.creative.canvas.CanvasEditorEngine
import com.example.creative.core.CreativeLayer
import com.example.creative.inspector.PropertyInspector
import com.example.creative.post.*

/**
 * P6.6.3 - PanaLink Post Studio Pro Screen
 * Professional Canva Pro + CapCut style creator for Wall Posts and Carousels.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostStudioScreen(
    onDismiss: () -> Unit,
    initialUris: List<Uri> = emptyList(),
    viewModel: PostStudioViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val pages by viewModel.pageManager.pages.collectAsState()
    val selectedPageIndex by viewModel.pageManager.selectedPageIndex.collectAsState()

    // Init state on start
    LaunchedEffect(initialUris) {
        viewModel.initRepository(context)
        if (initialUris.isNotEmpty()) {
            viewModel.initFromUris(context, initialUris)
        }
    }

    // Modal & Tool states
    var showTextDialog by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    var showFilterSheet by remember { mutableStateOf(false) }
    var showRatioSheet by remember { mutableStateOf(false) }
    var showCaptionSheet by remember { mutableStateOf(false) }
    var showInspectorSheet by remember { mutableStateOf(false) }
    var showStickerSheet by remember { mutableStateOf(false) }
    var showAssistantSheet by remember { mutableStateOf(false) }
    var isDrawingMode by remember { mutableStateOf(false) }

    val mediaPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            uris.forEach { uri ->
                val newPage = PostMediaImporter.importMediaUri(context, uri, pageIndex = pages.size)
                viewModel.addPage(newPage)
            }
        }
    }

    val currentPage = pages.getOrElse(selectedPageIndex) { PostPage() }
    val currentCreativeProject = remember(currentPage, uiState.project.id) {
        currentPage.let { page ->
            val mainSource = (page.getMainMediaLayer() as? CreativeLayer.Image)?.imageUriOrPath
                ?: (page.getMainMediaLayer() as? CreativeLayer.Video)?.videoUriOrPath
                ?: ""
            com.example.creative.core.CreativeProject(
                id = "${uiState.project.id}_${page.id}",
                sourceMedia = mainSource,
                layers = page.layers,
                type = com.example.creative.core.CreativeType.POST
            )
        }
    }

    val selectedLayer = currentPage.layers.firstOrNull { it.id == uiState.selectedLayerId }

    Scaffold(
        containerColor = Color(0xFF030712),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.project.title,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.isExporting) Color(0xFFF59E0B) else Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isExporting) "Exportando..." else "Guardado local",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.undo() },
                        enabled = viewModel.canUndo()
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Deshacer",
                            tint = if (viewModel.canUndo()) Color.White else Color.DarkGray
                        )
                    }
                    IconButton(
                        onClick = { viewModel.redo() },
                        enabled = viewModel.canRedo()
                    ) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = "Rehacer",
                            tint = if (viewModel.canRedo()) Color.White else Color.DarkGray
                        )
                    }
                    Button(
                        onClick = {
                            viewModel.exportAndPublish(
                                context = context,
                                privacy = "public",
                                onSuccess = { onDismiss() },
                                onError = {}
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Publicar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        bottomBar = {
            Column(modifier = Modifier.background(Color(0xFF090D16))) {
                // Carousel Page Navigator
                PostCarouselNavigator(
                    pages = pages,
                    selectedPageIndex = selectedPageIndex,
                    onSelectPage = { idx -> viewModel.selectPage(idx) },
                    onAddPage = { mediaPicker.launch("image/* video/*") },
                    onDeletePage = { id -> viewModel.removePage(id) },
                    onDuplicatePage = { id -> viewModel.duplicatePage(id) },
                    onMovePage = { from, to -> viewModel.movePage(from, to) }
                )

                // Main Tool Actions
                PostStudioToolbar(
                    onToolSelected = { toolId ->
                        when (toolId) {
                            "media" -> mediaPicker.launch("image/* video/*")
                            "ai" -> showAssistantSheet = true
                            "text" -> { textInput = ""; showTextDialog = true }
                            "sticker" -> showStickerSheet = true
                            "filter" -> showFilterSheet = true
                            "draw" -> isDrawingMode = !isDrawingMode
                            "ratio" -> showRatioSheet = true
                            "inspector" -> showInspectorSheet = true
                            "caption" -> showCaptionSheet = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF030712)),
            contentAlignment = Alignment.Center
        ) {
            // Main Canvas
            val canvasAspectRatio = when (currentPage.aspectRatio) {
                "1:1" -> 1.0f
                "16:9" -> 16f / 9f
                else -> 4f / 5f
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .aspectRatio(canvasAspectRatio)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF111827))
                    .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(16.dp))
            ) {
                val mainMediaLayer = currentPage.getMainMediaLayer()

                CanvasEditorEngine(
                    project = currentCreativeProject,
                    selectedLayerId = uiState.selectedLayerId,
                    isDrawingMode = isDrawingMode,
                    strokeColorHex = "#EC4899",
                    strokeWidthDp = 4f,
                    activeFilterName = (mainMediaLayer as? CreativeLayer.Image)?.filterName ?: "Normal",
                    onProjectUpdated = { updatedProj ->
                        val curP = viewModel.pageManager.getCurrentPage()
                        viewModel.pageManager.updateLayerInCurrentPage(
                            updatedProj.layers.firstOrNull { it.id == uiState.selectedLayerId } ?: return@CanvasEditorEngine
                        )
                    },
                    onLayerSelected = { id -> viewModel.selectLayer(id) },
                    backgroundContent = {
                        if (mainMediaLayer is CreativeLayer.Image && mainMediaLayer.imageUriOrPath.isNotEmpty()) {
                            AsyncImage(
                                model = mainMediaLayer.imageUriOrPath,
                                contentDescription = "Fondo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (mainMediaLayer is CreativeLayer.Video && mainMediaLayer.videoUriOrPath.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PlayCircle,
                                    contentDescription = "Video",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF1E293B)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Post Studio Canvas",
                                    color = Color.DarkGray,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )
            }
        }
    }

    // Text Input Dialog
    if (showTextDialog) {
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Añadir Texto") },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Texto para la capa") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            viewModel.addTextLayer(textInput)
                        }
                        showTextDialog = false
                    }
                ) {
                    Text("Agregar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Aspect Ratio Sheet
    if (showRatioSheet) {
        ModalBottomSheet(
            onDismissRequest = { showRatioSheet = false },
            containerColor = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Seleccionar Relación de Aspecto",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("4:5", "1:1", "16:9").forEach { ratio ->
                        Button(
                            onClick = {
                                viewModel.setAspectRatio(ratio)
                                showRatioSheet = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (currentPage.aspectRatio == ratio) Color(0xFF38BDF8) else Color(0xFF1E293B)
                            )
                        ) {
                            Text(
                                text = ratio,
                                color = if (currentPage.aspectRatio == ratio) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter Sheet
    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            containerColor = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Filtros de Imagen",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                val filters = listOf("Normal", "Vivid", "Mono", "Warm", "Cool", "Vintage", "Cyberpunk")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filters) { fName ->
                        Button(
                            onClick = {
                                viewModel.applyFilterToCurrentPage(fName)
                                showFilterSheet = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Text(fName, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Sticker Sheet
    if (showStickerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showStickerSheet = false },
            containerColor = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Seleccionar Sticker",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                val stickers = listOf("🔥", "❤️", "🚀", "✨", "🎉", "💯", "⭐", "😍")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(stickers) { sticker ->
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .clickable {
                                    viewModel.addStickerLayer(sticker)
                                    showStickerSheet = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(sticker, fontSize = 28.sp)
                        }
                    }
                }
            }
        }
    }

    // Post Caption & Hashtags Sheet
    if (showCaptionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCaptionSheet = false },
            containerColor = Color(0xFF0F172A)
        ) {
            var captionText by remember { mutableStateOf(uiState.project.caption) }
            var hashtagInput by remember { mutableStateOf(uiState.project.hashtags.joinToString(" ")) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Texto de la Publicación",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = { Text("Escribe una descripción...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = hashtagInput,
                    onValueChange = { hashtagInput = it },
                    label = { Text("Hashtags (separados por espacio)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.setCaption(captionText)
                        val tags = hashtagInput.split(" ").filter { it.isNotBlank() }
                        viewModel.setHashtags(tags)
                        showCaptionSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                ) {
                    Text("Guardar Texto", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Property Inspector Sheet
    if (showInspectorSheet || selectedLayer != null) {
        PropertyInspector(
            selectedLayer = selectedLayer,
            selectedTrack = null,
            currentTimeMs = 0L,
            onUpdateLayer = { updated -> viewModel.updateLayer(updated) },
            onUpdateTrack = {},
            onAddKeyframe = { _, _, _, _ -> },
            onRemoveKeyframe = { _, _, _ -> },
            onDismiss = {
                showInspectorSheet = false
                viewModel.selectLayer(null)
            }
        )
    }

    // Smart Assistant Sheet
    if (showAssistantSheet) {
        com.example.creative.ai.ui.SmartAssistantPanel(
            project = uiState.project,
            currentPage = currentPage,
            onApplyPageUpdate = { updatedPage ->
                val currentPages = pages.toMutableList()
                val idx = currentPages.indexOfFirst { it.id == currentPage.id }
                if (idx >= 0) {
                    currentPages[idx] = updatedPage
                    viewModel.pageManager.reorderPages(currentPages)
                }
            },
            onApplyTemplate = { template, vars ->
                val updatedProj = template.applyTemplate(uiState.project, vars)
                viewModel.pageManager.reorderPages(updatedProj.pages)
            },
            onUpdateCaption = { captionText ->
                viewModel.setCaption(captionText)
            },
            onDismiss = { showAssistantSheet = false }
        )
    }
}
