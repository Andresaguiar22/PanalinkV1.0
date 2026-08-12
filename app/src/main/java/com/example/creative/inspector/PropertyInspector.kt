package com.example.creative.inspector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.creative.animation.EasingType
import com.example.creative.core.CreativeLayer
import com.example.creative.timeline.CreativeTrack

/**
 * P6.5A - Unified Property Inspector Composable
 * Reusable inspector for Reel Studio, Story Studio, Post Studio, Chat, Profile & Sticker editor.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyInspector(
    selectedLayer: CreativeLayer?,
    selectedTrack: CreativeTrack?,
    currentTimeMs: Long,
    onUpdateLayer: (CreativeLayer) -> Unit,
    onUpdateTrack: (CreativeTrack) -> Unit,
    onAddKeyframe: (layerId: String, propertyName: String, value: Float, easing: EasingType) -> Unit,
    onRemoveKeyframe: (layerId: String, propertyName: String, timeMs: Long) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("transform") }

    Card(
        modifier = modifier.fillMaxWidth().padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF16161E)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF00E5FF))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        selectedLayer is CreativeLayer.Text -> "Inspector de Texto 🔤"
                        selectedLayer is CreativeLayer.Sticker -> "Inspector de Sticker 🎨"
                        selectedLayer is CreativeLayer.Image -> "Inspector de Imagen 🖼️"
                        selectedLayer is CreativeLayer.Drawing -> "Inspector de Dibujo ✏️"
                        selectedLayer is CreativeLayer.Filter -> "Inspector de Filtro 🔮"
                        selectedLayer is CreativeLayer.Audio -> "Inspector de Audio 🎵"
                        selectedTrack is CreativeTrack.VideoTrack -> "Inspector de Video 🎬"
                        selectedTrack is CreativeTrack.AudioTrack -> "Inspector de Pista de Audio 🎧"
                        else -> "Inspector de Propiedades ⚙️"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar Inspector", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = activeTab == "transform",
                    onClick = { activeTab = "transform" },
                    label = { Text("Transform", fontSize = 11.sp, color = if (activeTab == "transform") Color.Black else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF))
                )
                FilterChip(
                    selected = activeTab == "style",
                    onClick = { activeTab = "style" },
                    label = { Text("Estilo", fontSize = 11.sp, color = if (activeTab == "style") Color.Black else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF))
                )
                FilterChip(
                    selected = activeTab == "animation",
                    onClick = { activeTab = "animation" },
                    label = { Text("Keyframes", fontSize = 11.sp, color = if (activeTab == "animation") Color.Black else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF))
                )
                if (selectedLayer is CreativeLayer.Audio || selectedTrack is CreativeTrack.AudioTrack || selectedTrack is CreativeTrack.VideoTrack) {
                    FilterChip(
                        selected = activeTab == "audio",
                        onClick = { activeTab = "audio" },
                        label = { Text("Audio", fontSize = 11.sp, color = if (activeTab == "audio") Color.Black else Color.White) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (activeTab) {
                "transform" -> TransformInspectorPanel(selectedLayer, onUpdateLayer)
                "style" -> StyleInspectorPanel(selectedLayer, onUpdateLayer)
                "animation" -> KeyframeAnimationPanel(selectedLayer, currentTimeMs, onAddKeyframe, onRemoveKeyframe)
                "audio" -> AudioControlPanel(selectedLayer, selectedTrack, onUpdateLayer, onUpdateTrack)
            }
        }
    }
}

@Composable
private fun TransformInspectorPanel(layer: CreativeLayer?, onUpdateLayer: (CreativeLayer) -> Unit) {
    if (layer == null) return
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Escala: ${(layer.scale * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
        Slider(
            value = layer.scale,
            onValueChange = { newScale -> updateLayerTransform(layer, onUpdateLayer, scale = newScale) },
            valueRange = 0.2f..3.5f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
        )
        Text("Rotación: ${layer.rotation.toInt()}°", color = Color.White, fontSize = 12.sp)
        Slider(
            value = layer.rotation,
            onValueChange = { newRotation -> updateLayerTransform(layer, onUpdateLayer, rotation = newRotation) },
            valueRange = -180f..180f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
        )
        Text("Opacidad: ${(layer.opacity * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
        Slider(
            value = layer.opacity,
            onValueChange = { newOpacity -> updateLayerTransform(layer, onUpdateLayer, opacity = newOpacity) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
        )
    }
}

private fun updateLayerTransform(
    layer: CreativeLayer,
    onUpdateLayer: (CreativeLayer) -> Unit,
    scale: Float = layer.scale,
    rotation: Float = layer.rotation,
    opacity: Float = layer.opacity
) {
    when (layer) {
        is CreativeLayer.Text -> onUpdateLayer(layer.copy(scale = scale, rotation = rotation, opacity = opacity))
        is CreativeLayer.Sticker -> onUpdateLayer(layer.copy(scale = scale, rotation = rotation, opacity = opacity))
        is CreativeLayer.Drawing -> onUpdateLayer(layer.copy(scale = scale, rotation = rotation, opacity = opacity))
        is CreativeLayer.Filter -> onUpdateLayer(layer.copy(scale = scale, rotation = rotation, opacity = opacity))
        is CreativeLayer.Audio -> onUpdateLayer(layer.copy(scale = scale, rotation = rotation, opacity = opacity))
        is CreativeLayer.Interactive -> onUpdateLayer(layer.copy(scale = scale, rotation = rotation, opacity = opacity))
        is CreativeLayer.Group -> onUpdateLayer(layer.copy(scale = scale, rotation = rotation, opacity = opacity))
        is CreativeLayer.Image -> onUpdateLayer(layer.copy(scale = scale, rotation = rotation, opacity = opacity))
        is CreativeLayer.Video -> onUpdateLayer(layer.copy(scale = scale, rotation = rotation, opacity = opacity))
    }
}

@Composable
private fun StyleInspectorPanel(layer: CreativeLayer?, onUpdateLayer: (CreativeLayer) -> Unit) {
    when (layer) {
        is CreativeLayer.Text -> TextStyleControls(layer, onUpdateLayer)
        is CreativeLayer.Image -> ImageStyleControls(layer, onUpdateLayer)
        else -> Text("No hay opciones avanzadas de estilo para este tipo de capa.", color = Color.Gray, fontSize = 12.sp)
    }
}

@Composable
private fun TextStyleControls(layer: CreativeLayer.Text, onUpdateLayer: (CreativeLayer) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Tipografía:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        val fonts = listOf("SansSerif", "Serif", "Monospace", "Cursive")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fonts.forEach { font ->
                FilterChip(
                    selected = layer.fontFamily == font,
                    onClick = { onUpdateLayer(layer.copy(fontFamily = font)) },
                    label = { Text(font, fontSize = 11.sp, color = if (layer.fontFamily == font) Color.Black else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF))
                )
            }
        }
        Text("Color del Texto:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        val colors = listOf("#FFFFFF", "#00E5FF", "#FF4081", "#FFD54F", "#00FF85", "#E040FB", "#000000")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(colors) { hex ->
                val colorInt = android.graphics.Color.parseColor(hex)
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(colorInt)).border(
                        width = if (layer.colorHex == hex) 2.dp else 0.dp,
                        color = Color.White,
                        shape = CircleShape
                    ).clickable { onUpdateLayer(layer.copy(colorHex = hex)) }
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Sombra de Texto", color = Color.White, fontSize = 12.sp)
            Switch(
                checked = layer.hasShadow,
                onCheckedChange = { onUpdateLayer(layer.copy(hasShadow = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00E5FF))
            )
        }
    }
}

@Composable
private fun ImageStyleControls(layer: CreativeLayer.Image, onUpdateLayer: (CreativeLayer) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ajustes de imagen 🖼️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        AdjustmentSlider("Brillo", layer.brightness, -1f..1f) { onUpdateLayer(layer.copy(brightness = it)) }
        AdjustmentSlider("Contraste", layer.contrast, -1f..1f) { onUpdateLayer(layer.copy(contrast = it)) }
        AdjustmentSlider("Saturación", layer.saturation, 0f..2f) { onUpdateLayer(layer.copy(saturation = it)) }

        Text("Filtro", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        val filters = listOf("none", "warm", "cool", "mono", "vintage", "vivid")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(filters) { filter ->
                FilterChip(
                    selected = layer.filterName == filter,
                    onClick = { onUpdateLayer(layer.copy(filterName = filter)) },
                    label = { Text(filter, fontSize = 10.sp, color = if (layer.filterName == filter) Color.Black else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF))
                )
            }
        }
    }
}

@Composable
private fun AdjustmentSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit) {
    Text("$label: ${"%.2f".format(value)}", color = Color.White, fontSize = 11.sp)
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF))
    )
}

@Composable
private fun KeyframeAnimationPanel(
    layer: CreativeLayer?,
    currentTimeMs: Long,
    onAddKeyframe: (layerId: String, propertyName: String, value: Float, easing: EasingType) -> Unit,
    onRemoveKeyframe: (layerId: String, propertyName: String, timeMs: Long) -> Unit
) {
    if (layer == null) return
    var selectedProperty by remember { mutableStateOf("scale") }
    var selectedEasing by remember { mutableStateOf(EasingType.EASE_IN_OUT) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Pistas de Propiedad (Keyframes):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("scale" to "Escala", "rotation" to "Rotación", "opacity" to "Opacidad").forEach { (prop, label) ->
                FilterChip(
                    selected = selectedProperty == prop,
                    onClick = { selectedProperty = prop },
                    label = { Text(label, fontSize = 11.sp, color = if (selectedProperty == prop) Color.Black else Color.White) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF00E5FF))
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    val value = when (selectedProperty) {
                        "scale" -> layer.scale
                        "rotation" -> layer.rotation
                        "opacity" -> layer.opacity
                        else -> 1f
                    }
                    onAddKeyframe(layer.id, selectedProperty, value, selectedEasing)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Insertar Keyframe ($currentTimeMs ms)", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { onRemoveKeyframe(layer.id, selectedProperty, currentTimeMs) }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar Keyframe", tint = Color(0xFFFF5252))
            }
        }
    }
}

@Composable
private fun AudioControlPanel(
    layer: CreativeLayer?,
    track: CreativeTrack?,
    onUpdateLayer: (CreativeLayer) -> Unit,
    onUpdateTrack: (CreativeTrack) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Controles de Audio Profesional 🎧", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        if (layer is CreativeLayer.Audio) {
            Text("Volumen: ${(layer.volume * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
            Slider(value = layer.volume, onValueChange = { onUpdateLayer(layer.copy(volume = it)) }, valueRange = 0f..2f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF)))
        } else if (track is CreativeTrack.AudioTrack) {
            Text("Volumen de Música: ${(track.volume * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
            Slider(value = track.volume, onValueChange = { onUpdateTrack(track.copy(volume = it)) }, valueRange = 0f..2f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF)))
        } else if (track is CreativeTrack.VideoTrack) {
            Text("Volumen del Video Original: ${(track.volume * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
            Slider(value = track.volume, onValueChange = { onUpdateTrack(track.copy(volume = it)) }, valueRange = 0f..2f,
                colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5FF), activeTrackColor = Color(0xFF00E5FF)))
        }
    }
}
