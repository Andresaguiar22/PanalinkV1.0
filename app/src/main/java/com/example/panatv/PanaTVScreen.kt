package com.example.panatv

import android.app.Activity
import android.os.Build
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.BorderStroke
import android.app.PictureInPictureParams
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.ui.graphics.vector.rememberVectorPainter

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanaTVScreen(viewModel: PanaTVViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val channels by viewModel.channels.collectAsState()
    val currentChannel by viewModel.currentChannel.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCountry by viewModel.selectedCountry.collectAsState()
    val availableCountries by viewModel.availableCountries.collectAsState()
    val debugMessage by viewModel.debugMessage.collectAsState()
    val crashTrace by viewModel.crashTrace.collectAsState()
    
    val showOnlyFavorites by viewModel.showOnlyFavorites.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    
    var isSearching by remember { mutableStateOf(false) }
    var isFullscreen by remember { mutableStateOf(false) }
    var playerError by remember { mutableStateOf("") }
    var globalError by remember { mutableStateOf<String?>(null) }
    var showCountryDropdown by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val view = LocalView.current

    // Diagnostics: Show crash trace if exists
    if (crashTrace.isNotEmpty()) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.clearCrashTrace() },
            title = { Text("CRASH DIAGNOSTICS", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(crashTrace, fontSize = 10.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.clearCrashTrace() }) {
                    Text("BORRAR Y CONTINUAR")
                }
            },
            containerColor = Color(0xFF1E1E1E),
            textContentColor = Color.White
        )
    }
    
    var isVideoRendering by remember { mutableStateOf(false) }
    
    val exoPlayer = remember(currentChannel) {
        val channel = currentChannel
        val player = if (channel != null) {
            com.example.util.AppFloatingPlayerManager.acquirePlayer(
                context = context,
                id = channel.id,
                url = channel.streamUrl,
                title = channel.name,
                type = "panatv"
            )
        } else {
            ExoPlayer.Builder(context).build()
        }
        player.apply {
            playWhenReady = false
            volume = if (isMuted) 0f else 1f
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    playerError = error.message ?: "Error desconocido"
                    isVideoRendering = true // stop loading on error
                }
                override fun onRenderedFirstFrame() {
                    isVideoRendering = true
                    playWhenReady = true // Start playing only when video is visible
                }
            })
        }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    var activePlayerView by remember { mutableStateOf<PlayerView?>(null) }
    
    val rebindPlayer: (PlayerView) -> Unit = remember(exoPlayer) {
        { playerView ->
            playerView.player = null
            playerView.player = exoPlayer
            val surfaceView = playerView.videoSurfaceView as? android.view.SurfaceView
            if (surfaceView != null) {
                exoPlayer.setVideoSurfaceView(surfaceView)
            } else {
                val textureView = playerView.videoSurfaceView as? android.view.TextureView
                if (textureView != null) {
                    exoPlayer.setVideoTextureView(textureView)
                }
            }
            playerView.invalidate()
            playerView.requestLayout()
        }
    }
    
    LaunchedEffect(isFullscreen) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (isFullscreen) {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
        // Force rebind surface on fullscreen change
        kotlinx.coroutines.delay(100)
        activePlayerView?.let { rebindPlayer(it) }
    }

    // Auto-select first channel (Disabled by user request)
    /*
    LaunchedEffect(channels) {
        if (currentChannel == null && channels.isNotEmpty()) {
            viewModel.selectChannel(channels.first())
        }
    }
    */

    // Test channel
    val testChannel = remember {
        PanaTVChannelEntity(
            id = "test",
            name = "Canal prueba (MUX)",
            streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
            logoUrl = "",
            country = "TEST",
            userAgent = "",
            referrer = ""
        )
    }

    LaunchedEffect(Unit) {
        // Uncomment to test with a known working stream
        // viewModel.selectChannel(testChannel)
    }

    LaunchedEffect(currentChannel) {
        currentChannel?.let { channel ->
            isVideoRendering = false
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.clearVideoSurface()
            
            val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                channel.userAgent?.takeIf { it.isNotBlank() }?.let { setUserAgent(it) }
                val defaultProps = mutableMapOf<String, String>()
                channel.referrer?.takeIf { it.isNotBlank() }?.let { defaultProps["Referer"] = it }
                setDefaultRequestProperties(defaultProps)
            }
            val mediaSource = DefaultMediaSourceFactory(context)
                .setDataSourceFactory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(channel.streamUrl))
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            
            activePlayerView?.let { rebindPlayer(it) }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> {
                    exoPlayer.play()
                    activePlayerView?.let { rebindPlayer(it) }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            val prefs = context.getSharedPreferences("panalink_prefs", android.content.Context.MODE_PRIVATE)
            val isPipEnabled = prefs.getBoolean("floating_pip_enabled", true)
            
            if (currentChannel != null && isPipEnabled) {
                com.example.util.AppFloatingPlayerManager.isFloating = true
            } else {
                com.example.util.AppFloatingPlayerManager.isFloating = false
            }
            if (!com.example.util.AppFloatingPlayerManager.isFloating) {
                exoPlayer.release()
                if (com.example.util.AppFloatingPlayerManager.exoPlayer == exoPlayer) {
                    com.example.util.AppFloatingPlayerManager.exoPlayer = null
                }
            }
        }
    }

    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    Scaffold(
        containerColor = Color(0xFF0F0F11) // Very dark background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp)
                        .background(Color(0xFF232325), RoundedCornerShape(28.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                            modifier = Modifier.fillMaxWidth(),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Buscar canales y programas...", color = Color.Gray, fontSize = 16.sp)
                                }
                                innerTextField()
                            }
                        )
                    }
                }

                // Hero Player Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .padding(horizontal = 16.dp)
                        .background(Color.Black, RoundedCornerShape(16.dp))
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    if (currentChannel != null) {
                        if (!isFullscreen) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false // Custom controls
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                update = { view ->
                                    activePlayerView = view
                                    rebindPlayer(view)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        
                        if (!isVideoRendering && currentChannel != null) {
                            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFFFD700))
                            }
                        }
                        
                        // Overlays removed for cleanliness

                        // Mini Controls
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { 
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() 
                                }) {
                                    Icon(
                                        if (exoPlayer.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = { isMuted = !isMuted }) {
                                    Icon(if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("HD", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                                IconButton(onClick = { isFullscreen = true }) {
                                    Icon(Icons.Default.Fullscreen, contentDescription = null, tint = Color.White)
                                }
                                IconButton(onClick = { 
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        (context as? Activity)?.enterPictureInPictureMode(PictureInPictureParams.Builder().build())
                                    }
                                }) {
                                    Icon(Icons.Default.PictureInPictureAlt, contentDescription = null, tint = Color.White)
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Selecciona un canal para iniciar", color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }

                // Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Canales",
                            color = Color(0xFFFFD700),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = { viewModel.toggleShowFavorites() }) {
                            Icon(
                                if (showOnlyFavorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favoritos",
                                tint = if (showOnlyFavorites) Color(0xFFE91E63) else Color.Gray
                            )
                        }
                    }
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showCountryDropdown = true }.padding(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "País: ${selectedCountry.ifEmpty { "Todos" }}",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                        DropdownMenu(
                            expanded = showCountryDropdown,
                            onDismissRequest = { showCountryDropdown = false },
                            modifier = Modifier.background(Color(0xFF232325))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Todos los países", color = Color.White) },
                                onClick = { 
                                    viewModel.updateSelectedCountry("")
                                    showCountryDropdown = false 
                                }
                            )
                            availableCountries.forEach { country ->
                                DropdownMenuItem(
                                    text = { Text(country, color = Color.White) },
                                    onClick = { 
                                        viewModel.updateSelectedCountry(country)
                                        showCountryDropdown = false 
                                    }
                                )
                            }
                        }
                    }
                }

                // Channel List
                if (channels.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (debugMessage.isNotEmpty()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = Color(0xFFFF3B30))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(debugMessage, color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                            }
                        } else {
                            Text("No se encontraron canales", color = Color.Gray, fontSize = 16.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        itemsIndexed(channels, key = { _, ch -> ch.id }) { index, channel ->
                            ChannelItem(
                                rank = index + 1,
                                channel = channel,
                                isSelected = currentChannel?.id == channel.id,
                                isFavorite = favorites.contains(channel.id),
                                onToggleFavorite = { viewModel.toggleFavorite(channel.id) },
                                onClick = { viewModel.selectChannel(channel) }
                            )
                        }
                    }
                }
            }

            // Fullscreen Player Overlay
            AnimatedVisibility(
                visible = isFullscreen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        update = { view ->
                            activePlayerView = view
                            rebindPlayer(view)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    if (!isVideoRendering && currentChannel != null) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFFFFD700))
                        }
                    }
                    
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelItem(
    rank: Int,
    channel: PanaTVChannelEntity,
    isSelected: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) Color(0xFF232325) else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$rank.",
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.width(32.dp)
            )
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (channel.logoUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(channel.logoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                        error = rememberVectorPainter(image = Icons.Default.Tv),
                        placeholder = rememberVectorPainter(image = Icons.Default.Tv)
                    )
                } else {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    channel.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Canal en Vivo • ${channel.country}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (isFavorite) Color(0xFFE91E63) else Color.Gray
                )
            }
        }
    }
}
