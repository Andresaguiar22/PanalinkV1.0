package com.example.ui.components

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.panatv.PanaTVActivity
import com.example.util.AppFloatingPlayerManager
import kotlin.math.roundToInt

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun FloatingPlayerBubble(
    onNavigateToReels: (String) -> Unit
) {
    val manager = AppFloatingPlayerManager
    val isFloating = manager.isFloating
    val player = manager.exoPlayer
    val context = LocalContext.current

    if (!isFloating || player == null) return

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    // Floating Bubble Dimensions
    val bubbleWidthDp = 110.dp
    val bubbleHeightDp = 180.dp
    val bubbleWidthPx = with(density) { bubbleWidthDp.toPx() }
    val bubbleHeightPx = with(density) { bubbleHeightDp.toPx() }

    // Coordinates of bubble (initially bottom-right)
    var offsetX by remember { mutableStateOf(screenWidthPx - bubbleWidthPx - 40f) }
    var offsetY by remember { mutableStateOf(screenHeightPx - bubbleHeightPx - 250f) }

    // Close zone dimensions and location (Bottom Center)
    val closeZoneSizeDp = 64.dp
    val closeZoneSizePx = with(density) { closeZoneSizeDp.toPx() }
    val closeZoneX = screenWidthPx / 2f
    val closeZoneY = screenHeightPx - with(density) { 100.dp.toPx() }

    var isDragging by remember { mutableStateOf(false) }

    // Calculate distance between bubble center and close zone
    val bubbleCenterX = offsetX + (bubbleWidthPx / 2f)
    val bubbleCenterY = offsetY + (bubbleHeightPx / 2f)
    
    val dx = bubbleCenterX - closeZoneX
    val dy = bubbleCenterY - closeZoneY
    val distance = kotlin.math.sqrt(dx * dx + dy * dy)
    val isNearCloseZone = distance < 250f // ~80dp radius trigger

    // Animated scale for close target and bubble
    val closeZoneScale by animateFloatAsState(
        targetValue = if (isNearCloseZone && isDragging) 1.5f else 1.0f,
        animationSpec = spring(),
        label = "CloseZoneScale"
    )

    val bubbleScale by animateFloatAsState(
        targetValue = if (isNearCloseZone && isDragging) 0.8f else 1.0f,
        animationSpec = spring(),
        label = "BubbleScale"
    )

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Red Close Target Zone at the Bottom
        AnimatedVisibility(
            visible = isDragging,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(closeZoneSizeDp)
                    .shadow(
                        elevation = if (isNearCloseZone) 12.dp else 4.dp,
                        shape = CircleShape,
                        ambientColor = Color.Red,
                        spotColor = Color.Red
                    )
                    .background(
                        color = if (isNearCloseZone) Color(0xFFFF2D55) else Color.Black.copy(alpha = 0.75f),
                        shape = CircleShape
                    )
                    .pointerInput(Unit) {} // Consume touch
                    .wrapContentSize(Alignment.Center)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar reproductor",
                    tint = Color.White,
                    modifier = Modifier
                        .size(32.dp)
                        .graphicsLayer {
                            scaleX = closeZoneScale
                            scaleY = closeZoneScale
                        }
                )
            }
        }

        // Floating Player Bubble
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .width(bubbleWidthDp)
                .height(bubbleHeightDp)
                .graphicsLayer {
                    scaleX = bubbleScale
                    scaleY = bubbleScale
                }
                .shadow(16.dp, shape = RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            
                            val currentBubbleCenterX = offsetX + (bubbleWidthPx / 2f)
                            val currentBubbleCenterY = offsetY + (bubbleHeightPx / 2f)
                            val currentDx = currentBubbleCenterX - closeZoneX
                            val currentDy = currentBubbleCenterY - closeZoneY
                            val currentDistance = kotlin.math.sqrt(currentDx * currentDx + currentDy * currentDy)
                            val currentIsNearCloseZone = currentDistance < 250f

                            if (currentIsNearCloseZone) {
                                manager.releasePlayer()
                            } else {
                                // Snap bubble to nearest side of the screen
                                val snapLeft = 40f
                                val snapRight = screenWidthPx - bubbleWidthPx - 40f
                                val midScreen = screenWidthPx / 2f
                                offsetX = if (currentBubbleCenterX < midScreen) snapLeft else snapRight
                                
                                // Ensure vertical bounds are respected
                                offsetY = offsetY.coerceIn(
                                    100f,
                                    screenHeightPx - bubbleHeightPx - 100f
                                )
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    )
                }
                .clickable {
                    // Click to maximize / return to original video screen
                    val activeId = manager.activeId
                    val activeType = manager.activeType
                    manager.isFloating = false

                    if (activeType == "reel" && activeId != null) {
                        onNavigateToReels(activeId)
                    } else if (activeType == "panatv") {
                        val intent = Intent(context, PanaTVActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(intent)
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00FF85).copy(alpha = 0.8f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // The Video Player view
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            this.player = player
                        }
                    },
                    update = { playerView ->
                        playerView.player = player
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Glassmorphic title header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF00FF85),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (manager.activeType == "reel") "Reel" else "PanaTV",
                            color = Color.White,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
