import sys

content = open('app/src/main/java/com/example/ui/screen/ViewStateScreen.kt').read()

original_ui = """    AndroidView(
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
        modifier = modifier
    )"""

new_ui = """    var hasError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }

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
                    com.example.data.repository.CdnManager.clearCache()
                    hasError = false
                    isBuffering = true
                    exoPlayerRef?.prepare()
                    exoPlayerRef?.play()
                }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF85))) {
                    Text("Reintentar", color = Color.Black)
                }
            }
        }
    }"""

content = content.replace(original_ui, new_ui)

original_listener = """            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == androidx.media3.common.Player.STATE_READY) {"""

new_listener = """            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = (playbackState == androidx.media3.common.Player.STATE_BUFFERING || playbackState == androidx.media3.common.Player.STATE_IDLE)
                if (playbackState == androidx.media3.common.Player.STATE_READY) {"""

content = content.replace(original_listener, new_listener)

original_error = """            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("ViewStateScreen", "ExoPlayer error on url $videoUrl: code ${error.errorCode}, message ${error.message}", error)
            }"""

new_error = """            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                android.util.Log.e("ViewStateScreen", "ExoPlayer error on url $videoUrl: code ${error.errorCode}, message ${error.message}", error)
                hasError = true
                isBuffering = false
            }"""

content = content.replace(original_error, new_error)

open('app/src/main/java/com/example/ui/screen/ViewStateScreen.kt', 'w').write(content)
