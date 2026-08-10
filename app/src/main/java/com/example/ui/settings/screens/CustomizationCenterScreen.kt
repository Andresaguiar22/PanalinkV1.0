package com.example.ui.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.settings.models.CustomizationAction
import com.example.ui.settings.viewmodel.CustomizationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationCenterScreen(
    onBack: () -> Unit,
    viewModel: CustomizationViewModel = viewModel()
) {
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personalización", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121B22))
            )
        },
        containerColor = Color(0xFF121B22)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // CARD 1: TEMA
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1E2B33)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Tema de Aplicación 🌗", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val modes = listOf("claro" to "Claro ☀️", "oscuro" to "Oscuro 🌙", "system" to "Sistema ⚙️")
                            modes.forEach { (mode, label) ->
                                val isSelected = uiState.themeMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) Color(0xFF25D366).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isSelected) Color(0xFF25D366) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            viewModel.dispatch(CustomizationAction.SetThemeMode(mode))
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, color = if (isSelected) Color(0xFF25D366) else Color.White, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }

            // CARD 2: COLORES
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1E2B33)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Colores del Sistema 🎨", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val themesRow1 = listOf(
                            "dark_teal" to "Teal 🟢",
                            "royal_purple" to "Purple 🟣",
                            "neon_orange" to "Orange 🟠",
                            "nordic_ice" to "Ice ❄️"
                        )
                        val themesRow2 = listOf(
                            "cyberpunk" to "Cyberpunk 👾",
                            "neon" to "Neon 🔮",
                            "minimal_white" to "Minimal White ⚪",
                            "custom" to "Custom Sliders 🛠️"
                        )
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                themesRow1.forEach { (themeKey, themeLabel) ->
                                    val isSelected = uiState.profileThemeChoice == themeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSelected) Color(0xFF00FF85).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.dispatch(CustomizationAction.SetProfileTheme(themeKey))
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(themeLabel, color = if (isSelected) Color(0xFF00FF85) else Color.White, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                themesRow2.forEach { (themeKey, themeLabel) ->
                                    val isSelected = uiState.profileThemeChoice == themeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSelected) Color(0xFF00FF85).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.dispatch(CustomizationAction.SetProfileTheme(themeKey))
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(themeLabel, color = if (isSelected) Color(0xFF00FF85) else Color.White, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }

                        if (uiState.profileThemeChoice == "custom") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF101D24).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF25D366).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Laboratorio de Colores 🧪", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Color Primario (RGB):", fontSize = 11.sp, color = Color.White)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("R: ${uiState.customR}", modifier = Modifier.width(40.dp), fontSize = 11.sp, color = Color.Red)
                                        Slider(
                                            value = uiState.customR.toFloat(),
                                            onValueChange = { viewModel.dispatch(CustomizationAction.UpdateCustomPrimary(it.toInt(), uiState.customG, uiState.customB)) },
                                            valueRange = 0f..255f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(activeTrackColor = Color.Red, thumbColor = Color.Red)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("G: ${uiState.customG}", modifier = Modifier.width(40.dp), fontSize = 11.sp, color = Color.Green)
                                        Slider(
                                            value = uiState.customG.toFloat(),
                                            onValueChange = { viewModel.dispatch(CustomizationAction.UpdateCustomPrimary(uiState.customR, it.toInt(), uiState.customB)) },
                                            valueRange = 0f..255f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(activeTrackColor = Color.Green, thumbColor = Color.Green)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("B: ${uiState.customB}", modifier = Modifier.width(40.dp), fontSize = 11.sp, color = Color.Cyan)
                                        Slider(
                                            value = uiState.customB.toFloat(),
                                            onValueChange = { viewModel.dispatch(CustomizationAction.UpdateCustomPrimary(uiState.customR, uiState.customG, it.toInt())) },
                                            valueRange = 0f..255f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(activeTrackColor = Color.Cyan, thumbColor = Color.Cyan)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Color Secundario (RGB):", fontSize = 11.sp, color = Color.White)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("R: ${uiState.customSecR}", modifier = Modifier.width(40.dp), fontSize = 11.sp, color = Color.Red)
                                        Slider(
                                            value = uiState.customSecR.toFloat(),
                                            onValueChange = { viewModel.dispatch(CustomizationAction.UpdateCustomSecondary(it.toInt(), uiState.customSecG, uiState.customSecB)) },
                                            valueRange = 0f..255f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(activeTrackColor = Color.Red, thumbColor = Color.Red)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("G: ${uiState.customSecG}", modifier = Modifier.width(40.dp), fontSize = 11.sp, color = Color.Green)
                                        Slider(
                                            value = uiState.customSecG.toFloat(),
                                            onValueChange = { viewModel.dispatch(CustomizationAction.UpdateCustomSecondary(uiState.customSecR, it.toInt(), uiState.customSecB)) },
                                            valueRange = 0f..255f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(activeTrackColor = Color.Green, thumbColor = Color.Green)
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("B: ${uiState.customSecB}", modifier = Modifier.width(40.dp), fontSize = 11.sp, color = Color.Cyan)
                                        Slider(
                                            value = uiState.customSecB.toFloat(),
                                            onValueChange = { viewModel.dispatch(CustomizationAction.UpdateCustomSecondary(uiState.customSecR, uiState.customSecG, it.toInt())) },
                                            valueRange = 0f..255f,
                                            modifier = Modifier.weight(1f),
                                            colors = SliderDefaults.colors(activeTrackColor = Color.Cyan, thumbColor = Color.Cyan)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CARD 3: EXPERIENCIA PANALINK
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1E2B33)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Experiencia PanaLink 🚀", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Minimalist Mode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF101D24), RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.dispatch(CustomizationAction.SetMinimalistMode(!uiState.isMinimalistMode))
                                }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Modo Minimalista Futurista 🌐", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("UI ultra limpia sin textos superfluos en botones ni menús.", color = Color(0xFF90A4AE), fontSize = 11.sp, lineHeight = 14.sp)
                            }
                            Switch(
                                checked = uiState.isMinimalistMode,
                                onCheckedChange = { viewModel.dispatch(CustomizationAction.SetMinimalistMode(it)) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF25D366), uncheckedThumbColor = Color(0xFF90A4AE), uncheckedTrackColor = Color(0xFF37474F))
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Color de la Barra de Navegación 🌈:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val colorPresets = listOf(
                            "tropical" to "Mint 🌿",
                            "neon_cyber" to "Neon ⚡",
                            "monochrome" to "Gris 🩶",
                            "sunset" to "Sunset 🌅",
                            "aurora" to "Aurora 🌌"
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            colorPresets.take(3).forEach { (presetKey, label) ->
                                val isSelected = uiState.bottomBarColorChoice == presetKey
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .background(if (isSelected) Color(0xFF00FF85).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.dispatch(CustomizationAction.SetBottomBarColor(presetKey))
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text(label, color = if (isSelected) Color(0xFF00FF85) else Color.White, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            colorPresets.drop(3).forEach { (presetKey, label) ->
                                val isSelected = uiState.bottomBarColorChoice == presetKey
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .background(if (isSelected) Color(0xFF00FF85).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.dispatch(CustomizationAction.SetBottomBarColor(presetKey))
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text(label, color = if (isSelected) Color(0xFF00FF85) else Color.White, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Geometría y Forma de Bordes 📐:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val shapePresets = listOf(
                            "pill" to "Píldora 💊",
                            "rounded_rect" to "Suave 🔲",
                            "cut_corners" to "Futurista 🔪",
                            "wave" to "Onda 🌊",
                            "sharp" to "Rectangular ⬛"
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            shapePresets.take(3).forEach { (presetKey, label) ->
                                val isSelected = uiState.bottomBarShapeChoice == presetKey
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .background(if (isSelected) Color(0xFF00FF85).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.dispatch(CustomizationAction.SetBottomBarShape(presetKey))
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text(label, color = if (isSelected) Color(0xFF00FF85) else Color.White, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            shapePresets.drop(3).forEach { (presetKey, label) ->
                                val isSelected = uiState.bottomBarShapeChoice == presetKey
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .background(if (isSelected) Color(0xFF00FF85).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.dispatch(CustomizationAction.SetBottomBarShape(presetKey))
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text(label, color = if (isSelected) Color(0xFF00FF85) else Color.White, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
