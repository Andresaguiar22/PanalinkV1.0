package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Reusable Pull-to-Refresh container that wraps any scrollable list content (like LazyColumn)
 * and applies the custom PanaLink elastic list stretch behavior along with the Neon glowing indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanalinkPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberPullToRefreshState()

    // To implement the elastic stretching smoothly and prevent "jitter" on mobile devices,
    // we apply a dampened offset transition to the content container using graphicsLayer translationY.
    // We compute this from the state's distanceFraction and default threshold to keep it highly fluid.
    val density = LocalDensity.current
    val thresholdPx = remember(density) { with(density) { 80.dp.toPx() } }
    
    val elasticOffset = remember(state.distanceFraction, thresholdPx) {
        val rawOffset = state.distanceFraction * thresholdPx
        // Dampen the stretch effect dynamically to feel like a high-tension physical spring
        rawOffset * 0.55f
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier.fillMaxSize(),
        indicator = {
            PanalinkRefreshIndicator(
                state = state,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        // Content container with hardware-accelerated elastic stretch animation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = elasticOffset
                }
        ) {
            content()
        }
    }
}

/**
 * PanaLink custom glowing Neon Loader indicator.
 * Displays a central neon green circle with a golden outer border and a beautiful projected glow shadow.
 * Progresses from scale 0 to 1 as the user pulls down, and rotates continuously during refreshing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanalinkRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    // Current pull progress fraction
    val progress = state.distanceFraction

    // Infinite rotation animation triggered while refreshing
    val infiniteTransition = rememberInfiniteTransition(label = "NeonRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "NeonRotationAngle"
    )

    // Current angle: rotates continuously if refreshing, otherwise follows the drag progress
    val currentAngle = if (isRefreshing) rotationAngle else progress * 180f

    // Smooth exit animation (fade-out + scale-out) upon refresh completion
    val exitProgress by animateFloatAsState(
        targetValue = if (isRefreshing) 1f else 0f,
        animationSpec = if (isRefreshing) snap() else tween(350, easing = FastOutSlowInEasing),
        label = "NeonExitAnimation"
    )

    // Combine pull scale, active scale, and exit scale-out
    val finalScale = if (isRefreshing) {
        exitProgress
    } else {
        progress.coerceIn(0f, 1f)
    }

    val finalAlpha = if (isRefreshing) {
        exitProgress
    } else {
        (progress * 2f).coerceIn(0f, 1f)
    }

    if (finalScale > 0.01f) {
        Box(
            modifier = modifier
                .padding(top = 16.dp)
                .size(48.dp)
                .scale(finalScale)
                .alpha(finalAlpha)
                // Use custom drawBehind to paint the beautiful glowing Neon loader
                .drawBehind {
                    val width = size.width
                    val height = size.height
                    val center = Offset(width / 2f, height / 2f)
                    val radius = (width / 2f) - 8.dp.toPx()

                    // 1. Draw glowing background shadow (projected glow effect)
                    drawIntoCanvas { canvas ->
                        val paint = Paint().asFrameworkPaint().apply {
                            color = android.graphics.Color.parseColor("#00E676") // Neon Green
                            style = android.graphics.Paint.Style.FILL
                            maskFilter = BlurMaskFilter(12.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                        }
                        // Draw shadow circular glow behind the indicator
                        canvas.nativeCanvas.drawCircle(center.x, center.y, radius + 2.dp.toPx(), paint)
                    }

                    // 2. Draw outer Gold border (#FFD700)
                    drawCircle(
                        color = Color(0xFFFFD700),
                        radius = radius + 3.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // 3. Draw inner white-emerald core circular plate
                    drawCircle(
                        color = Color(0xFF111F1D),
                        radius = radius
                    )

                    // 4. Draw active Neon Green arc loader matching current drag/rotation state
                    val sweepAngle = if (isRefreshing) 270f else (progress * 300f).coerceAtMost(360f)
                    drawArc(
                        color = Color(0xFF00E676),
                        startAngle = currentAngle - 90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // A tiny elegant star or simple center point inside the neon loader
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color(0xFF00E676), CircleShape)
            )
        }
    }
}
