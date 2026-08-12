fun PostCard(
    post: com.example.data.model.PostDto,
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onMediaClick: (List<String>, Int, String?) -> Unit = { _, _, _ -> },
    onAudioPlaylistClick: (com.example.data.model.PostDto) -> Unit = {}
) {
    val context = LocalContext.current
    val currentUserId = remember {
        try { com.example.data.supabase.SupabaseClient.currentUser?.id } catch (e: Throwable) { null }
    }
    val isMyPost = currentUserId != null && post.userId == currentUserId
    var showMenu by remember { mutableStateOf(false) }

    val identityRepository = remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
    val identityState by identityRepository.observeIdentity(post.userId ?: "").collectAsStateWithLifecycle(initialValue = com.example.identity.memory.IdentityMemoryCache.profiles.get(post.userId ?: "")?.toIdentityUiState())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161618)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Post Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Resolve Avatar URL using CdnManager to fix the missing avatar bug!
                val rawAvatar = identityState?.avatarUrl ?: post.profile?.avatarUrl
                val resolvedAvatar = remember(rawAvatar) {
                    com.example.data.repository.CdnManager.resolveAvatarUrl(rawAvatar)
                }

                PanaAvatar(
                    avatarUrl = resolvedAvatar,
                    userId = identityState?.userId ?: post.profile?.id,
                    size = 40.dp,
                    borderWidth = 1.5.dp,
                    borderColor = Color(0xFF00FF85),
                    contentDescription = identityState?.displayName ?: post.profile?.displayName,
                    placeholderName = identityState?.displayName ?: post.profile?.displayName ?: ""
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = identityState?.displayName ?: post.profile?.displayName ?: "Pana de la Comunidad",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val timeStr = remember(post.createdAt) {
                        try {
                            if (post.createdAt != null) {
                                val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
                                parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
                                val date = parser.parse(post.createdAt)
                                val diff = System.currentTimeMillis() - (date?.time ?: System.currentTimeMillis())
                                val minutes = (diff / 60000).toInt()
                                when {
                                    minutes < 1 -> "hace un momento"
                                    minutes < 60 -> "hace ${minutes}m"
                                    minutes < 1440 -> "hace ${minutes / 60}h"
                                    else -> "hace ${minutes / 1440}d"
                                }
                            } else {
                                "hace poco"
                            }
                        } catch (e: Exception) {
                            "hace poco"
                        }
                    }
                    Text(text = timeStr, color = Color.Gray, fontSize = 11.sp)
                }

                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF1E222B))
                    ) {
                        if (isMyPost) {
                            DropdownMenuItem(
                                text = { Text("Editar", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White) },
                                onClick = { 
                                    showMenu = false
                                    onEditClick(post.content ?: "")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Eliminar", color = Color(0xFFFF4D4D)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF4D4D)) },
                                onClick = { 
                                    showMenu = false
                                    onDeleteClick()
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Reportar", color = Color.Gray) },
                                leadingIcon = { Icon(Icons.Default.Report, contentDescription = null, tint = Color.Gray) },
                                onClick = { 
                                    showMenu = false
                                    android.widget.Toast.makeText(context, "Publicación reportada", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Post Content / Description / YT Video
            val youtubeVideoId = remember(post.content) {
                if (!post.content.isNullOrBlank()) {
                    com.example.util.YouTubeUrlParser.extractYouTubeVideoId(post.content)
                } else {
                    null
                }
            }

            // Parse Metadata if it's potentially a Reel or has overlays
            val metadata = remember(post.content) { parseStateMetadata(post.content) }
            val cleanCaption = metadata.baseCaption

            if (!youtubeVideoId.isNullOrBlank()) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    YouTubePostCard(
                        videoId = youtubeVideoId,
                        originalText = cleanCaption
                    )
                }
            } else {
                if (cleanCaption.isNotBlank()) {
                    Text(
                        text = cleanCaption,
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Media Files (Images / Videos) resolved via CDN Sync
            val allMediaList = (post.mediaUrls ?: emptyList()).filter { it.isNotBlank() }
            val mediaImagesAndVideos = remember(allMediaList) {
                allMediaList.filter { !com.example.ui.components.isAudioUrl(it) }
            }
            val voiceAudioUrl = remember(post.audioUrl, allMediaList) {
                post.audioUrl ?: allMediaList.firstOrNull { com.example.ui.components.isAudioUrl(it) }
            }

            if (mediaImagesAndVideos.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { mediaImagesAndVideos.size })
                var isMuted by remember { mutableStateOf(true) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        val url = mediaImagesAndVideos[page]
                        // Resolve dynamic media URL via CdnManager Sync so it doesn't fail on local development URLs
                        val resolvedUrl = remember(url) {
                            com.example.data.repository.CdnManager.resolveMediaUrlSync(url)
                        }
                        val isVideo = post.type == "VIDEO" || post.type == "REEL" || com.example.ui.components.isVideoUrl(url)

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    // Map original list to resolved list for full screen viewer
                                    val resolvedList = mediaImagesAndVideos.map { com.example.data.repository.CdnManager.resolveMediaUrlSync(it) }
                                    val resolvedAudio = voiceAudioUrl?.let { com.example.data.repository.CdnManager.resolveMediaUrlSync(it) }
                                    onMediaClick(resolvedList, page, resolvedAudio)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isVideo) {
                                val resolvedResources = com.example.media.feed.PostMediaResolver.rememberResolvedMediaResources(
                                    mediaUrls = listOf(resolvedUrl),
                                    ownerId = post.userId
                                )
                                val mediaResource = resolvedResources.firstOrNull() ?: com.example.media.model.MediaResource.Remote(resolvedUrl)
                                val videoUri = when (mediaResource) {
                                    is com.example.media.model.MediaResource.Local -> Uri.fromFile(java.io.File(mediaResource.path))
                                    is com.example.media.model.MediaResource.Remote -> Uri.parse(mediaResource.url)
                                    else -> Uri.parse(resolvedUrl)
                                }
                                SimpleVideoPreviewPlayer(
                                    videoUri = videoUri,
                                    isMuted = isMuted,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                val resolvedResources = com.example.media.feed.PostMediaResolver.rememberResolvedMediaResources(
                                    mediaUrls = listOf(resolvedUrl),
                                    ownerId = post.userId
                                )
                                val mediaResource = resolvedResources.firstOrNull() ?: com.example.media.model.MediaResource.Remote(resolvedUrl)
                                com.example.media.ui.MediaRenderer(
                                    resource = mediaResource,
                                    contentDescription = "Imagen",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            
                            // Render overlays if available
                            RenderOverlays(metadata.overlaysBase64)
                        }
                    }

                    // Mute / Unmute Button for videos
                    val currentUrl = mediaImagesAndVideos.getOrNull(pagerState.currentPage) ?: ""
                    if (post.type == "VIDEO" || post.type == "REEL" || com.example.ui.components.isVideoUrl(currentUrl)) {
                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                                .size(36.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = "Sonido",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Page Indicator Badges
                    if (mediaImagesAndVideos.size > 1) {
                        Text(
                            text = "${pagerState.currentPage + 1}/${mediaImagesAndVideos.size}",
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(mediaImagesAndVideos.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) Color(0xFF00FF85) else Color.White.copy(alpha = 0.5f)
                                val width = if (pagerState.currentPage == iteration) 16.dp else 6.dp
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .height(6.dp)
                                        .width(width)
                                )
                            }
                        }
                    }

                    // Background Audio indicator badge if photos have background audio
                    if (voiceAudioUrl != null && post.type != "VIDEO" && post.type != "REEL") {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎵 Audio de fondo", color = Color(0xFF00FF85), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            } else if (voiceAudioUrl != null || post.type == "AUDIO") {
                val resolvedAudio = remember(voiceAudioUrl) {
                    com.example.data.repository.CdnManager.resolveMediaUrlSync(voiceAudioUrl ?: "")
                }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                    com.example.ui.components.PlaylistAudioPlayer(audioUrls = listOf(resolvedAudio))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Post Interaction Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isLiked = post.isLikedByMe
                IconButton(
                    onClick = { onLikeClick() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        tint = if (isLiked) Color(0xFFFF1744) else Color.White,
                        contentDescription = "Me gusta"
                    )
                }

                IconButton(
                    onClick = { onCommentClick() },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        tint = Color.White,
                        contentDescription = "Comentarios"
                    )
                }

                if (post.type == "AUDIO" || voiceAudioUrl != null) {
                    IconButton(
                        onClick = { onAudioPlaylistClick(post) },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            tint = Color(0xFF00FF85),
                            contentDescription = "Reproducir lista"
                        )
                    }
                }
            }

            // Likes and comments counters
            if (post.likesCount > 0) {
                Text(
                    text = "${post.likesCount} ${if (post.likesCount == 1) "Me gusta" else "Me gusta"}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                )
            }

            if (post.commentsCount > 0) {
                Text(
                    text = "Ver los ${post.commentsCount} comentarios",
                    color = Color.Gray,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clickable { onCommentClick() }
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

