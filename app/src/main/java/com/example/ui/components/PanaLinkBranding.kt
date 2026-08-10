package com.example.ui.components

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.geometry.center
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// The beautiful colorful background gradient from the screenshot
val PanaBackgroundBrush = Brush.linearGradient(
    colors = listOf(
        Color(0xFFE2E75E), // Top-left yellow-green (lime)
        Color(0xFF00E5FF), // Teal/cyan
        Color(0xFF00B0FF), // Soft blue
        Color(0xFFEC407A)  // Bottom-right pink
    )
)

@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "Aurora")

    // Animate coordinates to move gradient anchors smoothly to simulate Boreal Aurora
    val animOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )

    val animOffset2 by infiniteTransition.animateFloat(
        initialValue = 1000f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(26000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    val waveHeight1 by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 120f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )

    val waveHeight2 by infiniteTransition.animateFloat(
        initialValue = 120f,
        targetValue = -80f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width
                val height = size.height

                // Draw base dark midnight-teal space canvas
                drawRect(color = Color(0xFF030708))

                // 1. Draw Boreal Neon Green-Teal Aurora wave
                val brushGreen = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00FF85).copy(alpha = 0.22f), // Glowing Neon Green
                        Color(0xFF00E5FF).copy(alpha = 0.10f), // Glowing Cyan
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        x = width * 0.25f + (animOffset1 * 0.15f),
                        y = height * 0.22f + waveHeight1
                    ),
                    radius = width * 1.2f
                )
                drawRect(brush = brushGreen)

                // 2. Draw Boreal Violet-Pink Aurora wave
                val brushPink = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFEC407A).copy(alpha = 0.18f), // Soft Boreal Magenta
                        Color(0xFF7C4DFF).copy(alpha = 0.08f), // Deep Violet
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(
                        x = width * 0.75f - (animOffset2 * 0.12f),
                        y = height * 0.45f + waveHeight2
                    ),
                    radius = width * 1.3f
                )
                drawRect(brush = brushPink)

                // 3. Draw a subtle lime-yellow horizontal sweeping mist
                val brushLime = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFE2E75E).copy(alpha = 0.05f), // Lime glow
                        Color.Transparent
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, height * 0.35f + waveHeight1 * 0.6f),
                    end = androidx.compose.ui.geometry.Offset(width, height * 0.55f + waveHeight2 * 0.6f)
                )
                drawRect(brush = brushLime)
            }
    ) {
        content()
    }
}

