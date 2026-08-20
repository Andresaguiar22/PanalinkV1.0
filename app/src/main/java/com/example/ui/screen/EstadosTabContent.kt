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
fun EstadosTabContent(
    statesState: StatesUiState,
    onNavigateToViewState: (String) -> Unit,
    onNavigateToTikTok: (String) -> Unit,
    onNavigateToCreateState: () -> Unit,
    onRefresh: () -> Unit
) {
    val colors = com.example.ui.theme.LocalAppColors.current
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    PanalinkPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                onRefresh()
                kotlinx.coroutines.delay(1200)
                isRefreshing = false
            }
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
        // "Mi Estado" row to publish
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToCreateState() }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    com.example.ui.components.PanaAvatar(
                        avatarUrl = SupabaseClient.currentProfile?.avatarUrl,
                        userId = SupabaseClient.currentUser?.id,
                        size = 56.dp,
                        borderWidth = 0.dp,
                        placeholderName = SupabaseClient.currentProfile?.displayName
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.White, CircleShape)
                            .border(1.5.dp, colors.background, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text("Mi Estado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Añade una actualización de texto, foto o vídeo", color = Color(0xFF90A4AE), fontSize = 13.sp)
                }
            }
            HorizontalDivider(color = Color(0xFF1C2D35), thickness = 0.8.dp)
        }

        // Recent Updates - Facebook-style Carousel Title Header
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Recientes de los Panas ✨👥",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Facebook-style Story Carousel (Horizontal Pager/Row)
        item {
            when (statesState) {
                is StatesUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                }
                is StatesUiState.Success -> {
                    val list = statesState.states
                    if (list.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay estados recientes entre panas. ¡Sé el primero!",
                                color = Color(0xFF90A4AE),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // Display Facebook stories in a line carousel
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.background)
                        ) {
                            // "Create state" card inside carousel
                            item {
                                Card(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(170.dp)
                                        .clickable { onNavigateToCreateState() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.secondary)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        val resolvedAvatar = remember(SupabaseClient.currentProfile?.avatarUrl) {
                                            com.example.data.repository.CdnManager.resolveAvatarUrl(SupabaseClient.currentProfile?.avatarUrl)
                                        }
                                        // My avatar
                                        AsyncImage(
                                            model = resolvedAvatar ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80",
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        // Blue Add Icon
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.Center)
                                                .offset(y = 20.dp)
                                                .size(32.dp)
                                                .background(Color.White, CircleShape)
                                                .border(2.dp, colors.secondary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        }
                                        Text(
                                            text = "Crear Estado",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 10.dp)
                                        )
                                    }
                                }
                            }

                            // Active user states grouped by user ID
                            val uniqueUserStories = list.distinctBy { it.state.userId }
                            items(uniqueUserStories, key = { it.state.userId }) { stateWithUser ->
                                val firstState = list.firstOrNull { it.state.userId == stateWithUser.state.userId } ?: stateWithUser
                                val state = firstState.state
                                val profile = stateWithUser.profile
                                val userStories = list.filter { it.state.userId == stateWithUser.state.userId }
                                val hasUnread = userStories.any { it.state.viewedByMe != true }

                                // For Thumbnail First logic: fetch a frame if it's a video to serve as a high-fidelity local fallback
                                var videoBitmap by remember(state.mediaUrl) { mutableStateOf<Bitmap?>(null) }
                                LaunchedEffect(state.mediaUrl) {
                                    if (state.mediaType.equals("video", ignoreCase = true) || state.mediaType.contains("video", ignoreCase = true) || state.isReel) {
                                        val videoUrl = state.mediaUrl ?: ""
                                        val cached: Bitmap? = null // Dummy cache
                                        if (cached != null) {
                                            videoBitmap = cached
                                        } else {
                                            withContext(Dispatchers.IO) {
                                                val retriever = MediaMetadataRetriever()
                                                try {
                                                    retriever.setDataSource(videoUrl, HashMap<String, String>())
                                                    var fetched: Bitmap? = null
                                                    
                                                    // Try 1: Frame at 1 second sync
                                                    try {
                                                        fetched = retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                                    } catch (e: Exception) {
                                                        android.util.Log.w("StoryThumbnail", "Failed to get frame at 1s sync for $videoUrl: ${e.message}")
                                                    }
                                                    
                                                    // Try 2: Frame at 0s sync
                                                    if (fetched == null) {
                                                        try {
                                                            fetched = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                                        } catch (e: Exception) {
                                                            android.util.Log.w("StoryThumbnail", "Failed to get frame at 0s sync for $videoUrl: ${e.message}")
                                                        }
                                                     }
                                                     
                                                     // Try 3: Default representative frame
                                                     if (fetched == null) {
                                                         try {
                                                             fetched = retriever.getFrameAtTime()
                                                         } catch (e: Exception) {
                                                             android.util.Log.w("StoryThumbnail", "Failed to get default frame for $videoUrl: ${e.message}")
                                                         }
                                                     }
                                                     
                                                     // Try 4: Frame at any time (-1)
                                                     if (fetched == null) {
                                                         try {
                                                             fetched = retriever.getFrameAtTime(-1)
                                                         } catch (e: Exception) {
                                                             android.util.Log.e("StoryThumbnail", "Failed to get frame at -1 for $videoUrl: ${e.message}")
                                                         }
                                                     }
                                                     
                                                     if (fetched != null) {
                                                         videoThumbnailCache[videoUrl] = fetched
                                                         videoBitmap = fetched
                                                     } else {
                                                         android.util.Log.e("StoryThumbnail", "All thumbnail extraction attempts failed for $videoUrl")
                                                     }
                                                } catch (e: Exception) {
                                                    android.util.Log.e("StoryThumbnail", "Error setting datasource for $videoUrl", e)
                                                } finally {
                                                    try {
                                                        retriever.release()
                                                    } catch (e: Exception) {}
                                                }
                                            }
                                        }
                                    }
                                }

                                val thumbnailUrl = remember(state.mediaUrl) {
                                    val url = state.mediaUrl ?: ""
                                    if (url.contains("/videos/")) {
                                        url.replace("/videos/", "/images/")
                                           .replace("video_", "thumb_video_")
                                           .replace(".mp4", ".jpg")
                                           .replace(".mov", ".jpg")
                                           .replace(".3gp", ".jpg")
                                    } else {
                                        url
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(170.dp)
                                        .clickable { onNavigateToViewState(state.id) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = colors.secondary),
                                    border = if (hasUnread) {
                                        BorderStroke(1.5.dp, com.example.ui.theme.getPremiumActiveIconGradient())
                                    } else {
                                        BorderStroke(1.5.dp, Color.Gray.copy(alpha = 0.5f))
                                    }
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        // Media background
                                        if (state.mediaType == "text") {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFF7E57C2)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = state.caption ?: "",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 4,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        } else if (state.mediaType.equals("video", ignoreCase = true) || state.mediaType.contains("video", ignoreCase = true) || state.isReel) {
                                            Box(modifier = Modifier.fillMaxSize()) {
                                                val currentBitmap = videoBitmap
                                                if (currentBitmap != null) {
                                                    Image(
                                                        bitmap = currentBitmap.asImageBitmap(),
                                                        contentDescription = "Miniatura Historia",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    // High-fidelity dark slate loading state with green progress indicator
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxSize()
                                                            .background(Color(0xFF1E1E1E)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(24.dp),
                                                            color = Color(0xFF00FF85),
                                                            strokeWidth = 2.dp
                                                        )
                                                    }
                                                }
                                                
                                                // Speaker icon (state control) in the bottom-right corner of the Box
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(bottom = 8.dp, end = 8.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                                        .padding(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VolumeUp,
                                                        contentDescription = "Contenido Multimedia",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            AsyncImage(
                                                model = state.mediaUrl ?: "https://images.unsplash.com/photo-1563911302283-d2bc1d9e2659?auto=format&fit=crop&w=150&q=80",
                                                contentDescription = "Estado",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        }

                                        // Dark gradient overlay for bottom name
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                                    )
                                                )
                                        )

                                        // Top-left user avatar
                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        val identityRepository = androidx.compose.runtime.remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
                                        val identityState by identityRepository.observeIdentity(stateWithUser.state.userId).collectAsStateWithLifecycle(initialValue = com.example.identity.memory.IdentityMemoryCache.profiles.get(stateWithUser.state.userId)?.toIdentityUiState())
                                        val safeAvatarUrl = identityState?.avatarUrl ?: profile.avatarUrl
                                        val safeUserId = identityState?.userId ?: profile.id
                                        val safeDisplayName = identityState?.displayName ?: profile.displayName

                                        com.example.ui.components.PanaAvatar(
                                            avatarUrl = safeAvatarUrl,
                                            userId = safeUserId,
                                            size = 28.dp,
                                            borderWidth = 1.5.dp,
                                            borderColor = if (hasUnread) colors.primary else Color.Gray.copy(alpha = 0.5f),
                                            placeholderName = safeDisplayName,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .align(Alignment.TopStart)
                                        )
                                        // Bottom name text
                                        Text(
                                            text = safeDisplayName.split(" ").first(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is StatesUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Error al cargar estados", color = Color.Red, fontSize = 13.sp)
                    }
                }
            }
        }

        // Section Title: Videos from all users (TikTok style)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.background)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "El Feed de Panalink 🇻🇪",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Grid-like listing of all videos published by all users
        when (statesState) {
            is StatesUiState.Success -> {
                val list = statesState.states
                val videoStates = list.filter { 
                    it.state.mediaType.equals("video", ignoreCase = true) || 
                    it.state.mediaType.contains("video", ignoreCase = true) || 
                    it.state.isReel || 
                    it.state.type.equals("reel", ignoreCase = true)
                }

                if (videoStates.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF37474F), modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Aún no hay vídeos publicados en la comunidad.",
                                color = Color(0xFF90A4AE),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "¡Sube un vídeo desde tu galería para comenzar el ambiente!",
                                color = Color(0xFF607D8B),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    // Display them in a beautiful 2-column grid-like structure (by chunking 2 items per row)
                    val rows = videoStates.chunked(2)
                    items(rows) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (videoState in rowItems) {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val identityRepository = androidx.compose.runtime.remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
                                val identityState by identityRepository.observeIdentity(videoState.state.userId).collectAsStateWithLifecycle(initialValue = com.example.identity.memory.IdentityMemoryCache.profiles.get(videoState.state.userId)?.toIdentityUiState())
                                val safeAvatarUrl = identityState?.avatarUrl ?: videoState.profile.avatarUrl
                                val safeDisplayName = identityState?.displayName ?: videoState.profile.displayName

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.75f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.secondary)
                                        .border(1.dp, Color(0xFF262629), RoundedCornerShape(12.dp))
                                        .clickable { onNavigateToTikTok(videoState.state.id) }
                                ) {
                                    // Visual card design
                                    if (videoState.state.mediaType.equals("video", ignoreCase = true) || videoState.state.mediaType.contains("video", ignoreCase = true) || videoState.state.isReel) {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Color.Black),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription="Play", tint=Color.White)
                                        }
                                    } else {
                                        AsyncImage(
                                            model = videoState.state.mediaUrl ?: "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=300&q=80",
                                            contentDescription = "Video Thumbnail",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    // Immersive Dark Overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                                )
                                            )
                                    )

                                    // Play icon overlay
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                            .padding(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Ver Video",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }

                                    // Uploader detail overlay at bottom
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            AsyncImage(
                                                model = safeAvatarUrl ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=100&q=80",
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(18.dp)
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Text(
                                                text = safeDisplayName.split(" ").first(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        if (!videoState.state.caption.isNullOrBlank()) {
                                            Text(
                                                text = videoState.state.caption,
                                                color = Color.White.copy(alpha = 0.9f),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            // If row is not complete, add a spacer to balance the weight
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            else -> {
                items(5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .shimmerEffect()
                        )
                        // Name and message preview column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(16.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .shimmerEffect()
                            )
                        }
                        // Trailing info
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .shimmerEffect()
                        )
                    }
                }
            }
        }
    }
}
}

