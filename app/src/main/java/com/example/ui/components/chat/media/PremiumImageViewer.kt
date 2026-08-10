package com.example.ui.components.chat.media

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import com.example.ui.components.chat.media.loading.MediaLoadingState
import com.example.ui.components.chat.media.loading.PremiumMediaLoadingOverlay

@Composable
fun PremiumImageViewer(
    imageUrl: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var loadingState by remember { mutableStateOf<MediaLoadingState>(MediaLoadingState.Loading) }
    var retryCount by remember { mutableIntStateOf(0) }

    val animatedScale by animateFloatAsState(targetValue = scale, label = "scale")

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2f
                        }
                    }
                )
            }
    ) {
        AsyncImage(
            model = if (retryCount == 0) imageUrl else "$imageUrl?retry=$retryCount",
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit,
            onState = { state ->
                loadingState = when (state) {
                    is AsyncImagePainter.State.Loading -> MediaLoadingState.Loading
                    is AsyncImagePainter.State.Success -> MediaLoadingState.Success
                    is AsyncImagePainter.State.Error -> MediaLoadingState.Error()
                    else -> MediaLoadingState.Idle
                }
            }
        )

        PremiumMediaLoadingOverlay(
            state = loadingState,
            onRetry = {
                retryCount++
                loadingState = MediaLoadingState.Retrying
            }
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
        }
    }
}
