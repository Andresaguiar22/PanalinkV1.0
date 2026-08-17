package com.example.ui.reels

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.data.model.Comment
import com.example.data.model.UserStateWithUser
import com.example.data.repository.CdnManager
import com.example.data.video.CacheDataSourceFactory
import com.example.ui.viewmodel.ReelsUiState
import com.example.ui.viewmodel.ReelsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

private enum class ReelFilterV2(val label: String) {
    EXPLORE("Explorar"), NEW("Nuevos"), TRENDING("Tendencias"), MOST_VIEWED("Más vistos")
}

@OptIn(UnstableApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReelsFeedScreenV2(
    viewModel: ReelsViewModel,
    initialStateId: String,
    isActive: Boolean = true,
    onBack: () -> Unit,
    onNavigateToUserProfile: ((String) -> Unit)? = null,
    onNavigateToHashtag: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reelsState by viewModel.reelsState.collectAsStateWithLifecycle()
    val comments by viewModel.currentComments.collectAsStateWithLifecycle()
    val commentsReelId by viewModel.commentsReelId.collectAsStateWithLifecycle()
    val reels = (reelsState as? ReelsUiState.Success)?.reels.orEmpty()
    var refreshing by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(ReelFilterV2.EXPLORE) }
    var commentsDialogReelId by remember { mutableStateOf<String?>(null) }

    val targetId = initialStateId.ifBlank { viewModel.getLastViewedReelId().orEmpty() }
    val filteredReels = remember(reels, filter) {
        when (filter) {
            ReelFilterV2.EXPLORE -> reels
            ReelFilterV2.NEW -> reels.sortedByDescending { it.state.createdAt ?: "" }
            ReelFilterV2.MOST_VIEWED -> reels.sortedByDescending { it.state.viewsCount ?: 0 }
            ReelFilterV2.TRENDING -> reels.sortedByDescending {
                (it.state.likesCount ?: 0).toLong() * 3 +
                    (it.state.commentsCount ?: 0).toLong() * 4 +
                    (it.state.favoritesCount ?: 0).toLong() * 5 +
                    (it.state.sharesCount ?: 0).toLong() * 6 +
                    (it.state.viewsCount ?: 0).toLong()
            }
        }
    }

    val initialPage = remember(filteredReels, targetId) {
        filteredReels.indexOfFirst { it.state.id == targetId }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { filteredReels.size })

    LaunchedEffect(initialPage, filteredReels.size) {
        if (filteredReels.isNotEmpty()) pagerState.scrollToPage(initialPage.coerceIn(0, filteredReels.lastIndex))
    }

    LaunchedEffect(pagerState.currentPage, filteredReels, isActive) {
        if (!isActive || filteredReels.isEmpty()) return@LaunchedEffect
        filteredReels.getOrNull(pagerState.currentPage)?.let { current ->
            viewModel.rememberLastViewedReel(current.state.id)
            viewModel.registerView(current)
            
            // Trigger preloading of the next reel
            val nextIndex = pagerState.currentPage + 1
            if (nextIndex < filteredReels.size) {
                val nextUrl = CdnManager.resolveMediaUrlSync(filteredReels[nextIndex].state.mediaUrl)
                if (!nextUrl.isNullOrBlank()) {
                    viewModel.preloadNextReel(nextUrl)
                }
            }

            (pagerState.currentPage + 1..pagerState.currentPage + 2).forEach { index ->
                filteredReels.getOrNull(index)?.state?.mediaUrl?.let { raw ->
                    CdnManager.resolveMediaUrlSync(raw)?.takeIf(String::isNotBlank)?.let {
                        CacheDataSourceFactory.prefetchVideo(context, it)
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            reelsState is ReelsUiState.Loading && reels.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            reels.isEmpty() -> {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No hay Reels disponibles", color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { viewModel.refresh() }) { Text("Reintentar") }
                }
            }
            else -> VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = isActive,
                beyondViewportPageCount = 1,
            ) { page ->
                val reel = filteredReels[page]
                ReelsPageV2(
                    viewModel = viewModel,
                    reel = reel,
                    active = isActive && page == pagerState.currentPage,
                    muted = muted,
                    commentsCount = reel.state.commentsCount ?: 0,
                    onMute = { muted = !muted },
                    onLike = { viewModel.toggleLike(reel.state.id, reel.state.likedByMe == true) },
                    onFavorite = { viewModel.toggleFavorite(reel.state.id, reel.state.favoritedByMe == true) },
                    onShare = { viewModel.registerShare(reel.state.id); shareReelV2(context, reel) },
                    onComments = { commentsDialogReelId = reel.state.id },
                    onProfile = { onNavigateToUserProfile?.invoke(reel.state.userId) },
                    onHashtag = { onNavigateToHashtag?.invoke(it) },
                    onNotInterested = { viewModel.removeFromFeed(reel.state.id) },
                    onCopyLink = { copyReelLinkV2(context, reel) },
                )
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 10.dp),
            color = Color.Black.copy(alpha = .42f),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Row(Modifier.height(46.dp).padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                }
                Text("Reels", color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(3.dp))
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ReelFilterV2.values().forEach { option ->
                        FilterChip(
                            selected = filter == option,
                            onClick = { filter = option },
                            label = { Text(option.label, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = Color.White,
                                selectedLabelColor = Color.White,
                                containerColor = Color.Transparent,
                                selectedContainerColor = Color.White.copy(alpha = .22f),
                            ),
                            border = null,
                        )
                    }
                }
                IconButton(enabled = !refreshing, onClick = {
                    refreshing = true
                    viewModel.refresh()
                    scope.launch { delay(700); refreshing = false }
                }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.Refresh, "Actualizar Reels", tint = Color.White)
                }
            }
        }
        if (refreshing) {
            LinearProgressIndicator(modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 50.dp).fillMaxWidth(.86f))
        }
    }

    commentsDialogReelId?.let { reelId ->
        ReelsCommentsSheet(
            viewModel = viewModel,
            reelId = reelId,
            comments = if (commentsReelId == reelId) comments else emptyList(),
            onDismiss = { viewModel.clearComments(reelId); commentsDialogReelId = null }
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun ReelsPageV2(
    viewModel: ReelsViewModel,
    reel: UserStateWithUser,
    active: Boolean,
    muted: Boolean,
    commentsCount: Int,
    onMute: () -> Unit,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
    onComments: () -> Unit,
    onProfile: () -> Unit,
    onHashtag: (String) -> Unit,
    onNotInterested: () -> Unit,
    onCopyLink: () -> Unit,
) {
    val context = LocalContext.current
    val url = remember(reel.state.mediaUrl) { CdnManager.resolveMediaUrlSync(reel.state.mediaUrl) }
    var menuExpanded by remember { mutableStateOf(false) }
    var paused by remember(reel.state.id) { mutableStateOf(false) }
    var liked by remember(reel.state.id, reel.state.likedByMe) { mutableStateOf(reel.state.likedByMe == true) }
    var favorited by remember(reel.state.id, reel.state.favoritedByMe) { mutableStateOf(reel.state.favoritedByMe == true) }
    var likes by remember(reel.state.id, reel.state.likesCount) { mutableIntStateOf(reel.state.likesCount ?: 0) }
    var favorites by remember(reel.state.id, reel.state.favoritesCount) { mutableIntStateOf(reel.state.favoritesCount ?: 0) }
    var shares by remember(reel.state.id, reel.state.sharesCount) { mutableIntStateOf(reel.state.sharesCount ?: 0) }
    var showHeartBurst by remember(reel.state.id) { mutableStateOf(false) }
    var durationMs by remember(reel.state.id) { mutableLongStateOf(0L) }
    var positionMs by remember(reel.state.id) { mutableLongStateOf(0L) }
    
    val player = remember(url) {
        // Attempt to consume preloaded, fallback to pooled
        viewModel.consumePreloadedPlayer(url)
            ?: com.example.core.media.ExoPlayerManager.getPlayer(context).apply {
                url?.takeIf(String::isNotBlank)?.let { setMediaItem(MediaItem.fromUri(it)) }
                prepare()
            }
    }

    // Keep the player configured even if consumed preloaded
    LaunchedEffect(player, url) {
        player.repeatMode = Player.REPEAT_MODE_ONE
    }
    // Add listener if not already added to avoid multiple registrations
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                // (This listener was previously inside the remember block, 
                // but listeners need to be maintained across re-remembers if the player instance changes)
            }
        }
        player.addListener(listener)
        onDispose { 
            player.removeListener(listener)
            com.example.core.media.ExoPlayerManager.releasePlayer(player) 
        }
    }
    LaunchedEffect(active, muted, paused) {
        player.volume = if (muted) 0f else 1f
        player.playWhenReady = active && !paused
    }
    LaunchedEffect(player, active) {
        while (true) {
            if (active) {
                positionMs = player.currentPosition.coerceAtLeast(0L)
                durationMs = max(durationMs, player.duration.takeIf { it > 0L } ?: 0L)
            }
            delay(150)
        }
    }

    fun doubleTapLike() {
        showHeartBurst = true
        if (!liked) { liked = true; likes += 1; onLike() }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM; this.player = player } },
            modifier = Modifier.fillMaxSize().pointerInput(active) {
                detectTapGestures(onDoubleTap = { if (active) doubleTapLike() }, onTap = { if (active) paused = !paused })
            },
        )
        AnimatedVisibility(visible = showHeartBurst, modifier = Modifier.align(Alignment.Center), enter = fadeIn() + scaleIn(initialScale = .35f), exit = fadeOut() + scaleOut(targetScale = 1.45f)) {
            Icon(Icons.Default.Favorite, "Me gusta", tint = Color.Red, modifier = Modifier.size(150.dp))
        }
        LaunchedEffect(showHeartBurst) { if (showHeartBurst) { delay(620); showHeartBurst = false } }
        Surface(modifier = Modifier.align(Alignment.Center).size(56.dp), shape = CircleShape, color = Color.Black.copy(alpha = if (paused) .74f else .28f)) {
            IconButton(onClick = { if (active) paused = !paused }) {
                Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, "Reproducción", tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
        Column(Modifier.align(Alignment.CenterEnd).offset(y = 44.dp).padding(end = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ReelActionButtonV2(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, compactCountV2(likes), liked) {
                val next = !liked; liked = next; likes = (likes + if (next) 1 else -1).coerceAtLeast(0); onLike()
            }
            ReelActionButtonV2(Icons.Default.ChatBubbleOutline, compactCountV2(commentsCount), false, onComments)
            ReelActionButtonV2(Icons.Default.Star, compactCountV2(favorites), favorited) {
                val next = !favorited; favorited = next; favorites = (favorites + if (next) 1 else -1).coerceAtLeast(0); onFavorite()
            }
            ReelActionButtonV2(Icons.Default.Share, compactCountV2(shares), false) { shares += 1; onShare() }
            ReelActionButtonV2(if (muted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, "", false, onMute)
            Box {
                ReelActionButtonV2(Icons.Default.MoreVert, "", false) { menuExpanded = true }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("Compartir") }, onClick = { menuExpanded = false; onShare() })
                    DropdownMenuItem(text = { Text("Copiar enlace") }, onClick = { menuExpanded = false; onCopyLink() })
                    DropdownMenuItem(text = { Text("No me interesa") }, onClick = { menuExpanded = false; onNotInterested() })
                    DropdownMenuItem(text = { Text("Ver perfil") }, onClick = { menuExpanded = false; onProfile() })
                }
            }
        }
        Column(Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 16.dp, end = 90.dp, bottom = 34.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .18f), modifier = Modifier.size(40.dp).clickable(onClick = onProfile)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(reel.profile?.displayName?.firstOrNull()?.uppercase() ?: "P", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(9.dp))
                Text("@${reel.profile?.displayName?.ifBlank { "pana" } ?: "pana"}", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onProfile))
            }
            reel.state.caption?.takeIf(String::isNotBlank)?.let {
                Spacer(Modifier.height(5.dp)); Text(it, color = Color.White, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
            }
            val hashtags = Regex("#[A-Za-z0-9_ÁÉÍÓÚáéíóúÑñ]+").findAll(reel.state.caption.orEmpty()).map { it.value }.distinct().toList()
            if (hashtags.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    hashtags.take(4).forEach { tag -> Text(tag, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { onHashtag(tag.removePrefix("#")) }) }
                }
            }
        }
        if (durationMs > 0L) {
            val fraction = (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            Column(
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTimeV2(positionMs), color = Color.White, style = MaterialTheme.typography.labelSmall)
                    Text(formatTimeV2(durationMs), color = Color.White.copy(alpha = .72f), style = MaterialTheme.typography.labelSmall)
                }
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth().height(3.dp))
            }
        }
    }
}
