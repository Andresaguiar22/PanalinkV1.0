package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import java.io.File

import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.identity.model.AvatarDownloadResult
import com.example.identity.model.toIdentityUiState

@Composable
fun PanaAvatar(
    avatarUrl: String? = null,
    userId: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    borderWidth: Dp = 1.dp,
    borderColor: Color = Color.White,
    contentDescription: String? = "Avatar",
    placeholderName: String? = "Pana"
) {
    var finalAvatarUrl = avatarUrl
    var finalPlaceholder = placeholderName
    var finalAvatarLocalPath: String? = null

    if (!userId.isNullOrEmpty()) {
        val context = LocalContext.current
        val bridge = remember { 
            com.example.identity.bridge.LegacyIdentityBridge(context) 
        }
        val initialCached = remember(userId) { com.example.identity.memory.IdentityMemoryCache.profiles.get(userId) }
        val identityUiState by bridge.identityRepository.observeIdentity(userId).collectAsStateWithLifecycle(initialValue = initialCached?.toIdentityUiState())
        
        identityUiState?.let {
            if (finalAvatarUrl == null) finalAvatarUrl = it.avatarUrl
            finalAvatarLocalPath = it.avatarLocalPath
            if (finalPlaceholder == "Pana" || finalPlaceholder == null) {
                finalPlaceholder = it.displayName
            }
        }
        
        val scope = androidx.compose.runtime.rememberCoroutineScope()
        LaunchedEffect(finalAvatarUrl, finalAvatarLocalPath) {
            if (finalAvatarLocalPath.isNullOrBlank() || !File(finalAvatarLocalPath!!).exists()) {
                val rawUrl = finalAvatarUrl?.takeIf { 
                    it.isNotBlank() && 
                    it != "null" && 
                    !it.startsWith("preset:") && 
                    !it.contains("unsplash.com") 
                }
                if (!rawUrl.isNullOrBlank()) {
                    val resolvedUrl = com.example.data.repository.CdnManager.resolveAvatarUrl(rawUrl)
                    if (resolvedUrl != null) {
                        scope.launch(Dispatchers.IO) {
                            val storageManager = com.example.identity.storage.AvatarStorageManager(context)
                            val result = storageManager.downloadAvatar(userId, resolvedUrl)
                            if (result is com.example.identity.model.AvatarDownloadResult.Success) {
                                val cached = bridge.identityRepository.getProfile(userId)
                                if (cached != null) {
                                    bridge.identityRepository.saveProfile(cached.copy(avatarLocalPath = result.localPath))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val isPreset = finalAvatarUrl?.startsWith("preset:") == true
    val cleanUrlModel = remember(finalAvatarUrl, finalAvatarLocalPath) {
        if (!finalAvatarLocalPath.isNullOrBlank() && File(finalAvatarLocalPath!!).exists()) {
            File(finalAvatarLocalPath!!)
        } else {
            val raw = finalAvatarUrl?.takeIf { 
                it.isNotBlank() && 
                it != "null" && 
                !it.contains("unsplash.com") 
            }
            if (!raw.isNullOrBlank()) {
                com.example.data.repository.CdnManager.resolveAvatarUrl(raw)
            } else {
                null
            }
        }
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isPreset) {
            val emoji = finalAvatarUrl?.removePrefix("preset:") ?: "👤"
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFFB026FF), Color(0xFF00FF85))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = (size.value * 0.5f).sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else if (cleanUrlModel != null) {
            val painter = coil.compose.rememberAsyncImagePainter(
                model = coil.request.ImageRequest.Builder(LocalContext.current)
                    .data(cleanUrlModel)
                    .crossfade(false)
                    .build()
            )
            
            Box(modifier = Modifier.fillMaxSize()) {
                if (painter.state !is coil.compose.AsyncImagePainter.State.Success) {
                    InitialsAvatar(placeholderName = finalPlaceholder, size = size)
                }
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            InitialsAvatar(placeholderName = finalPlaceholder, size = size)
        }
    }
}

@Composable
private fun InitialsAvatar(
    placeholderName: String?,
    size: Dp
) {
    val cleanName = placeholderName?.trim()?.ifBlank { "Pana" } ?: ""
    val words = cleanName.split(" ").filter { it.isNotBlank() }
    val initials = when {
        words.size >= 2 -> "${words[0].first().uppercaseChar()}${words[1].first().uppercaseChar()}"
        words.size == 1 -> "${words[0].first().uppercaseChar()}"
        else -> "P"
    }

    val colorPairs = listOf(
        listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)),
        listOf(Color(0xFF00c6ff), Color(0xFF0072ff)),
        listOf(Color(0xFFf857a6), Color(0xFFff5858)),
        listOf(Color(0xFF11998e), Color(0xFF38ef7d)),
        listOf(Color(0xFFFF8008), Color(0xFFFFC837)),
        listOf(Color(0xFFD500F9), Color(0xFF651FFF))
    )
    val pairIndex = kotlin.math.abs(cleanName.hashCode()) % colorPairs.size
    val gradientColors = colorPairs[pairIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
            maxLines = 1
        )
    }
}