@Composable
fun PanaLinkLogo(modifier: Modifier = Modifier, logoSize: Dp = 100.dp) {
    Box(
        modifier = modifier
            .size(logoSize)
            .clip(RoundedCornerShape(logoSize * 0.28f))
            .background(Color.Black)
            .drawBehind {
                val width = this.size.width
                val height = this.size.height
                val cornerRadius = width * 0.28f
                
                // 1. Draw rich radial slate/black gradient background
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF24282D), Color(0xFF121315), Color(0xFF070809)),
                        center = this.size.center,
                        radius = width * 0.75f
                    )
                )
                
                // 2. Draw outer metallic bezel stroke (with linear silver gradient)
                drawIntoCanvas { canvas ->
                    val borderPaint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = (width * 0.035f).coerceAtLeast(1.5f)
                        shader = android.graphics.LinearGradient(
                            0f, 0f, width, height,
                            intArrayOf(
                                android.graphics.Color.parseColor("#FFFFFF"),
                                android.graphics.Color.parseColor("#4F5357"),
                                android.graphics.Color.parseColor("#B0B5BC"),
                                android.graphics.Color.parseColor("#E1E4E6"),
                                android.graphics.Color.parseColor("#7F848C")
                            ),
                            null,
                            android.graphics.Shader.TileMode.CLAMP
                        )
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        borderPaint.strokeWidth / 2f,
                        borderPaint.strokeWidth / 2f,
                        width - borderPaint.strokeWidth / 2f,
                        height - borderPaint.strokeWidth / 2f,
                        cornerRadius,
                        cornerRadius,
                        borderPaint
                    )
                }
            }
            .padding(logoSize * 0.04f),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            
            val scaleX = w / 108f
            val scaleY = h / 108f
            val avgScale = (scaleX + scaleY) / 2f
            
            // Build the Letter P Path precisely
            val pPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(38f * scaleX, 76f * scaleY)
                lineTo(38f * scaleX, 34f * scaleY)
                // Curve to (46, 26)
                quadraticTo(38f * scaleX, 26f * scaleY, 46f * scaleX, 26f * scaleY)
                lineTo(62f * scaleX, 26f * scaleY)
                // Curve to (70, 34)
                quadraticTo(70f * scaleX, 26f * scaleY, 70f * scaleX, 34f * scaleY)
                lineTo(70f * scaleX, 50f * scaleY)
                // Curve to (62, 58)
                quadraticTo(70f * scaleX, 58f * scaleY, 62f * scaleX, 58f * scaleY)
                lineTo(38f * scaleX, 58f * scaleY)
            }
            
            // Build the Safe Neon Heart Path precisely
            val heartPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(79f * scaleX, 82f * scaleY)
                cubicTo(
                    75f * scaleX, 79f * scaleY,
                    71f * scaleX, 76f * scaleY,
                    71f * scaleX, 73f * scaleY
                )
                cubicTo(
                    71f * scaleX, 70f * scaleY,
                    74f * scaleX, 67f * scaleY,
                    78f * scaleX, 70f * scaleY
                )
                lineTo(79f * scaleX, 71f * scaleY)
                lineTo(80f * scaleX, 70f * scaleY)
                cubicTo(
                    84f * scaleX, 67f * scaleY,
                    87f * scaleX, 70f * scaleY,
                    87f * scaleX, 73f * scaleY
                )
                cubicTo(
                    87f * scaleX, 76f * scaleY,
                    83f * scaleX, 79f * scaleY,
                    79f * scaleX, 82f * scaleY
                )
            }
            
            drawIntoCanvas { canvas ->
                val nativeCanvas = canvas.nativeCanvas
                val androidPPath = pPath.asAndroidPath()
                val androidHeartPath = heartPath.asAndroidPath()
                
                // --- 1. Draw P Metallic Bevel Outline ---
                val metallicPaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 14f * avgScale
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    shader = android.graphics.LinearGradient(
                        25f * scaleX, 20f * scaleY, 85f * scaleX, 85f * scaleY,
                        intArrayOf(
                            android.graphics.Color.parseColor("#FFFFFF"),
                            android.graphics.Color.parseColor("#B0B5BC"),
                            android.graphics.Color.parseColor("#7F848C"),
                            android.graphics.Color.parseColor("#E1E4E6"),
                            android.graphics.Color.parseColor("#9AA0A6"),
                            android.graphics.Color.parseColor("#4F5357")
                        ),
                        floatArrayOf(0.0f, 0.2f, 0.4f, 0.6f, 0.8f, 1.0f),
                        android.graphics.Shader.TileMode.CLAMP
                    )
                }
                nativeCanvas.drawPath(androidPPath, metallicPaint)
                
                // --- 2. Draw P Groove Inset ---
                val groovePaint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 10f * avgScale
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    color = android.graphics.Color.parseColor("#141619")
                }
                nativeCanvas.drawPath(androidPPath, groovePaint)
                
                // --- 3. Draw Neon Green Glow of P ---
                // Outer ambient glow
                val pOuterGlow = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 12f * avgScale
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    color = android.graphics.Color.parseColor("#00FF85")
                    alpha = 55 // ~0.22 opacity
                    maskFilter = BlurMaskFilter((6f * avgScale).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
                }
                nativeCanvas.drawPath(androidPPath, pOuterGlow)
                
                // Inner glow
                val pInnerGlow = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 6f * avgScale
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    color = android.graphics.Color.parseColor("#00FF85")
                    alpha = 180 // ~0.70 opacity
                    maskFilter = BlurMaskFilter((2f * avgScale).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
                }
                nativeCanvas.drawPath(androidPPath, pInnerGlow)
                
                // Pure core light tube
                val pCore = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 2.5f * avgScale
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    color = android.graphics.Color.WHITE
                }
                nativeCanvas.drawPath(androidPPath, pCore)
                
                // --- 4. Draw Neon Green Heart ---
                // Outer glow
                val heartOuterGlow = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.FILL_AND_STROKE
                    strokeWidth = 4f * avgScale
                    color = android.graphics.Color.parseColor("#00FF85")
                    alpha = 70 // ~0.27 opacity
                    maskFilter = BlurMaskFilter((4f * avgScale).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
                }
                nativeCanvas.drawPath(androidHeartPath, heartOuterGlow)
                
                // Inner glow / main color
                val heartMain = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.FILL_AND_STROKE
                    strokeWidth = 1.5f * avgScale
                    color = android.graphics.Color.parseColor("#00FF85")
                    alpha = 220
                    maskFilter = BlurMaskFilter((1f * avgScale).coerceAtLeast(1f), BlurMaskFilter.Blur.NORMAL)
                }
                nativeCanvas.drawPath(androidHeartPath, heartMain)
                
                // Core white neon line
                val heartCore = android.graphics.Paint().apply {
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 1.5f * avgScale
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    color = android.graphics.Color.WHITE
                }
                nativeCanvas.drawPath(androidHeartPath, heartCore)
            }
        }
    }
}


