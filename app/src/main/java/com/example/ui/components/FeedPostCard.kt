package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screen.parseStateMetadata
import com.example.ui.screen.RenderOverlays
import com.example.data.model.PostDto
import com.example.identity.model.toIdentityUiState


fun isVideoUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val lower = url.lowercase()
    return lower.contains(".mp4") || lower.contains(".mov") || lower.contains(".webm") || lower.contains(".mkv") || lower.contains("video")
}

fun isAudioUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val lower = url.lowercase()
    return lower.contains(".mp3") || lower.contains(".wav") || lower.contains(".ogg") || lower.contains(".m4a") || lower.contains("audio")
}

fun isDocumentUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val lower = url.lowercase()
    return lower.contains(".pdf") || lower.contains(".doc") || lower.contains(".docx") ||
           lower.contains(".xls") || lower.contains(".xlsx") || lower.contains(".ppt") ||
           lower.contains(".pptx") || lower.contains(".zip") || lower.contains(".rar") ||
           lower.contains(".txt") || lower.contains(".csv") || lower.contains(".json") ||
           lower.contains("document") || lower.contains("application/") || lower.contains("file/")
}

@Composable
fun FeedPostCard(
    post: PostDto,
    onLikeClick: () -> Unit = {},
    onCommentClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onEditClick: (String) -> Unit = {},
    onMediaClick: (List<String>, Int, String?) -> Unit = { _, _, _ -> },
    onAudioPlaylistClick: (PostDto) -> Unit = {},
    onShareClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentUserId = remember {
        try { com.example.data.supabase.SupabaseClient.currentUser?.id } catch (e: Throwable) { null }
    }
    val isMyPost = currentUserId != null && post.userId == currentUserId
    var showMenu by remember { mutableStateOf(false) }

    val identityRepository = remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
    val identityState by identityRepository.observeIdentity(post.userId ?: "").collectAsStateWithLifecycle(initialValue = com.example.identity.memory.IdentityMemoryCache.profiles.get(post.userId ?: "")?.toIdentityUiState())

    var isSaved by remember { mutableStateOf(false) }
    var isExpandedText by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E22)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Resolve Avatar
                val rawAvatar = identityState?.avatarUrl ?: post.profile?.avatarUrl
                val resolvedAvatar = remember(rawAvatar) {
                    com.example.data.repository.CdnManager.resolveAvatarUrl(rawAvatar)
                }
                
                PanaAvatar(
                    avatarUrl = resolvedAvatar,
                    userId = identityState?.userId ?: post.profile?.id,
                    size = 46.dp,
                    borderWidth = 1.5.dp,
                    borderColor = Color(0xFF00FF85),
                    contentDescription = identityState?.displayName ?: post.profile?.displayName,
                    placeholderName = identityState?.displayName ?: post.profile?.displayName ?: ""
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = identityState?.displayName ?: post.profile?.displayName ?: "Pana de la Comunidad",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
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
                    Text(text = timeStr, color = Color.Gray, fontSize = 12.sp)
                }
                
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Opciones", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color(0xFF2A2A30))
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
                                    Toast.makeText(context, "Publicación reportada", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Extract content and metadata
            val youtubeVideoId = remember(post.content) {
                if (!post.content.isNullOrBlank()) {
                    com.example.util.YouTubeUrlParser.extractYouTubeVideoId(post.content)
                } else null
            }
            val metadata = remember(post.content) { parseStateMetadata(post.content) }
            val cleanCaption = metadata.baseCaption

            // 1. Text Content (Before Media)
            if (cleanCaption.isNotBlank() && youtubeVideoId.isNullOrBlank()) {
                val isLongText = cleanCaption.length > 150 || cleanCaption.lines().size > 4
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text(
                        text = cleanCaption,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        maxLines = if (isExpandedText) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(enabled = isLongText) { isExpandedText = !isExpandedText }
                    )
                    if (isLongText && !isExpandedText) {
                        Text(
                            text = "Ver más",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable { isExpandedText = true }
                        )
                    }
                }
            } else if (!youtubeVideoId.isNullOrBlank()) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    YouTubePostCard(
                        videoId = youtubeVideoId,
                        originalText = cleanCaption
                    )
                }
            }

            // 2. Media Content
            val allMediaList = (post.mediaUrls ?: emptyList()).filter { it.isNotBlank() }
            val mediaImagesAndVideos = remember(allMediaList) {
                allMediaList.filter { !isAudioUrl(it) && !isDocumentUrl(it) }
            }
            val mediaDocuments = remember(allMediaList) {
                allMediaList.filter { isDocumentUrl(it) }
            }
            val voiceAudioUrl = remember(post.audioUrl, allMediaList) {
                post.audioUrl ?: allMediaList.firstOrNull { isAudioUrl(it) }
            }

            if (mediaImagesAndVideos.isNotEmpty()) {
                val pagerState = rememberPagerState(pageCount = { mediaImagesAndVideos.size })
                var isMuted by remember { mutableStateOf(true) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                        .background(Color.Black)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val url = mediaImagesAndVideos[page]
                        val resolvedUrl = remember(url) {
                            com.example.data.repository.CdnManager.resolveMediaUrlSync(url)
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { onMediaClick(mediaImagesAndVideos, page, voiceAudioUrl) }
                        ) {
                            if (post.type == "VIDEO" || post.type == "REEL" || isVideoUrl(resolvedUrl)) {
                                val videoUri = remember(resolvedUrl) { Uri.parse(resolvedUrl) }
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
                                    contentDescription = "Media",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            RenderOverlays(metadata.overlaysBase64)
                        }
                    }

                    // Video Controls
                    val currentUrl = mediaImagesAndVideos.getOrNull(pagerState.currentPage) ?: ""
                    if (post.type == "VIDEO" || post.type == "REEL" || isVideoUrl(currentUrl)) {
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

                    // Pager Indicators
                    if (mediaImagesAndVideos.size > 1) {
                        Text(
                            text = "${pagerState.currentPage + 1}/${mediaImagesAndVideos.size}",
                            color = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(mediaImagesAndVideos.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) Color(0xFF00FF85) else Color.White.copy(alpha = 0.5f)
                                val width = if (pagerState.currentPage == iteration) 18.dp else 6.dp
                                Box(
                                    modifier = Modifier
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .height(6.dp)
                                        .width(width)
                                )
                            }
                        }
                    }

                    // Audio Badge
                    if (voiceAudioUrl != null && post.type != "VIDEO" && post.type != "REEL") {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎵 Audio", color = Color(0xFF00FF85), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (voiceAudioUrl != null || post.type == "AUDIO") {
                val resolvedAudio = remember(voiceAudioUrl) {
                    com.example.data.repository.CdnManager.resolveMediaUrlSync(voiceAudioUrl ?: "")
                }
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                    PlaylistAudioPlayer(audioUrls = listOf(resolvedAudio))
                }
            } else if (post.type == "TEXT" && mediaImagesAndVideos.isEmpty() && youtubeVideoId.isNullOrBlank()) {
                // If it's just short text and was not expanded, we might want to make it look like a quote card
                if (cleanCaption.isNotBlank() && cleanCaption.length < 150) {
                     // The text is already shown in the Header section (1. Text Content).
                     // We don't need to do anything here unless we want to remove it from there and show it here instead.
                     // Since the prompt says "NO obligar a generar una imagen artificial. crear una presentación visual tipo quote/card."
                     // actually, the standard text display above is enough for Twitter-like text.
                     // Just add some bottom padding.
                     Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (mediaDocuments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mediaDocuments.forEach { docUrl ->
                        val resolvedDocUrl = remember(docUrl) {
                            com.example.data.repository.CdnManager.resolveMediaUrlSync(docUrl)
                        }
                        com.example.ui.components.chat.media.DocumentPreviewCard(
                            docUrl = resolvedDocUrl,
                            mediaSize = null,
                            bubbleColor = Color(0xFF2A2A30),
                            isSender = false,
                            senderAvatarUrl = null,
                            messageStatus = "sent",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action Bar (Social)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isLiked = post.isLikedByMe
                
                IconButton(onClick = { onLikeClick() }) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        tint = if (isLiked) Color(0xFFFF1744) else Color.White,
                        contentDescription = "Me gusta",
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                IconButton(onClick = { onCommentClick() }) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        tint = Color.White,
                        contentDescription = "Comentar",
                        modifier = Modifier.size(26.dp)
                    )
                }
                
                IconButton(
                    onClick = { 
                        onShareClick()
                        
                        // Opcional: Compartir nativo (Android Sharesheet)
                        val shareIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "¡Mira esta publicación en PanaLink!\n${post.content ?: ""}")
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir publicación"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        tint = Color.White,
                        contentDescription = "Compartir",
                        modifier = Modifier.size(26.dp)
                    )
                }

                if (post.type == "AUDIO" || voiceAudioUrl != null) {
                    IconButton(onClick = { onAudioPlaylistClick(post) }) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            tint = Color(0xFF00FF85),
                            contentDescription = "Reproducir lista",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { 
                        isSaved = !isSaved
                        val msg = if (isSaved) "Guardado" else "Eliminado de guardados"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.BookmarkBorder else Icons.Outlined.BookmarkBorder,
                        tint = if (isSaved) Color(0xFFD500F9) else Color.White,
                        contentDescription = "Guardar",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Counters
            val likesText = if (post.likesCount == 1) "1 Me gusta" else "${post.likesCount} Me gusta"
            val commentsText = if (post.commentsCount == 1) "1 comentario" else "${post.commentsCount} comentarios"
            val sharesText = if (post.sharesCount == 1) "1 vez compartido" else "${post.sharesCount} veces compartido"
            
            if (post.likesCount > 0 || post.commentsCount > 0 || post.sharesCount > 0) {
                val countersText = buildString {
                    val parts = mutableListOf<String>()
                    if (post.likesCount > 0) parts.add(likesText)
                    if (post.commentsCount > 0) parts.add(commentsText)
                    if (post.sharesCount > 0) parts.add(sharesText)
                    append(parts.joinToString(" · "))
                }
                
                Text(
                    text = countersText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