@OptIn(ExperimentalFoundationApi::class)

@Composable
fun StateItemRow(
    stateWithUser: UserStateWithUser,
    onNavigateToViewState: (String) -> Unit
) {
    val state = stateWithUser.state
    val profile = stateWithUser.profile
    val formattedTime = com.example.data.model.formatIsoDateTime(state.createdAt)
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val identityRepository = androidx.compose.runtime.remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
    val identityState by identityRepository.observeIdentity(state.userId).collectAsStateWithLifecycle(initialValue = com.example.identity.memory.IdentityMemoryCache.profiles.get(state.userId)?.toIdentityUiState())
    
    val safeAvatarUrl = identityState?.avatarUrl ?: profile.avatarUrl
    val safeDisplayName = identityState?.displayName ?: profile.displayName
    val safeUserId = identityState?.userId ?: profile.id

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToViewState(state.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .border(2.dp, com.example.ui.theme.getPremiumActiveIconGradient(), CircleShape)
                .padding(3.dp)
        ) {
            com.example.ui.components.PanaAvatar(
                avatarUrl = safeAvatarUrl,
                userId = safeUserId,
                size = 54.dp,
                borderWidth = 0.dp,
                placeholderName = safeDisplayName,
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = safeDisplayName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Publicado hoy, $formattedTime",
                color = Color(0xFF90A4AE),
                fontSize = 12.sp
            )
        }
    }
}

