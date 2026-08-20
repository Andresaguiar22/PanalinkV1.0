@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screen

import com.example.ui.components.*
import com.example.util.*

import androidx.compose.foundation.BorderStroke
import com.example.ui.components.FeedPostCard
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import com.example.ui.viewmodel.StatesViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import coil.compose.AsyncImage
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.animation.core.*
import androidx.compose.animation.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.asImageBitmap
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.identity.model.toIdentityUiState
import androidx.navigation.NavGraph.Companion.findStartDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.model.*
import com.example.data.supabase.SupabaseClient
import com.example.ui.viewmodel.*
import com.example.ui.theme.shimmerEffect
import com.example.ui.theme.getAvatarGradient
import com.example.ui.components.PanalinkPullToRefreshBox
import com.example.ui.theme.bounceClick
import com.example.ui.components.chat.list.ChatPreviewCard
import com.example.util.ChatListScrollManager
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.text.SimpleDateFormat
import java.util.*

import com.example.ui.viewmodel.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingUploadsBanner(
    pendingUploadsViewModel: com.example.ui.viewmodel.PendingUploadsViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeUploads by pendingUploadsViewModel.activeUploads.collectAsState()
    val progressMap by pendingUploadsViewModel.uploadProgressMap.collectAsState()
    var showStatusCenterModal by remember { mutableStateOf(false) }
    var itemToCancel by remember { mutableStateOf<com.example.data.database.PendingUploadEntity?>(null) }
    var deleteLocalFileOnCancel by remember { mutableStateOf(false) }

    if (activeUploads.isEmpty() && !showStatusCenterModal) return

    val failedUploads = remember(activeUploads) { activeUploads.filter { it.status == "failed" } }
    val uploadingUploads = remember(activeUploads) { activeUploads.filter { it.status == "uploading" } }
    val pendingUploads = remember(activeUploads) { activeUploads.filter { it.status == "pending" } }
    val hasFailed = failedUploads.isNotEmpty()

    if (activeUploads.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (hasFailed) Color(0xFF2C1618) else Color(0xFF19232D)
            ),
            border = BorderStroke(
                1.dp,
                if (hasFailed) Color(0xFFFF453A) else Color(0xFF007AFF)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Gestor 📊",
                            color = Color(0xFF64D2FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showStatusCenterModal = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )

                        if (hasFailed) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Button(
                                onClick = {
                                    failedUploads.forEach { item ->
                                        pendingUploadsViewModel.retryUpload(context, item.id)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF453A)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("Reintentar 🔄", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Section 1: Uploading Items with real progress
                uploadingUploads.forEach { item ->
                    val file = remember(item.localFilePath) { java.io.File(item.localFilePath) }
                    val progressInfo = progressMap[item.id]
                    val percent = progressInfo?.progressPercent ?: 0
                    val bytesWritten = progressInfo?.bytesWritten ?: 0L
                    val totalBytes = if ((progressInfo?.totalBytes ?: 0L) > 0L) progressInfo!!.totalBytes else file.length()
                    val statusText = progressInfo?.statusText ?: "Subiendo..."

                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = "📤", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$statusText ${item.uploadType.uppercase()} ($percent%)",
                                    color = Color(0xFF64D2FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Cancelar ✕",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable {
                                        itemToCancel = item
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        LinearProgressIndicator(
                            progress = { (percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF007AFF),
                            trackColor = Color(0xFF1C2D3D)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${localFormatFileSize(bytesWritten)} / ${localFormatFileSize(totalBytes)}",
                            color = Color.LightGray,
                            fontSize = 10.sp
                        )
                    }
                }

                // Section 2: Pending Items
                pendingUploads.forEach { item ->
                    val file = remember(item.localFilePath) { java.io.File(item.localFilePath) }
                    val fileSizeText = remember(file) { localFormatFileSize(file.length()) }

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "⏳", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "En cola: ${item.uploadType.uppercase()} ($fileSizeText)",
                                color = Color(0xFFFFD60A),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        Text(
                            text = "Cancelar ✕",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clickable {
                                    itemToCancel = item
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Section 3: Failed Items
                failedUploads.forEach { failedItem ->
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Error al publicar ${failedItem.uploadType.uppercase()}",
                                color = Color(0xFFFF453A),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = failedItem.errorMessage ?: "Conexión interrumpida o fallo temporal",
                                color = Color(0xFFFF9F0A),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Reintentar",
                                color = Color(0xFF64D2FF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        pendingUploadsViewModel.retryUpload(context, failedItem.id)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Eliminar ✕",
                                color = Color.Gray,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .clickable {
                                        pendingUploadsViewModel.dismissUpload(failedItem.id)
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Cancellation Dialog
    if (itemToCancel != null) {
        val target = itemToCancel!!
        AlertDialog(
            onDismissRequest = { itemToCancel = null },
            title = {
                Text("¿Cancelar subida de ${target.uploadType.uppercase()}?", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Se detendrá el proceso de publicación actual.",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { deleteLocalFileOnCancel = !deleteLocalFileOnCancel }
                    ) {
                        Checkbox(
                            checked = deleteLocalFileOnCancel,
                            onCheckedChange = { deleteLocalFileOnCancel = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFFF453A),
                                uncheckedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Eliminar también archivo local",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingUploadsViewModel.cancelUpload(context, target.id, deleteLocalFileOnCancel)
                        itemToCancel = null
                        deleteLocalFileOnCancel = false
                    }
                ) {
                    Text("Cancelar Subida", color = Color(0xFFFF453A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToCancel = null }) {
                    Text("Volver", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1C1C1E)
        )
    }

    // Status Center Modal / Sheet
    if (showStatusCenterModal) {
        MediaStatusCenterModal(
            pendingUploadsViewModel = pendingUploadsViewModel,
            onDismiss = { showStatusCenterModal = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun MediaStatusCenterModal(
    pendingUploadsViewModel: com.example.ui.viewmodel.PendingUploadsViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val allUploads by pendingUploadsViewModel.allUploads.collectAsState()
    val progressMap by pendingUploadsViewModel.uploadProgressMap.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Todas, 1: Activas, 2: Fallidas, 3: Completadas

    val filteredList = remember(allUploads, selectedTab) {
        when (selectedTab) {
            1 -> allUploads.filter { it.status == "uploading" || it.status == "pending" }
            2 -> allUploads.filter { it.status == "failed" }
            3 -> allUploads.filter { it.status == "completed" || it.status == "cancelled" }
            else -> allUploads
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF141416),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Centro de Estado Multimedia 📊",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Text("✕", color = Color.Gray, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF007AFF)
            ) {
                listOf("Todas", "En proceso", "Fallidas", "Historial").forEachIndexed { index, label ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                label,
                                color = if (selectedTab == index) Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay publicaciones en esta categoría",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 380.dp)
                ) {
                    itemsIndexed(filteredList, key = { index, item -> "${item.id}_$index" }) { _, item ->
                        val file = remember(item.localFilePath) { java.io.File(item.localFilePath) }
                        val sizeText = remember(file) { localFormatFileSize(file.length()) }
                        val progressInfo = progressMap[item.id]
                        val percent = progressInfo?.progressPercent ?: 0
                        val bytesWritten = progressInfo?.bytesWritten ?: 0L
                        val totalBytes = if ((progressInfo?.totalBytes ?: 0L) > 0L) progressInfo!!.totalBytes else file.length()
                        val statusText = progressInfo?.statusText ?: "Procesando"

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.uploadType.uppercase(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            LocalStatusChip(status = item.status)
                                            if (item.status == "uploading") {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "$percent%",
                                                    color = Color(0xFF64D2FF),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        val safeCaption = item.caption
                                        if (!safeCaption.isNullOrEmpty()) {
                                            Text(
                                                text = safeCaption,
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = if (item.status == "uploading") {
                                                "$statusText (${localFormatFileSize(bytesWritten)} / ${localFormatFileSize(totalBytes)})"
                                            } else {
                                                "Tamaño: $sizeText ${if (item.errorMessage != null) "• " + item.errorMessage else ""}"
                                            },
                                            color = if (item.status == "failed") Color(0xFFFF453A) else Color.Gray,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (item.status == "failed") {
                                            Text(
                                                text = "Reintentar 🔄",
                                                color = Color(0xFF64D2FF),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable {
                                                        pendingUploadsViewModel.retryUpload(context, item.id)
                                                    }
                                                    .padding(6.dp)
                                            )
                                        }
                                        Text(
                                            text = "Borrar ✕",
                                            color = Color.Gray,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    pendingUploadsViewModel.dismissUpload(item.id)
                                                }
                                                .padding(6.dp)
                                        )
                                    }
                                }

                                if (item.status == "uploading") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { (percent / 100f).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = Color(0xFF007AFF),
                                        trackColor = Color(0xFF2C2C2E)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    pendingUploadsViewModel.clearCompletedUploads()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Limpiar historial completado 🧹", color = Color.LightGray, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@Composable
fun LocalStatusChip(status: String) {
    val (bgColor, textColor, label) = when (status) {
        "uploading" -> Triple(Color(0xFF0C2A4A), Color(0xFF64D2FF), "Subiendo")
        "pending" -> Triple(Color(0xFF332B00), Color(0xFFFFD60A), "En cola")
        "failed" -> Triple(Color(0xFF3B0D0C), Color(0xFFFF453A), "Fallido")
        "completed" -> Triple(Color(0xFF0F3818), Color(0xFF30D158), "Publicado")
        "cancelled" -> Triple(Color(0xFF262628), Color(0xFF8E8E93), "Cancelado")
        else -> Triple(Color.DarkGray, Color.White, status)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

fun localFormatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024 * 1024)
    return if (mb < 0.1) {
        val kb = bytes.toDouble() / 1024
        String.format("%.1f KB", kb)
    } else {
        String.format("%.1f MB", mb)
    }
}


@Composable
fun PendingPostCard(post: com.example.data.database.PendingPostEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("P", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Subiendo publicación...", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("En cola local", color = Color.Gray, fontSize = 11.sp)
                }
            }
            if (!post.content.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(post.content, color = Color.LightGray, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF00FF85),
                trackColor = Color(0xFF2C2C2E)
            )
        }
    }
}
