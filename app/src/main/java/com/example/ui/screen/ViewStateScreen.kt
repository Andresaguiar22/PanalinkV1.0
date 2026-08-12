@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.example.ui.screen

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import com.example.identity.model.toIdentityUiState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import com.example.ui.components.PanaAvatar
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.viewmodel.StatesUiState
import com.example.ui.viewmodel.StatesViewModel
import com.example.data.supabase.SupabaseClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction

data class StateMetadata(
    val baseCaption: String,
    val filter: String?,
    val musicName: String?,
    val musicTrim: Pair<Float, Float>?,
    val carouselCount: Int,
    val videoTrim: Pair<Float, Float>? = null, val overlaysBase64: String? = null
)

fun parseStateMetadata(caption: String?): StateMetadata {
    if (caption == null) return StateMetadata("", null, null, null, 1)
    
    var filter: String? = null
    var musicName: String? = null
    var musicTrim: Pair<Float, Float>? = null
    var carouselCount = 1
    var videoTrim: Pair<Float, Float>? = null
    
    // Parse Filter
    val filterRegex = "\\[Filtro:\\s*([^\\]]+)\\]".toRegex()
    val filterMatch = filterRegex.find(caption)
    if (filterMatch != null) {
        filter = filterMatch.groupValues[1].trim()
    }
    
    // Parse Music
    val musicRegex = "\\[Música:\\s*([^\\]]+)\\]".toRegex()
    val musicMatch = musicRegex.find(caption)
    if (musicMatch != null) {
        val fullMusicStr = musicMatch.groupValues[1].trim()
        if (fullMusicStr.contains("✂️")) {
            val parts = fullMusicStr.split("✂️")
            musicName = parts[0].trim().removeSuffix("(").trim()
            val timeStr = parts.getOrNull(1)?.removeSuffix(")")?.trim() ?: ""
            val timeParts = timeStr.split("-")
            val startStr = timeParts.getOrNull(0)?.trim() ?: ""
            val endStr = timeParts.getOrNull(1)?.trim() ?: ""
            
            fun parseToSeconds(s: String): Float {
                val t = s.split(":")
                if (t.size == 2) {
                    return (t[0].toIntOrNull() ?: 0) * 60f + (t[1].toIntOrNull() ?: 0)
                }
                return s.toFloatOrNull() ?: 0f
            }
            val startSec = parseToSeconds(startStr)
            val endSec = parseToSeconds(endStr)
            musicTrim = Pair(startSec, endSec)
        } else {
            musicName = fullMusicStr
        }
    }
    
    // Parse Video Trim
    val videoTrimRegex = "\\[VideoTrim:\\s*([0-9.]+)\\s*-\\s*([0-9.]+)\\]".toRegex()
    val videoTrimMatch = videoTrimRegex.find(caption)
    if (videoTrimMatch != null) {
        val startSec = videoTrimMatch.groupValues[1].toFloatOrNull() ?: 0f
        val endSec = videoTrimMatch.groupValues[2].toFloatOrNull() ?: 60f
        videoTrim = Pair(startSec, endSec)
    }
    
    // Parse Carousel
    val carouselRegex = "\\[Carrusel:\\s*([0-9]+)\\]".toRegex()
    val carouselMatch = carouselRegex.find(caption)
    if (carouselMatch != null) {
        carouselCount = carouselMatch.groupValues[1].toIntOrNull() ?: 1
    }
    
    var overlaysBase64: String? = null
    val overlaysRegex = "\\[Overlays:\\s*([^\\]]+)\\]".toRegex()
    val overlaysMatch = overlaysRegex.find(caption)
    if (overlaysMatch != null) {
        overlaysBase64 = overlaysMatch.groupValues[1].trim()
    }

    // Clean Caption
    var cleaned = caption
    cleaned = filterRegex.replace(cleaned, "")
    cleaned = musicRegex.replace(cleaned, "")
    cleaned = videoTrimRegex.replace(cleaned, "")
    cleaned = carouselRegex.replace(cleaned, ""); cleaned = overlaysRegex.replace(cleaned, "")
    cleaned = cleaned.trim()
    
    return StateMetadata(
        baseCaption = cleaned,
        filter = filter,
        musicName = musicName,
        musicTrim = musicTrim,
        carouselCount = carouselCount,
        videoTrim = videoTrim,
        overlaysBase64 = overlaysBase64
    )
}

