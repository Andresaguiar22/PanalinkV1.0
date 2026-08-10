package com.example.features.stickers.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.features.stickers.data.StickerRepository
import com.example.features.stickers.domain.Sticker
import com.example.features.stickers.domain.StickerPack
import com.example.features.stickers.editor.StickerEditorScreen

@Composable
fun StickerPanel(
    modifier: Modifier = Modifier,
    onStickerSelected: (Sticker) -> Unit
) {
    val context = LocalContext.current
    var packs by remember { mutableStateOf<List<StickerPack>>(emptyList()) }
    var selectedPackId by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditor by remember { mutableStateOf(false) }

    suspend fun loadPacks() {
        val loadedPacks = StickerRepository.getCatalog(context).toMutableList()
        val saved = com.example.data.repository.StickerRepository.getSavedStickers(context)
        if (saved.isNotEmpty()) {
            loadedPacks.add(0, StickerPack("saved", "Guardados", "", saved.map { Sticker(it.url, "Guardado", it.url, "💾", "saved") }))
        }
        val favs = com.example.data.repository.StickerRepository.getFavoriteStickers(context)
        if (favs.isNotEmpty()) {
            loadedPacks.add(0, StickerPack("favs", "Favoritos", "", favs.map { Sticker(it.url, "Favorito", it.url, "⭐", "favs") }))
        }
        val recents = com.example.data.repository.StickerRepository.getRecentStickers(context)
        if (recents.isNotEmpty()) {
            loadedPacks.add(0, StickerPack("recent", "Recientes", "", recents.map { Sticker(it.url, "Reciente", it.url, "🕒", "recent") }))
        }
        packs = loadedPacks
        if (selectedPackId == null && loadedPacks.isNotEmpty()) {
            selectedPackId = loadedPacks.first().id
        }
    }

    LaunchedEffect(Unit) {
        loadPacks()
        isLoading = false
        launch(Dispatchers.IO) {
            com.example.data.repository.StickerRepository.syncStickersFromRemote(context)
            withContext(Dispatchers.Main) {
                loadPacks()
            }
        }
    }
    
    val coroutineScope = rememberCoroutineScope()

    val currentPack = packs.find { it.id == selectedPackId } ?: packs.firstOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(Color(0xFF1F2C34))
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF00A884))
            }
        } else if (packs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay paquetes de stickers disponibles", color = Color(0xFF8596A0), fontSize = 14.sp)
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111B21))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2A3942))
                            .clickable { showEditor = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ Crear", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                items(packs, key = { it.id }) { pack ->
                    val isSelected = pack.id == selectedPackId
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF2A3942) else Color.Transparent)
                            .clickable { selectedPackId = pack.id }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (pack.coverUrl.isNotBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(pack.coverUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = pack.name,
                                    modifier = Modifier.size(20.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Text(
                                text = pack.name,
                                color = if (isSelected) Color(0xFF00A884) else Color(0xFF8596A0),
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            currentPack?.let { pack ->
                if (pack.stickers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Este paquete no tiene stickers", color = Color(0xFF8596A0), fontSize = 13.sp)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pack.stickers, key = { it.id }) { sticker ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onStickerSelected(sticker) }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(sticker.imageUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = sticker.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    if (showEditor) {
        Dialog(
            onDismissRequest = { showEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            StickerEditorScreen(
                onBack = { showEditor = false },
                onStickerCreated = { url ->
                    showEditor = false
                    coroutineScope.launch {
                        loadPacks()
                    }
                }
            )
        }
    }
}
