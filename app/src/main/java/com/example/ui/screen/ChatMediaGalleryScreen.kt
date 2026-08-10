package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.chat.gallery.MediaGalleryItem
import com.example.ui.components.chat.gallery.MediaGridItem
import com.example.ui.viewmodel.MediaGalleryUiState
import com.example.ui.viewmodel.MediaGalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMediaGalleryScreen(
    chatId: String,
    onBack: () -> Unit,
    viewModel: MediaGalleryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Fotos", "Videos", "Documentos", "Audio")

    LaunchedEffect(chatId) {
        viewModel.loadMedia(chatId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Archivos multimedia",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1F2C34),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF121B22)
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1F2C34),
                contentColor = Color(0xFF00A884),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF00A884)
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title,
                                color = if (selectedTab == index) Color(0xFF00A884) else Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            ) 
                        }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is MediaGalleryUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color(0xFF00A884)
                        )
                    }
                    is MediaGalleryUiState.Empty -> {
                        EmptyGalleryState(tabs[selectedTab])
                    }
                    is MediaGalleryUiState.Error -> {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is MediaGalleryUiState.Success -> {
                        val currentList = when (selectedTab) {
                            0 -> state.photos
                            1 -> state.videos
                            2 -> state.documents
                            3 -> state.audios
                            else -> emptyList()
                        }

                        if (currentList.isEmpty()) {
                            EmptyGalleryState(tabs[selectedTab])
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(3),
                                contentPadding = PaddingValues(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(
                                    items = currentList,
                                    key = { it.id },
                                    contentType = { it.type }
                                ) { item ->
                                    MediaGridItem(
                                        item = item,
                                        onClick = {
                                            // Handle click - Navigation to viewer could be added here
                                        }
                                    )
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
fun EmptyGalleryState(tabName: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val icon = when (tabName) {
            "Fotos" -> Icons.Default.Image
            "Videos" -> Icons.Default.Videocam
            "Documentos" -> Icons.Default.Description
            "Audio" -> Icons.Default.Mic
            else -> Icons.Default.FolderOpen
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No hay $tabName aún",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 16.sp
        )
    }
}