@Composable
fun RenderOverlays(base64Str: String?) {
    if (base64Str.isNullOrEmpty()) return
    
    val overlaysData = remember(base64Str) {
        try {
            val jsonStr = String(android.util.Base64.decode(base64Str, android.util.Base64.NO_WRAP))
            val jsonObj = org.json.JSONObject(jsonStr)
            val textArr = jsonObj.optJSONArray("textOverlays")
            val stickerArr = jsonObj.optJSONArray("stickerOverlays")
            
            val stickers = mutableListOf<org.json.JSONObject>()
            if (stickerArr != null) {
                for (i in 0 until stickerArr.length()) {
                    stickers.add(stickerArr.getJSONObject(i))
                }
            }
            
            val texts = mutableListOf<org.json.JSONObject>()
            if (textArr != null) {
                for (i in 0 until textArr.length()) {
                    texts.add(textArr.getJSONObject(i))
                }
            }
            Pair(stickers, texts)
        } catch (e: Exception) {
            android.util.Log.e("RenderOverlays", "Failed to parse overlays", e)
            null
        }
    } ?: return

    val stickerList = overlaysData.first
    val textList = overlaysData.second

    Box(modifier = Modifier.fillMaxSize()) {
        stickerList.forEach { s ->
            AsyncImage(
                model = s.optString("url"),
                contentDescription = "Sticker",
                modifier = Modifier
                    .size(100.dp)
                    .offset { androidx.compose.ui.unit.IntOffset(s.optDouble("x", 0.0).toInt(), s.optDouble("y", 0.0).toInt()) }
                    .graphicsLayer {
                        scaleX = s.optDouble("scale", 1.0).toFloat()
                        scaleY = s.optDouble("scale", 1.0).toFloat()
                        rotationZ = s.optDouble("rotation", 0.0).toFloat()
                    }
            )
        }
        textList.forEach { t ->
            val fontName = t.optString("fontName", "Default")
            val hasBackground = t.optBoolean("hasBackground", false)
            val hasShadow = t.optBoolean("hasShadow", true)
            val isGradient = t.optBoolean("isGradient", false)
            val colorLong = t.optString("color").toULongOrNull() ?: androidx.compose.ui.graphics.Color.White.value.toULong()
            val color = androidx.compose.ui.graphics.Color(colorLong)
            val brush = if (isGradient) androidx.compose.ui.graphics.Brush.linearGradient(listOf(color, androidx.compose.ui.graphics.Color.White)) else null
            
            var textMod: Modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(t.optDouble("x", 0.0).toInt(), t.optDouble("y", 0.0).toInt()) }
                .graphicsLayer {
                    scaleX = t.optDouble("scale", 1.0).toFloat()
                    scaleY = t.optDouble("scale", 1.0).toFloat()
                    rotationZ = t.optDouble("rotation", 0.0).toFloat()
                }
            
            if (hasBackground) {
                textMod = textMod.background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(8.dp)
            }

            androidx.compose.material3.Text(
                text = t.optString("text"),
                color = if (brush == null) color else androidx.compose.ui.graphics.Color.Unspecified,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = when (fontName) {
                    "Serif" -> androidx.compose.ui.text.font.FontFamily.Serif
                    "Monospace" -> androidx.compose.ui.text.font.FontFamily.Monospace
                    "Cursive" -> androidx.compose.ui.text.font.FontFamily.Cursive
                    else -> androidx.compose.ui.text.font.FontFamily.Default
                },
                style = TextStyle(
                    brush = brush,
                    shadow = if (hasShadow) androidx.compose.ui.graphics.Shadow(color = androidx.compose.ui.graphics.Color.Black, offset = androidx.compose.ui.geometry.Offset(4f, 4f), blurRadius = 8f) else null
                ),
                modifier = textMod
            )
        }
    }
}
fun formatCreatedTime(isoString: String?): String {
    if (isoString == null) return "Ahora"
    try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val cleanIso = isoString.substringBefore(".")
        val date = format.parse(cleanIso) ?: return "Ahora"
        val diffMs = System.currentTimeMillis() - date.time
        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60
        val diffHour = diffMin / 60
        
        return when {
            diffSec < 45 -> "Ahora"
            diffMin < 60 -> "${diffMin} min"
            diffHour < 24 -> "${diffHour} h"
            else -> "${diffHour / 24} d"
        }
    } catch (e: Exception) {
        return "Ahora"
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun ViewStateScreen(
    viewModel: StatesViewModel,
    stateId: String,
    onClose: () -> Unit,
    onNavigateToUserProfile: ((String) -> Unit)? = null
) {
    val statesState by viewModel.statesState.collectAsState()

    if (statesState is StatesUiState.Loading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    if (statesState is StatesUiState.Error) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Error al cargar estados. Reintentando...", color = Color.White)
            LaunchedEffect(Unit) {
                delay(2000)
                onClose()
            }
        }
        return
    }

    val allStates = (statesState as? StatesUiState.Success)?.states?.filter { !it.state.isReel } ?: emptyList()
    val initialIndex = allStates.indexOfFirst { it.state.id == stateId }

    if (allStates.isEmpty() || initialIndex == -1) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("Estado expirado o no encontrado.", color = Color.White)
            LaunchedEffect(Unit) {
                delay(1500)
                onClose()
            }
        }
        return
    }

    val statesGroupedByUser = remember(allStates) {
        allStates.groupBy { it.state.userId }
    }

    val uniqueUsers = remember(allStates) {
        allStates.distinctBy { it.state.userId }.map { it.state.userId }
    }

    val initialUserId = allStates[initialIndex].state.userId
    val initialPage = uniqueUsers.indexOf(initialUserId).coerceAtLeast(0)

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { uniqueUsers.size }
    )
    val coroutineScope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) { page ->
        val userId = uniqueUsers.getOrNull(page) ?: return@HorizontalPager
        val userStates = statesGroupedByUser[userId] ?: emptyList()

        if (userStates.isNotEmpty()) {
            val initialStatusIndex = remember(page) {
                if (page == initialPage) {
                    val clickedIndex = userStates.indexOfFirst { it.state.id == stateId }
                    if (clickedIndex != -1) clickedIndex else 0
                } else {
                    0
                }
            }

            UserStoryViewer(
                viewModel = viewModel,
                userStates = userStates,
                initialStatusIndex = initialStatusIndex,
                page = page,
                isPageActive = pagerState.currentPage == page,
                uniqueUsersSize = uniqueUsers.size,
                onClose = onClose,
                onNavigateToUserProfile = onNavigateToUserProfile,
                onNextUser = {
                    if (page < uniqueUsers.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page + 1)
                        }
                    } else {
                        onClose()
                    }
                },
                onPreviousUser = {
                    if (page > 0) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(page - 1)
                        }
                    } else {
                        onClose()
                    }
                }
            )
        }
    }
}

