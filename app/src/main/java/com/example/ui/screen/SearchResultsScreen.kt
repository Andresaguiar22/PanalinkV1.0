package com.example.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.viewmodel.StatesUiState
import com.example.ui.viewmodel.StatesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    tag: String,
    viewModel: StatesViewModel,
    onBack: () -> Unit,
    onVideoClick: (String) -> Unit
) {
    val reelsState by viewModel.reelsState.collectAsState()
    
    val filteredVideos = remember(reelsState, tag) {
        if (reelsState is StatesUiState.Success) {
            val allVideos = (reelsState as StatesUiState.Success).states.filter { it.state.mediaType == "video" }
            allVideos.filter { it.state.caption?.contains("#$tag", ignoreCase = true) == true }
        } else {
            emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("#$tag", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F0F10))
            )
        },
        containerColor = Color(0xFF0F0F10)
    ) { padding ->
        if (filteredVideos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No hay vídeos para #$tag", color = Color.Gray, fontSize = 16.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredVideos) { video ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(9f / 16f)
                            .background(Color.DarkGray)
                            .clickable { onVideoClick(video.state.id) }
                    ) {
                        AsyncImage(
                            model = video.state.mediaUrl, // Coil handles video frames automatically if configured, or use thumbnail
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}