@Composable
fun SmallPanaLinkLogo(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    PanaLinkLogo(modifier = modifier, logoSize = size)
}

/**
 * Custom pulsating and glowing PanaLink logo container.
 * Constrained to a small size suitable for top bars, it features a gentle scale pulsation
 * accompanied by custom drawing behind of very soft blinking neon-green and gold borders.
 */
@Composable
fun PulsatingPanaLinkLogo(modifier: Modifier = Modifier, size: Dp = 24.dp) {
    val infiniteTransition = rememberInfiniteTransition(label = "LogoPulse")
    
    // Extremely soft pulsating scale (between 94% and 106% of its size)
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScaleAnim"
    )
    
    // Very gentle glowing brightness/opacity pulsation (between 40% and 100%)
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlphaAnim"
    )

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .drawBehind {
                val width = this.size.width
                val height = this.size.height
                val cornerRadius = width * 0.28f
                
                drawIntoCanvas { canvas ->
                    // 1. Draw a soft glowing neon-green outer border/halo
                    val greenPaint = Paint().asFrameworkPaint().apply {
                        color = android.graphics.Color.parseColor("#00FF85") // Neon green
                        alpha = (glowAlpha * 255).toInt().coerceIn(0, 255)
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 2.dp.toPx()
                        maskFilter = BlurMaskFilter(4.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        -1.dp.toPx(), -1.dp.toPx(), width + 1.dp.toPx(), height + 1.dp.toPx(),
                        cornerRadius, cornerRadius,
                        greenPaint
                    )
                    
                    // 2. Draw a soft gold inner warm glow that blinks out of phase
                    val goldPaint = Paint().asFrameworkPaint().apply {
                        color = android.graphics.Color.parseColor("#FFD700") // Gold
                        alpha = ((1.2f - glowAlpha).coerceIn(0f, 1f) * 0.4f * 255).toInt()
                        style = android.graphics.Paint.Style.STROKE
                        strokeWidth = 1.5.dp.toPx()
                        maskFilter = BlurMaskFilter(2.5.dp.toPx(), BlurMaskFilter.Blur.NORMAL)
                    }
                    canvas.nativeCanvas.drawRoundRect(
                        0f, 0f, width, height,
                        cornerRadius, cornerRadius,
                        goldPaint
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Render the tiny logo in the center
        PanaLinkLogo(logoSize = size)
    }
}

@Composable
fun PanaTopBarTitle(sectionName: String, primaryColor: Color = Color(0xFF00FF85)) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Clean layout: No text application name, just the miniature glowing pulsating logo and the section name
        PulsatingPanaLinkLogo(size = 24.dp)
        Text(
            text = sectionName,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = primaryColor,
            letterSpacing = 0.2.sp
        )
    }
}
