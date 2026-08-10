package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppColors
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class FabSubItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val containerColor: Color? = null,
    val testTag: String = ""
)

@Composable
fun ContextualExpandableFab(
    mainIcon: ImageVector = Icons.Default.Add,
    subItems: List<FabSubItem>,
    modifier: Modifier = Modifier,
    testTag: String = "contextual_fab",
    onShakeStateChanged: ((Boolean) -> Unit)? = null
) {
    if (subItems.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val colors = LocalAppColors.current
    val coroutineScope = rememberCoroutineScope()

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 135f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "FabRotation"
    )

    // Continuously rotating neon border glow animations (slow, 6s)
    val infiniteTransition = rememberInfiniteTransition(label = "neon_glow")
    
    val rotationGlowAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotationGlow"
    )

    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )


    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Overlay barrier when expanded to dismiss by clicking outside
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isExpanded = false
                    }
            )
        }

        // We use a Box with the main FAB at the center and sub-items fanning out from the center
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp, end = 16.dp)
                .size(240.dp), // Area for the radial menu expansion
            contentAlignment = Alignment.BottomEnd
        ) {
            val numItems = subItems.size
            
            subItems.forEachIndexed { index, item ->
                val progress by animateFloatAsState(
                    targetValue = if (isExpanded) 1f else 0f,
                    animationSpec = spring(
                        dampingRatio = 0.7f,
                        stiffness = 150f - (index * 20f) // stagger effect
                    ),
                    label = "itemProgress_$index"
                )

                // Calculate radial angle between 100° (almost straight up) and 170° (almost left)
                val angleRad = if (numItems > 1) {
                    val startAngle = 105f
                    val endAngle = 165f
                    val step = (endAngle - startAngle) / (numItems - 1)
                    (startAngle + step * index) * (Math.PI / 180f)
                } else {
                    135f * (Math.PI / 180f)
                }

                // 115 dp radius
                val radius = 115f
                val xOffsetDp = (radius * Math.cos(angleRad) * progress).toFloat()
                val yOffsetDp = -(radius * Math.sin(angleRad) * progress).toFloat() // Negative Y moves UPWARDS in Android coordinates

                // Animated properties for scale, opacity, etc.
                val itemScale = 0.4f + (0.6f * progress)
                val itemAlpha = progress

                if (isExpanded || progress > 0.01f) {
                    Box(
                        modifier = Modifier
                            .offset(x = xOffsetDp.dp, y = yOffsetDp.dp)
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                                alpha = itemAlpha
                            }
                            .size(52.dp)
                            // Bright glowing neon border
                            .border(1.5.dp, colors.accent, CircleShape)
                            .background(Color.Black, CircleShape)
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                isExpanded = false
                                item.onClick()
                            }
                            .testTag(item.testTag.ifEmpty { "sub_fab_${item.label}" }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Optional subtle floating text tooltip above the item
                    if (isExpanded && progress > 0.8f) {
                        Box(
                            modifier = Modifier
                                .offset(x = xOffsetDp.dp, y = (yOffsetDp - 38f).dp)
                                .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                                .border(0.5.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = item.label,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Main Rotating Floating Action Button with rotating neon rainbow border
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
            ) {
                // Pulsating glow background shadow (made rounded square to match)
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(glowScale)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    colors.accent.copy(alpha = glowAlpha),
                                    colors.primary.copy(alpha = glowAlpha * 0.4f),
                                    Color.Transparent
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                )

                // Rotating Rainbow border outline (using RoundedCornerShape for a square look)
                Box(
                    modifier = Modifier
                        .size(59.dp)
                        .graphicsLayer { rotationZ = rotationGlowAngle }
                        .border(
                            width = 2.5.dp,
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFFF0000), // Red
                                    Color(0xFFFF7F00), // Orange
                                    Color(0xFFFFD700), // Yellow/Gold
                                    Color(0xFF00FF85), // Neon Green
                                    Color(0xFF00E5FF), // Cyan
                                    Color(0xFF8B00FF), // Violet
                                    Color(0xFFFF0000)  // Red
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                )

                // Main FAB Rounded Square (Gradient background as requested, matching the image)
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF8B00FF), // Violet / Purple
                                    Color(0xFF00E5FF), // Blue / Cyan
                                    Color(0xFF00FF85)  // Neon Green / Mint
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .pointerInput(onShakeStateChanged) {
                            detectTapGestures(
                                onTap = {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                    isExpanded = !isExpanded
                                },
                                onPress = { offset ->
                                    var isHeldFor5s = false
                                    val timerJob = coroutineScope.launch {
                                        delay(5000)
                                        isHeldFor5s = true
                                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                        onShakeStateChanged?.invoke(true)
                                    }
                                    try {
                                        tryAwaitRelease()
                                    } finally {
                                        timerJob.cancel()
                                        if (isHeldFor5s) {
                                            onShakeStateChanged?.invoke(false)
                                        }
                                    }
                                }
                            )
                        }
                        .testTag(testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = mainIcon,
                        contentDescription = "Expandir Acciones",
                        modifier = Modifier
                            .size(26.dp)
                            .rotate(rotationAngle),
                        tint = Color.Black // Black icon in the center of the gradient as shown in the image
                    )
                }
            }
        }
    }
}
