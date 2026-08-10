package com.example.debug.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaDebugScreen(
    viewModel: MediaDebugViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Media Engine Diagnostic", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121214))
            )
        },
        containerColor = Color(0xFF0D0D0F)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Estado global del Media Engine V2",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Archivos locales",
                        value = "${state.localFilesCount}",
                        icon = Icons.Default.Folder,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Espacio usado",
                        value = state.formattedCacheSize,
                        icon = Icons.Default.Storage,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Cache Hit Rate",
                        value = "%.1f%%".format(state.report?.cacheHitRatePercentage ?: 100f),
                        icon = Icons.Default.OfflinePin,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Éxito Offline",
                        value = "%.1f%%".format(state.report?.offlineSuccessRatePercentage ?: 100f),
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C22)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Detalles de Solicitudes", color = Color.Gray, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Cache Hits: ${state.report?.cacheHitCount ?: 0}", color = Color.White, fontSize = 14.sp)
                        Text("Cache Misses: ${state.report?.cacheMissCount ?: 0}", color = Color.White, fontSize = 14.sp)
                        Text("Descargas exitosas: ${state.report?.downloadSuccessCount ?: 0}", color = Color.White, fontSize = 14.sp)
                        Text("Descargas fallidas: ${state.report?.downloadFailureCount ?: 0}", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

            item {
                Button(
                    onClick = { viewModel.purgeExpiredCache() },
                    enabled = !state.isCleaning,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF), contentColor = Color.Black)
                ) {
                    if (state.isCleaning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Purgando cache...")
                    } else {
                        Icon(Icons.Default.CleaningServices, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Purgar Archivos Expirados")
                    }
                }
            }

            if (!state.message.isNullOrBlank()) {
                item {
                    Text(
                        text = state.message!!,
                        color = Color(0xFF00E5FF),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C22)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, color = Color.Gray, fontSize = 12.sp)
            Text(text = value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
