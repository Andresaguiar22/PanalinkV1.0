package com.example.creative.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CanvasEditorEngine(
    project: CreativeProject,
    selectedLayerId: String?,
    selectedLayerIds: Set<String> = emptySet(),
    isDrawingMode: Boolean,
    strokeColorHex: String,
    strokeWidthDp: Float,
    activeFilterName: String,
    currentVideoTimeMs: Long = 0L,
    onProjectUpdated: (CreativeProject) -> Unit,
    onLayerSelected: (String?) -> Unit,
    onLayerDoubleTap: (CreativeLayer) -> Unit = {},
    onLayerLongPress: (CreativeLayer) -> Unit = {},
    modifier: Modifier = Modifier,
    backgroundContent: @Composable () -> Unit
) {
    var currentDrawingPoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    val haptic = LocalHapticFeedback.current

    // Snap Guides state
    var showVerticalSnapGuide by remember { mutableStateOf(false) }
    var showHorizontalSnapGuide by remember { mutableStateOf(false) }

    // Smart Rules HUD state (X, Y, Scale, Rotation)
    var hudText by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Base Content (Video / Image preview)
        backgroundContent()

        // 2. Active Filter Overlay
        FilterRenderer(filterName = activeFilterName)

        // 3. Render Drawings (Filter out hidden layers or outside time window)
        val visibleDrawings = project.layers
            .filterIsInstance<CreativeLayer.Drawing>()
            .filter { it.isVisible && currentVideoTimeMs >= it.startOffsetMs && currentVideoTimeMs <= (it.startOffsetMs + it.durationMs) }
        DrawingRenderer(drawings = visibleDrawings)

        // 4. Interactive Drawing Canvas (When in drawing mode)
        if (isDrawingMode) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(strokeColorHex, strokeWidthDp) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentDrawingPoints = listOf(
                                    Pair(offset.x / size.width.toFloat(), offset.y / size.height.toFloat())
                                )
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val pt = Pair(
                                    change.position.x / size.width.toFloat(),
                                    change.position.y / size.height.toFloat()
                                )
                                currentDrawingPoints = currentDrawingPoints + pt
                            },
                            onDragEnd = {
                                if (currentDrawingPoints.size > 1) {
                                    val newDrawingLayer = CreativeLayer.Drawing(
                                        id = "draw_${System.currentTimeMillis()}",
                                        strokeColorHex = strokeColorHex,
                                        strokeWidthDp = strokeWidthDp,
                                        points = currentDrawingPoints
                                    )
                                    onProjectUpdated(
                                        project.copy(layers = project.layers + newDrawingLayer)
                                    )
                                }
                                currentDrawingPoints = emptyList()
                            }
                        )
                    }
            ) {
                if (currentDrawingPoints.size >= 2) {
                    val strokeColor = try {
                        Color(android.graphics.Color.parseColor(strokeColorHex))
                    } catch (e: Exception) {
                        Color.Red
                    }

                    val path = Path().apply {
                        val first = currentDrawingPoints.first()
                        moveTo(first.first * size.width, first.second * size.height)
                        for (i in 1 until currentDrawingPoints.size) {
                            val pt = currentDrawingPoints[i]
                            lineTo(pt.first * size.width, pt.second * size.height)
                        }
                    }

                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = strokeWidthDp * density,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }

        // Helper function for magnetic snapping
        fun applyMagneticSnap(
            rawXFraction: Float,
            rawYFraction: Float,
            scale: Float,
            rotation: Float
        ): Pair<Float, Float> {
            var snapX = rawXFraction
            var snapY = rawYFraction
            var vSnap = false
            var hSnap = false

            // Snap to Center X = 0.5f
            if (abs(rawXFraction - 0.5f) < 0.04f) {
                snapX = 0.5f
                vSnap = true
            } else if (abs(rawXFraction - 0.33f) < 0.03f) {
                snapX = 0.33f
                vSnap = true
            } else if (abs(rawXFraction - 0.66f) < 0.03f) {
                snapX = 0.66f
                vSnap = true
            }

            // Snap to Center Y = 0.5f
            if (abs(rawYFraction - 0.5f) < 0.04f) {
                snapY = 0.5f
                hSnap = true
            } else if (abs(rawYFraction - 0.33f) < 0.03f) {
                snapY = 0.33f
                hSnap = true
            } else if (abs(rawYFraction - 0.66f) < 0.03f) {
                snapY = 0.66f
                hSnap = true
            }

            if ((vSnap && !showVerticalSnapGuide) || (hSnap && !showHorizontalSnapGuide)) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            showVerticalSnapGuide = vSnap
            showHorizontalSnapGuide = hSnap

            val xPct = (snapX * 100).roundToInt()
            val yPct = (snapY * 100).roundToInt()
            val scaleFormatted = String.format("%.1fx", scale)
            val rotFormatted = "${rotation.roundToInt()}°"
            hudText = "X: $xPct%  Y: $yPct%  Escala: $scaleFormatted  Rot: $rotFormatted"

            return Pair(snapX, snapY)
        }

        // Render Layers
        project.layers.forEach { layer ->
            if (!layer.isVisible) return@forEach
            if (currentVideoTimeMs < layer.startOffsetMs || currentVideoTimeMs > (layer.startOffsetMs + layer.durationMs)) return@forEach

            val isSelected = (layer.id == selectedLayerId) || selectedLayerIds.contains(layer.id)

            when (layer) {
                is CreativeLayer.Text -> {
                    TextLayerRenderer(
                        layer = layer,
                        isSelected = isSelected,
                        onLayerTransformed = { xFrac, yFrac, scale, rotation ->
                            if (layer.isLocked) return@TextLayerRenderer
                            val (snappedX, snappedY) = applyMagneticSnap(xFrac, yFrac, scale, rotation)
                            val updatedLayers = project.layers.map {
                                if (it.id == layer.id) {
                                    layer.copy(xFraction = snappedX, yFraction = snappedY, scale = scale, rotation = rotation)
                                } else it
                            }
                            onProjectUpdated(project.copy(layers = updatedLayers))
                        },
                        onClick = { onLayerSelected(layer.id) },
                        onDoubleTap = { onLayerDoubleTap(layer) },
                        onLongPress = { onLayerLongPress(layer) }
                    )
                }
                is CreativeLayer.Sticker -> {
                    StickerLayerRenderer(
                        layer = layer,
                        isSelected = isSelected,
                        onLayerTransformed = { xFrac, yFrac, scale, rotation ->
                            if (layer.isLocked) return@StickerLayerRenderer
                            val (snappedX, snappedY) = applyMagneticSnap(xFrac, yFrac, scale, rotation)
                            val updatedLayers = project.layers.map {
                                if (it.id == layer.id) {
                                    layer.copy(xFraction = snappedX, yFraction = snappedY, scale = scale, rotation = rotation)
                                } else it
                            }
                            onProjectUpdated(project.copy(layers = updatedLayers))
                        },
                        onClick = { onLayerSelected(layer.id) },
                        onDoubleTap = { onLayerDoubleTap(layer) },
                        onLongPress = { onLayerLongPress(layer) }
                    )
                }
                is CreativeLayer.Interactive -> {
                    InteractiveLayerRenderer(
                        layer = layer,
                        isSelected = isSelected,
                        onLayerTransformed = { xFrac, yFrac, scale, rotation ->
                            if (layer.isLocked) return@InteractiveLayerRenderer
                            val (snappedX, snappedY) = applyMagneticSnap(xFrac, yFrac, scale, rotation)
                            val updatedLayers = project.layers.map {
                                if (it.id == layer.id) {
                                    layer.copy(xFraction = snappedX, yFraction = snappedY, scale = scale, rotation = rotation)
                                } else it
                            }
                            onProjectUpdated(project.copy(layers = updatedLayers))
                        },
                        onClick = { onLayerSelected(layer.id) },
                        onDoubleTap = { onLayerDoubleTap(layer) },
                        onLongPress = { onLayerLongPress(layer) }
                    )
                }
                else -> Unit
            }
        }

        // 6. SNAP GUIDES OVERLAY (Vertical and Horizontal Alignment Lines)
        if (showVerticalSnapGuide || showHorizontalSnapGuide) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                if (showVerticalSnapGuide) {
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(size.width / 2f, 0f),
                        end = Offset(size.width / 2f, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }
                if (showHorizontalSnapGuide) {
                    drawLine(
                        color = Color(0xFF00E5FF),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        // 7. SMART RULES HUD (Real-time X, Y, Scale, Rotation display)
        if (hudText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .background(Color(0xDD0D0D12), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = hudText!!,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