@Composable
fun UserStoryViewer(
    viewModel: StatesViewModel,
    userStates: List<UserStateWithUser>,
    initialStatusIndex: Int,
    page: Int,
    isPageActive: Boolean,
    uniqueUsersSize: Int,
    onClose: () -> Unit,
    onNavigateToUserProfile: ((String) -> Unit)? = null,
    onNextUser: () -> Unit,
    onPreviousUser: () -> Unit
) {
    var currentStatusIndex by remember(page) { mutableStateOf(initialStatusIndex) }
    val stateWithUser = userStates.getOrNull(currentStatusIndex) ?: return

    val state = stateWithUser.state
    val profile = stateWithUser.profile
    val context = LocalContext.current
    val identityRepository = remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
    val initialCached = remember(state.userId) { com.example.identity.memory.IdentityMemoryCache.profiles.get(state.userId) }
    val identityState by identityRepository.observeIdentity(state.userId).collectAsStateWithLifecycle(initialValue = initialCached?.toIdentityUiState())

    val focusManager = LocalFocusManager.current

    var currentDurationMs by remember { mutableStateOf(6000L) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var isMuted by remember { mutableStateOf(false) }
    
    // Bottom Sheet Control (Pauses advance when open)
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showSpectatorsSheet by remember { mutableStateOf(false) }
    
    // Owner menu control
    var showOwnerMenu by remember { mutableStateOf(false) }

    val currentUid = SupabaseClient.currentUser?.id ?: ""
    val isOwner = state.userId == currentUid

    var isUserPressing by remember { mutableStateOf(false) }
    var isInputFocused by remember { mutableStateOf(false) }
    val isPaused = showCommentsSheet || showSpectatorsSheet || showOwnerMenu || isUserPressing || !isPageActive || isInputFocused

    val spectatorsList by viewModel.currentSpectators.collectAsState()
    val commentsList by viewModel.currentComments.collectAsState()
    val filteredComments = remember(commentsList) {
        val childrenGrouped = commentsList.filter { it.parentCommentId != null }.groupBy { it.parentCommentId }
        commentsList.filter { comment ->
            if (comment.deletedAt == null) {
                true
            } else {
                comment.parentCommentId == null && childrenGrouped[comment.id]?.isNotEmpty() == true
            }
        }
    }

    // Likes scale animation
    var likeScale by remember { mutableStateOf(1f) }
    val animLikeScale by animateFloatAsState(
        targetValue = likeScale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        finishedListener = { likeScale = 1f }
    )

    // Double tap like heart animation
    var showDoubleTapHeart by remember { mutableStateOf(false) }
    
    // Reply & Reactions State
    var replyText by remember { mutableStateOf("") }
    var reactionMessage by remember { mutableStateOf<String?>(null) }

    // Parse Metadata
    val metadata = remember(state.caption) { parseStateMetadata(state.caption) }
    val cleanCaption = metadata.baseCaption

    val haptic = LocalHapticFeedback.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    // Load comments and views on status change
    LaunchedEffect(state.id) {
        viewModel.registerView(state.id)
        viewModel.loadComments(state.id)
        viewModel.loadSpectators(state.id)
    }

    // Reset progress when status changes
    LaunchedEffect(currentStatusIndex) {
        elapsedMs = 0L
        val currentMediaType = userStates.getOrNull(currentStatusIndex)?.state?.mediaType ?: "text"
        currentDurationMs = if (currentMediaType == "video") {
            10000L
        } else {
            6000L
        }
    }

    LaunchedEffect(currentStatusIndex, userStates) {
        // Aggressive Preloading: 1 back and 3 ahead within the same user's stories
        val preloadIndices = listOf(currentStatusIndex - 1, currentStatusIndex + 1, currentStatusIndex + 2, currentStatusIndex + 3)
        
        preloadIndices.forEach { index ->
            if (index >= 0 && index < userStates.size) {
                val nextState = userStates[index].state
                if (nextState.mediaType == "video" && !nextState.mediaUrl.isNullOrEmpty()) {
                    com.example.data.video.CacheDataSourceFactory.prefetchVideo(context, nextState.mediaUrl)
                }
            }
        }
    }

    // Background Audio Player Loop
    val musicUrl = remember(metadata.musicName) {
        val name = metadata.musicName ?: ""
        when {
            name.contains("Lofi Joropo", ignoreCase = true) -> "https://assets.mixkit.co/music/preview/mixkit-lofi-band-925.mp3"
            name.contains("Tambor Remix", ignoreCase = true) -> "https://assets.mixkit.co/music/preview/mixkit-tribal-drums-958.mp3"
            name.contains("Gaita Pop", ignoreCase = true) -> "https://assets.mixkit.co/music/preview/mixkit-pop-05-1522.mp3"
            else -> "https://assets.mixkit.co/music/preview/mixkit-dreaming-big-31.mp3"
        }
    }
    
    val bgMediaPlayer = remember { android.media.MediaPlayer() }
    
    DisposableEffect(musicUrl, state.id) {
        if (state.mediaType != "video" && metadata.musicName != null && musicUrl.isNotEmpty()) {
            try {
                bgMediaPlayer.reset()
                bgMediaPlayer.setDataSource(context, android.net.Uri.parse(musicUrl))
                bgMediaPlayer.isLooping = true
                bgMediaPlayer.prepareAsync()
                bgMediaPlayer.setOnPreparedListener { mp ->
                    if (!isPaused) {
                        mp.start()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("UserStoryViewer", "Error preparing music player", e)
            }
        }
        onDispose {
            try {
                if (bgMediaPlayer.isPlaying) {
                    bgMediaPlayer.stop()
                }
                bgMediaPlayer.reset()
            } catch (e: Exception) {}
        }
    }
    
    LaunchedEffect(isPaused) {
        try {
            if (state.mediaType != "video" && metadata.musicName != null && musicUrl.isNotEmpty()) {
                if (isPaused) {
                    if (bgMediaPlayer.isPlaying) bgMediaPlayer.pause()
                } else {
                    bgMediaPlayer.start()
                }
            }
        } catch (e: Exception) {}
    }
    
    DisposableEffect(Unit) {
        onDispose {
            try {
                bgMediaPlayer.release()
            } catch (e: Exception) {}
        }
    }

    // Dynamic progress calculation
    val progress = if (currentDurationMs > 0) elapsedMs.toFloat() / currentDurationMs else 0f

    // Main autoplay and timing progress loop
    LaunchedEffect(currentStatusIndex, isPaused, currentDurationMs) {
        if (isPaused) return@LaunchedEffect
        val interval = 50L
        while (elapsedMs < currentDurationMs) {
            delay(interval)
            if (!isPaused) {
                elapsedMs = (elapsedMs + interval).coerceAtMost(currentDurationMs)
            }
        }
        
        // Auto-advance
        if (elapsedMs >= currentDurationMs) {
            if (currentStatusIndex < userStates.lastIndex) {
                currentStatusIndex++
            } else {
                onNextUser()
            }
        }
    }

    val backgroundColors = listOf(
        Color(0xFF1E1B24), Color(0xFF122421), Color(0xFF2B121C),
        Color(0xFF122329), Color(0xFF2C1E1B), Color(0xFF1D1715)
    )
    val chosenBg = backgroundColors[kotlin.math.abs(state.id.hashCode()) % backgroundColors.size]

    val formattedTime = formatCreatedTime(state.createdAt)

    val carouselImages = remember(state.mediaUrl, state.mediaUrls, state.id) {
        val list = mutableListOf<String>()
        val base = state.mediaUrl ?: ""
        if (state.mediaUrls != null && state.mediaUrls.isNotEmpty()) {
            list.addAll(state.mediaUrls)
        } else if (base.isNotEmpty()) {
            if (base.contains(",")) {
                list.addAll(base.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            } else {
                list.add(base)
            }
        }
        list
    }

    // Carousel Image Selection
    val timePerImage = currentDurationMs
    val innerCarouselIndex = if (carouselImages.size > 1) {
        ((elapsedMs.toFloat() / currentDurationMs) * carouselImages.size).toInt().coerceIn(0, carouselImages.lastIndex)
    } else {
        0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("state_view_player")
    ) {
        // Render Media Content with gestures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(userStates, currentStatusIndex) {
                    detectTapGestures(
                        onDoubleTap = {
                            showDoubleTapHeart = true
                            likeScale = 1.4f
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (state.likedByMe != true) {
                                viewModel.toggleLike(state.id, false, onError = { err ->
                                    android.widget.Toast.makeText(context, "Error: $err", android.widget.Toast.LENGTH_LONG).show()
                                })
                            }
                        },
                        onLongPress = {
                            // Handled to prevent onTap from firing on release
                        },
                        onPress = {
                            isUserPressing = true
                            try {
                                awaitRelease()
                            } catch (e: Exception) {
                                // Ignored
                            } finally {
                                isUserPressing = false
                            }
                        },
                        onTap = { offset ->
                            focusManager.clearFocus()
                            val screenWidth = size.width
                            if (offset.x < screenWidth * 0.3f) {
                                // Left 30% -> Previous
                                if (currentStatusIndex > 0) {
                                    currentStatusIndex--
                                } else {
                                    onPreviousUser()
                                }
                            } else {
                                // Right 70% -> Next
                                if (currentStatusIndex < userStates.lastIndex) {
                                    currentStatusIndex++
                                } else {
                                    onNextUser()
                                }
                            }
                        }
                    )
                }
        ) {
            if (state.mediaType == "text") {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(chosenBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cleanCaption,
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else if (state.mediaType == "video") {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    VideoPlayer(
                        videoUrl = com.example.data.repository.CdnManager.resolveMediaUrlSync(state.mediaUrl) ?: "",
                        isMuted = isMuted,
                        isPaused = isPaused,
                        onDurationReady = { durationMs ->
                            currentDurationMs = durationMs.toLong()
                        },
                        videoTrim = metadata.videoTrim,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                if (carouselImages.size > 1) {
                    val photoPagerState = rememberPagerState(pageCount = { carouselImages.size })
                    HorizontalPager(
                        state = photoPagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pIndex ->
                        AsyncImage(
                            model = carouselImages[pIndex],
                            contentDescription = "Foto ${pIndex + 1}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {


                    val imageUrl = carouselImages.firstOrNull() ?: state.mediaUrl
                    if (!imageUrl.isNullOrEmpty()) {
                        val resolvedStoryResource = com.example.media.social.StoryMediaResolver.rememberResolvedStoryMediaResource(state)
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val scope = rememberCoroutineScope()
                        LaunchedEffect(userStates, currentStatusIndex) {
                            com.example.media.social.StoryPreloader.preloadStories(context, userStates, currentStatusIndex, 0, scope)
                        }
                        com.example.media.ui.MediaRenderer(
                            resource = resolvedStoryResource,
                            contentDescription = "Estado Media",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        RenderOverlays(metadata.overlaysBase64)
        // Cinematic Filter Overlay
        metadata.filter?.let { activeFilter ->
            when (activeFilter) {
                "Atardecer Criollo 🌅" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFF5722).copy(alpha = 0.40f), Color(0xFFFFC107).copy(alpha = 0.25f))
                                )
                            )
                    )
                }
                "Neon Petare 🌌" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFEC407A).copy(alpha = 0.35f), Color(0xFF00E5FF).copy(alpha = 0.20f), Color.Transparent)
                                )
                            )
                    )
                }
                "Retro VHS 📺" -> {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val h = size.height
                        val w = size.width
                        var y = 0f
                        while (y < h) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.08f),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1.5f
                            )
                            y += 10f
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 140.dp, start = 20.dp)
                    ) {
                        Text(
                            text = "REC 🔴  PLAY ▶\n00:${String.format("%02d", (elapsedMs / 1000 % 60).toInt())}",
                            color = Color(0xFF00FF85),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.TopStart)
                        )
                    }
                }
            }
        }

        // Inner Carousel Dots Indicator
        if (carouselImages.size > 1 && state.mediaType == "image") {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(carouselImages.size) { idx ->
                    Box(
                        modifier = Modifier
                            .size(if (idx == innerCarouselIndex) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (idx == innerCarouselIndex) Color(0xFF00FF85) else Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // Top UI HUD (Progress Indicator + Profile Details)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Linear Progress indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                userStates.forEachIndexed { index, _ ->
                    val segmentProgress = when {
                        index < currentStatusIndex -> 1f
                        index == currentStatusIndex -> progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segmentProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }

            // Profile info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        val targetUserId = state.userId
                        if (targetUserId.isNotBlank()) {
                            onNavigateToUserProfile?.invoke(targetUserId)
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0xFF00FF85), CircleShape)
                            .padding(2.dp)
                    ) {
                        PanaAvatar(
                            avatarUrl = identityState?.avatarUrl ?: profile.avatarUrl,
                            userId = state.userId,
                            placeholderName = identityState?.displayName ?: (identityState?.displayName ?: profile.displayName),
                            size = 38.dp,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = (identityState?.displayName ?: profile.displayName),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = formattedTime,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.mediaType == "video") {
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Text(
                                text = if (isMuted) "🔇" else "🔊",
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Options menu for Owner / Viewers
                    Box {
                        IconButton(
                            onClick = { showOwnerMenu = true },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Opciones",
                                tint = Color.White
                            )
                        }
                        
                        DropdownMenu(
                            expanded = showOwnerMenu,
                            onDismissRequest = { showOwnerMenu = false },
                            modifier = Modifier.background(Color(0xFF1E222B))
                        ) {
                            if (isOwner) {
                                DropdownMenuItem(
                                    text = { Text("Eliminar estado para todos", color = Color(0xFFFF4D4D)) },
                                    leadingIcon = { Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFFF4D4D)) },
                                    onClick = {
                                        showOwnerMenu = false
                                        viewModel.deleteState(state.id) {
                                            onClose()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Ver espectadores", color = Color.White) },
                                    leadingIcon = { Text("👁", fontSize = 16.sp) },
                                    onClick = {
                                        showOwnerMenu = false
                                        showSpectatorsSheet = true
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Borrar historia para mí", color = Color(0xFFFF4D4D)) },
                                    leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFFF4D4D)) },
                                    onClick = {
                                        showOwnerMenu = false
                                        viewModel.deleteStateForMe(state.id) {
                                            onClose()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Silenciar historias", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.White) },
                                    onClick = {
                                        showOwnerMenu = false
                                        Toast.makeText(context, "Historias de ${(identityState?.displayName ?: profile.displayName)} silenciadas", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Compartir enlace", color = Color.White) },
                                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White) },
                                onClick = {
                                    showOwnerMenu = false
                                    viewModel.incrementShare(state.id)
                                    val shareUrl = "https://panalink.app/status/${state.id}"
                                    copyToClipboard(context, shareUrl)
                                    shareText(context, "¡Mira el estado de ${(identityState?.displayName ?: profile.displayName)} en PanaLink! 👉 $shareUrl")
                                    reactionMessage = "¡Enlace copiado al portapapeles!"
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }
            }
        }

        // Large animated heart popped on double tap
        AnimatedVisibility(
            visible = showDoubleTapHeart,
            enter = scaleIn(initialScale = 0.4f) + fadeIn(),
            exit = scaleOut(targetScale = 1.6f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color(0xFFFF2D55),
                modifier = Modifier.size(110.dp)
            )
        }
        LaunchedEffect(showDoubleTapHeart) {
            if (showDoubleTapHeart) {
                delay(700)
                showDoubleTapHeart = false
            }
        }

        // Unified Bottom Interactivity Panel
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .pointerInput(state.id) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -15f) {
                            viewModel.loadSpectators(state.id)
                            showSpectatorsSheet = true
                        }
                    }
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                    )
                )
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Caption overlay
            if (state.mediaType != "text" && cleanCaption.isNotBlank()) {
                Text(
                    text = cleanCaption,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    textAlign = TextAlign.Start,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Quick views count display
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val realViewsCount = spectatorsList.size
                
                Surface(
                    onClick = {
                        viewModel.loadSpectators(state.id)
                        showSpectatorsSheet = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.testTag("views_counter_pill")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isOwner) "👁 $realViewsCount personas vieron tu estado" else "👁 $realViewsCount vistas",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Ver espectadores",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (metadata.musicName != null) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF00FF85).copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFF00FF85).copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🎵", fontSize = 10.sp)
                        Text(
                            text = metadata.musicName,
                            color = Color(0xFF00FF85),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.widthIn(max = 120.dp)
                        )
                    }
                }
            }
            
            // Quick reactions bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val reactionEmojis = listOf("🔥", "👏", "😂", "🇻🇪", "❤️")
                reactionEmojis.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.10f))
                            .clickable {
                                if (!isOwner) {
                                    viewModel.sendQuickReplyToAuthor(
                                        authorId = state.userId,
                                        messageText = emoji,
                                        onSuccess = {
                                            reactionMessage = "📩 DM enviado a ${(identityState?.displayName ?: profile.displayName)} ($emoji)"
                                        },
                                        onError = { err ->
                                            android.widget.Toast.makeText(context, "Error DM: $err", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    reactionMessage = "¡Reaccionaste con $emoji!"
                                }
                                viewModel.addComment(state.id, emoji, onError = { err ->
                                    android.widget.Toast.makeText(context, "Error: $err", android.widget.Toast.LENGTH_LONG).show()
                                })
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 20.sp)
                    }
                }
            }
            
            // Interactive Social Floating Bar - Unified
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                    .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(28.dp))
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Public comments trigger (Left icon)
                IconButton(
                    onClick = { showCommentsSheet = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    BadgedBox(
                        badge = {
                            if (commentsList.isNotEmpty()) {
                                Badge(containerColor = Color(0xFF00FF85)) {
                                    Text("${commentsList.size}", color = Color.Black, fontSize = 9.sp)
                                }
                            }
                        }
                    ) {
                        Text("💬", fontSize = 18.sp)
                    }
                }

                // Main input area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isInputFocused = it.isFocused },
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(Color(0xFF00FF85)),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (replyText.isNotBlank()) {
                                val textToSend = replyText
                                replyText = ""
                                focusManager.clearFocus()
                                if (!isOwner) {
                                    viewModel.sendQuickReplyToAuthor(
                                        authorId = state.userId,
                                        messageText = textToSend,
                                        onSuccess = {
                                            reactionMessage = "📩 DM enviado a ${(identityState?.displayName ?: profile.displayName)}"
                                        }
                                    )
                                } else {
                                    viewModel.addComment(state.id, textToSend)
                                    reactionMessage = "¡Comentario publicado!"
                                }
                            }
                        }),
                        decorationBox = { innerTextField ->
                            if (replyText.isEmpty()) {
                                Text(
                                    text = if (isOwner) "Añade un comentario..." else "Responde a ${(identityState?.displayName ?: profile.displayName)}...",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                if (replyText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            val textToSend = replyText
                            replyText = ""
                            focusManager.clearFocus()
                            if (!isOwner) {
                                viewModel.sendQuickReplyToAuthor(
                                    authorId = state.userId,
                                    messageText = textToSend,
                                    onSuccess = {
                                        reactionMessage = "📩 DM enviado a ${(identityState?.displayName ?: profile.displayName)}"
                                    }
                                )
                            } else {
                                viewModel.addComment(state.id, textToSend)
                                reactionMessage = "¡Comentario publicado!"
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Enviar",
                            tint = Color(0xFF00FF85),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Divider
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.1f)))

                // Action: Like
                IconButton(
                    onClick = {
                        likeScale = 1.4f
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.toggleLike(state.id, state.likedByMe ?: false)
                    },
                    modifier = Modifier.size(36.dp).scale(animLikeScale)
                ) {
                    Icon(
                        imageVector = if (state.likedByMe == true) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (state.likedByMe == true) Color(0xFFFF2D55) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Action: Favorite
                IconButton(
                    onClick = {
                        viewModel.toggleFavorite(state.id, state.favoritedByMe ?: false)
                        reactionMessage = if (state.favoritedByMe == true) "Eliminado de favoritos" else "⭐ Guardado en favoritos"
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (state.favoritedByMe == true) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = "Favorito",
                        tint = if (state.favoritedByMe == true) Color(0xFFFFCC00) else Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Action: Share
                IconButton(
                    onClick = {
                        viewModel.incrementShare(state.id)
                        val shareUrl = "https://panalink.app/status/${state.id}"
                        copyToClipboard(context, shareUrl)
                        shareText(context, "Mira esta historia en PanaLink: $shareUrl")
                        reactionMessage = "¡Enlace copiado!"
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Compartir",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Temp floating feedback badge
        reactionMessage?.let { msg ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 160.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF00FF85), RoundedCornerShape(20.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(msg, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
            LaunchedEffect(msg) {
                delay(2000)
                reactionMessage = null
            }
        }

        // --- Custom BottomSheet: Comments ---
        AnimatedVisibility(
            visible = showCommentsSheet,
            enter = fadeIn() + expandIn(expandFrom = Alignment.BottomCenter),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { showCommentsSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f) // Full standard bottom sheet height
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color(0xFF151821))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .clickable(enabled = false) {}
                        .imePadding()
                ) {
                    // Grabber handle
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 12.dp)
                            .width(42.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Comentarios (${filteredComments.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showCommentsSheet = false }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    // List of comments
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (filteredComments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Aún no hay comentarios.\n¡Sé el primero en comentar! 💬",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(filteredComments) { comment ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PanaAvatar(
                                            avatarUrl = comment.avatarUrl,
                                            userId = comment.userId,
                                            placeholderName = comment.authorName,
                                            size = 34.dp,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = if (comment.deletedAt != null) "Eliminado" else comment.authorName,
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = formatCreatedTime(comment.createdAt),
                                                color = Color.White.copy(alpha = 0.45f),
                                                fontSize = 11.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (comment.deletedAt != null) "Este comentario ha sido eliminado" else comment.text,
                                            color = if (comment.deletedAt != null) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.9f),
                                            fontSize = 13.sp,
                                            fontStyle = if (comment.deletedAt != null) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        // Reply handler
                                        if (comment.deletedAt == null) {
                                            Text(
                                                text = "Responder",
                                                color = Color(0xFF00FF85),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clickable {
                                                        replyText = "@${comment.authorName} "
                                                     }
                                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                                            )
                                        }
                                    }

                                    // Delete comment own button
                                    if (comment.userId == currentUid && comment.deletedAt == null) {
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteComment(state.id, comment.id)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Delete,
                                                contentDescription = "Borrar comentario",
                                                tint = Color.White.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    // Text write comments input bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = { Text("Escribe un comentario...", color = Color.White.copy(alpha = 0.4f), fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E222B),
                                unfocusedContainerColor = Color(0xFF1E222B),
                                focusedBorderColor = Color(0xFF00FF85),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                            ),
                            singleLine = true,
                            trailingIcon = {
                                if (replyText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            keyboardController?.hide()
                                            val textToSend = replyText
                                            replyText = ""
                                            viewModel.addComment(state.id, textToSend, onError = { err ->
                                                android.widget.Toast.makeText(context, "Error: $err", android.widget.Toast.LENGTH_LONG).show()
                                            })
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Send,
                                            contentDescription = "Enviar",
                                            tint = Color(0xFF00FF85),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // --- Custom BottomSheet: Spectators (Who viewed) ---
        AnimatedVisibility(
            visible = showSpectatorsSheet,
            enter = fadeIn() + expandIn(expandFrom = Alignment.BottomCenter),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable { showSpectatorsSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.60f)
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color(0xFF151821))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .clickable(enabled = false) {}
                        .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 12.dp)
                            .width(42.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Personas que vieron tu estado (${spectatorsList.size})",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showSpectatorsSheet = false }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.08f))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (spectatorsList.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nadie ha visto tu estado todavía.\n¡Comparte el enlace para tener más vistas! 👁",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 14.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(spectatorsList) { spectator ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        PanaAvatar(
                                            avatarUrl = spectator.avatarUrl,
                                            userId = spectator.viewerId,
                                            placeholderName = spectator.name,
                                            size = 38.dp,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = spectator.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Visto hace ${formatCreatedTime(spectator.viewedAt)}",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 11.sp
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
}

fun shareText(context: android.content.Context, text: String) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Compartir estado"))
}

fun copyToClipboard(context: android.content.Context, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("Estado Link", text)
    clipboard.setPrimaryClip(clip)
}

@Composable
fun VideoPlayer(
    videoUrl: String,
    isMuted: Boolean,
    isPaused: Boolean,
    onDurationReady: (Int) -> Unit,
    videoTrim: Pair<Float, Float>? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var exoPlayerRef by remember { mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }

    DisposableEffect(videoUrl, videoTrim) {
        val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10000, // minBufferMs: 10s
                20000, // maxBufferMs: 20s
                200,   // bufferForPlaybackMs: 200ms for instant start
                500    // bufferForPlaybackAfterRebufferMs: 500ms
            )
            .setBackBuffer(5000, true) // Cache 5s of already played video for instant seek-back
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val player = androidx.media3.exoplayer.ExoPlayer.Builder(context)
            .setMediaSourceFactory(
                androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                    .setDataSourceFactory(com.example.data.video.CacheDataSourceFactory.getCacheDataSourceFactory(context))
            )
            .setLoadControl(loadControl)
            .build()
            .apply {
                val mediaItem = androidx.media3.common.MediaItem.fromUri(videoUrl)
                setMediaItem(mediaItem)
                repeatMode = androidx.media3.common.Player.REPEAT_MODE_OFF
                playWhenReady = !isPaused
                volume = if (isMuted) 0f else 1f
                prepare()
            }
        
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == androidx.media3.common.Player.STATE_BUFFERING || playbackState == androidx.media3.common.Player.STATE_IDLE)
                if (playbackState == androidx.media3.common.Player.STATE_READY) {
                    val duration = if (videoTrim != null) {
                        ((videoTrim.second - videoTrim.first) * 1000).toInt()
                    } else {
                        player.duration.toInt()
                    }
                    if (duration > 0) {
                        onDurationReady(duration)
                    }
                    if (videoTrim != null) {
                        player.seekTo((videoTrim.first * 1000).toLong())
                    }
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("ViewStateScreen", "ExoPlayer error on url $videoUrl: code ${error.errorCode}, message ${error.message}", error)
                hasError = true
                isBuffering = false
            }
        }
        player.addListener(listener)
        exoPlayerRef = player

        onDispose {
            player.removeListener(listener)
            player.release()
            exoPlayerRef = null
        }
    }

    LaunchedEffect(exoPlayerRef, videoTrim) {
        while (true) {
            delay(100)
            val p = exoPlayerRef
            if (p != null && videoTrim != null && p.playbackState == androidx.media3.common.Player.STATE_READY) {
                val currentPosMs = p.currentPosition
                val startMs = (videoTrim.first * 1000).toLong()
                val endMs = (videoTrim.second * 1000).toLong()
                if (currentPosMs < startMs) {
                    p.seekTo(startMs)
                } else if (currentPosMs >= endMs) {
                    p.seekTo(startMs)
                }
            }
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayerRef?.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(isPaused, exoPlayerRef) {
        exoPlayerRef?.playWhenReady = !isPaused
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    useController = false
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    player = exoPlayerRef
                }
            },
            update = { playerView ->
                playerView.player = exoPlayerRef
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (isBuffering && !hasError) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        
        if (hasError) {
            Column(
                modifier = Modifier.align(Alignment.Center).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Error al reproducir video", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    coroutineScope.launch {
                        hasError = false
                        isBuffering = true
                        
                        // forceRefresh CDN
                        com.example.data.repository.CdnManager.getCDNUrl(forceRefresh = true)
                        
                        // resolveMediaUrl actualizado
                        val newUrl = com.example.data.repository.CdnManager.resolveMediaUrlSync(videoUrl)
                        
                        // crear nuevo MediaItem y reemplazar en el player
                        exoPlayerRef?.let { player ->
                            val mediaItem = androidx.media3.common.MediaItem.fromUri(newUrl)
                            player.setMediaItem(mediaItem)
                            player.prepare()
                            player.play()
                        }
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF85))) {
                    Text("Reintentar", color = Color.Black)
                }
            }
        }
    }
}