// Global cache for video thumbnails to avoid fetching repeatedly
val videoThumbnailCache = java.util.concurrent.ConcurrentHashMap<String, Bitmap>()


@Composable
fun VideoThumbnail(
    videoUrl: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    val colors = com.example.ui.theme.LocalAppColors.current
    var bitmap by remember(videoUrl) { mutableStateOf(videoThumbnailCache[videoUrl]) }
    var isLoading by remember(videoUrl) { mutableStateOf(bitmap == null) }

    LaunchedEffect(videoUrl) {
        if (bitmap == null) {
            isLoading = true
            val fetchedBitmap = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(videoUrl, HashMap<String, String>())
                    // Fetch a frame at 1 second (1,000,000 microseconds)
                    retriever.getFrameAtTime(1000000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (e: Exception) {
                    android.util.Log.e("VideoThumbnail", "Error fetching frame for $videoUrl", e)
                    null
                } finally {
                    try {
                        retriever.release()
                    } catch (e: Exception) {}
                }
            }
            if (fetchedBitmap != null) {
                videoThumbnailCache[videoUrl] = fetchedBitmap
                bitmap = fetchedBitmap
            }
            isLoading = false
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            // Nice fallback landscape background
            AsyncImage(
                model = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=300&q=80",
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        }
    }
}

