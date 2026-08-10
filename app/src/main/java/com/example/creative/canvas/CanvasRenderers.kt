package com.example.creative.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.creative.core.CreativeLayer
import java.io.File

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TextLayerRenderer(
    layer: CreativeLayer.Text,
    isSelected: Boolean,
    onLayerTransformed: (xFraction: Float, yFraction: Float, scale: Float, rotation: Float) -> Unit,
    onClick: () -> Unit,
    onDoubleTap: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    var offsetX by remember { mutableFloatStateOf(layer.xFraction) }
    var offsetY by remember { mutableFloatStateOf(layer.yFraction) }
    var currentScale by remember { mutableFloatStateOf(layer.scale) }
    var currentRotation by remember { mutableFloatStateOf(layer.rotation) }

    val parseColor = try {
        Color(android.graphics.Color.parseColor(layer.colorHex))
    } catch (e: Exception) {
        Color.White
    }

    val bgColor = layer.backgroundColorHex?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
            .scale(currentScale)
            .rotate(currentRotation)
            .graphicsLayer(alpha = layer.opacity)
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleTap,
                onLongClick = onLongPress
            )
            .pointerInput(layer.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (layer.isLocked) return@detectTransformGestures
                    offsetX += pan.x / density
                    offsetY += pan.y / density
                    currentScale = (currentScale * zoom).coerceIn(0.5f, 4.0f)
                    currentRotation += rotation
                    onLayerTransformed(offsetX, offsetY, currentScale, currentRotation)
                }
            }
            .then(
                if (isSelected) Modifier.border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                else Modifier
            )
            .then(
                if (bgColor != null) Modifier.background(bgColor, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)
                else Modifier.padding(4.dp)
            )
    ) {
        Text(
            text = layer.text,
            color = parseColor,
            fontSize = layer.fontSizeSp.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StickerLayerRenderer(
    layer: CreativeLayer.Sticker,
    isSelected: Boolean,
    onLayerTransformed: (xFraction: Float, yFraction: Float, scale: Float, rotation: Float) -> Unit,
    onClick: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    var offsetX by remember { mutableFloatStateOf(layer.xFraction) }
    var offsetY by remember { mutableFloatStateOf(layer.yFraction) }
    var currentScale by remember { mutableFloatStateOf(layer.scale) }
    var currentRotation by remember { mutableFloatStateOf(layer.rotation) }

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
            .scale(currentScale)
            .rotate(currentRotation)
            .graphicsLayer(alpha = layer.opacity)
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleTap,
                onLongClick = onLongPress
            )
            .pointerInput(layer.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (layer.isLocked) return@detectTransformGestures
                    offsetX += pan.x / density
                    offsetY += pan.y / density
                    currentScale = (currentScale * zoom).coerceIn(0.4f, 3.5f)
                    currentRotation += rotation
                    onLayerTransformed(offsetX, offsetY, currentScale, currentRotation)
                }
            }
            .then(
                if (isSelected) Modifier.border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(4.dp))
                else Modifier
            )
    ) {
        val model: Any = if (layer.stickerUrlOrPath.startsWith("/")) {
            File(layer.stickerUrlOrPath)
        } else {
            layer.stickerUrlOrPath
        }

        AsyncImage(
            model = model,
            contentDescription = "Sticker",
            modifier = Modifier.size(120.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InteractiveLayerRenderer(
    layer: CreativeLayer.Interactive,
    isSelected: Boolean,
    onLayerTransformed: (xFraction: Float, yFraction: Float, scale: Float, rotation: Float) -> Unit,
    onClick: () -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onLongPress: () -> Unit = {}
) {
    var offsetX by remember { mutableFloatStateOf(layer.xFraction) }
    var offsetY by remember { mutableFloatStateOf(layer.yFraction) }
    var currentScale by remember { mutableFloatStateOf(layer.scale) }
    var currentRotation by remember { mutableFloatStateOf(layer.rotation) }

    Box(
        modifier = Modifier
            .offset(x = offsetX.dp, y = offsetY.dp)
            .scale(currentScale)
            .rotate(currentRotation)
            .graphicsLayer(alpha = layer.opacity)
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onDoubleTap,
                onLongClick = onLongPress
            )
            .pointerInput(layer.id) {
                detectTransformGestures { _, pan, zoom, rotation ->
                    if (layer.isLocked) return@detectTransformGestures
                    offsetX += pan.x / density
                    offsetY += pan.y / density
                    currentScale = (currentScale * zoom).coerceIn(0.5f, 3.0f)
                    currentRotation += rotation
                    onLayerTransformed(offsetX, offsetY, currentScale, currentRotation)
                }
            }
            .then(
                if (isSelected) Modifier.border(1.5.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                else Modifier
            )
            .background(Color(0xEE1F1F2C), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF3F3F52), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = layer.interactiveType.uppercase(),
                color = Color(0xFF00E5FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            if (layer.title.isNotEmpty()) {
                Text(
                    text = layer.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (layer.optionA.isNotEmpty() || layer.optionB.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (layer.optionA.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2D2D3E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(layer.optionA, color = Color.White, fontSize = 12.sp)
                        }
                    }
                    if (layer.optionB.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF2D2D3E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(layer.optionB, color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (layer.extraData.isNotEmpty()) {
                Text(
                    text = layer.extraData,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DrawingRenderer(
    drawings: List<CreativeLayer.Drawing>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        for (drawing in drawings) {
            if (drawing.points.size < 2) continue
            val strokeColor = try {
                Color(android.graphics.Color.parseColor(drawing.strokeColorHex))
            } catch (e: Exception) {
                Color.Red
            }

            val path = Path().apply {
                val first = drawing.points.first()
                moveTo(first.first * size.width, first.second * size.height)
                for (i in 1 until drawing.points.size) {
                    val pt = drawing.points[i]
                    lineTo(pt.first * size.width, pt.second * size.height)
                }
            }

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(
                    width = drawing.strokeWidthDp * density,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun FilterRenderer(
    filterName: String,
    modifier: Modifier = Modifier
) {
    val overlayColor = when (filterName.lowercase()) {
        "cinematic" -> Color(0x3300E5FF)
        "vintage" -> Color(0x33FFA726)
        "neon" -> Color(0x33E91E63)
        "warm" -> Color(0x22FF7043)
        "black_white" -> Color(0x66000000)
        else -> Color.Transparent
    }

    if (overlayColor != Color.Transparent) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(overlayColor)
        )
    }
}
