new_content = """package com.example.ui.settings.screens

import android.content.Context
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.data.supabase.SupabaseClient
import com.example.ui.theme.ThemeManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val currentUid = SupabaseClient.currentUser?.id ?: ""
    val prefs = remember { context.getSharedPreferences("panalink_prefs", Context.MODE_PRIVATE) }

    // States
    var profileThemeChoice by remember { mutableStateOf(prefs.getString("profile_theme_${currentUid}", "dark_teal") ?: "dark_teal") }
    var bottomBarColorChoice by remember { mutableStateOf(prefs.getString("bottom_bar_color_preset", "tropical") ?: "tropical") }
    var bottomBarShapeChoice by remember { mutableStateOf(prefs.getString("bottom_bar_shape_preset", "pill") ?: "pill") }

    var customR by remember { mutableStateOf(((ThemeManager.customPrimary.value.red) * 255f).toInt()) }
    var customG by remember { mutableStateOf(((ThemeManager.customPrimary.value.green) * 255f).toInt()) }
    var customB by remember { mutableStateOf(((ThemeManager.customPrimary.value.blue) * 255f).toInt()) }

    var customSecR by remember { mutableStateOf(((ThemeManager.customSecondary.value.red) * 255f).toInt()) }
    var customSecG by remember { mutableStateOf(((ThemeManager.customSecondary.value.green) * 255f).toInt()) }
    var customSecB by remember { mutableStateOf(((ThemeManager.customSecondary.value.blue) * 255f).toInt()) }

    val activeMinimalistMode by ThemeManager.isMinimalistMode.collectAsState()

    var themeMode by remember { mutableStateOf(prefs.getString("theme_mode_global", "system") ?: "system") } // claro, oscuro, system

    // Effects
    LaunchedEffect(profileThemeChoice) {
        prefs.edit().apply {
            putString("profile_theme_${currentUid}", profileThemeChoice)
            putString("profile_theme_global", profileThemeChoice)
            apply()
        }
        ThemeManager.themeKey.value = profileThemeChoice
    }

    LaunchedEffect(customR, customG, customB) {
        val colorInt = android.graphics.Color.rgb(customR, customG, customB)
        val newColor = Color(colorInt)
        ThemeManager.customPrimary.value = newColor
        prefs.edit().putInt("custom_primary", colorInt).apply()
    }

    LaunchedEffect(customSecR, customSecG, customSecB) {
        val colorInt = android.graphics.Color.rgb(customSecR, customSecG, customSecB)
        val newColor = Color(colorInt)
        ThemeManager.customSecondary.value = newColor
        prefs.edit().putInt("custom_secondary", colorInt).apply()
    }

    LaunchedEffect(bottomBarColorChoice, bottomBarShapeChoice) {
        prefs.edit().apply {
            putString("bottom_bar_color_preset", bottomBarColorChoice)
            putString("bottom_bar_shape_preset", bottomBarShapeChoice)
            apply()
        }
        ThemeManager.bottomBarColorPreset.value = bottomBarColorChoice
        ThemeManager.bottomBarShapePreset.value = bottomBarShapeChoice
    }

    LaunchedEffect(themeMode) {
        prefs.edit().putString("theme_mode_global", themeMode).apply()
        // If they ever requested this to do something globally to ThemeManager, it would go here.
        // For now we just persist it, as per instructions.
    }

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
                                val isSelected = themeMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) Color(0xFF25D366).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                        .border(1.dp, if (isSelected) Color(0xFF25D366) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable { themeMode = mode; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
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
                                    val isSelected = profileThemeChoice == themeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSelected) Color(0xFF00FF85).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); profileThemeChoice = themeKey }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(themeLabel, color = if (isSelected) Color(0xFF00FF85) else Color.White, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                themesRow2.forEach { (themeKey, themeLabel) ->
                                    val isSelected = profileThemeChoice == themeKey
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isSelected) Color(0xFF00FF85).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                            .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                            .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); profileThemeChoice = themeKey }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(themeLabel, color = if (isSelected) Color(0xFF00FF85) else Color.White, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }

                        if (profileThemeChoice == "custom") {
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
                                        Text("R: $customR", modifier = Modifier.width(40.dp), fontSize = 11.sp, color = Color.Red)
                                        Slider(value = customR.toFloat(), onValueChange = { customR = it.toInt() }, valueRange = 0f..255f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(activeTrackColor = Color.Red, thumbColor = Color.Red))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("G: $customG", modifier = Modifier.width(40.dp), fontSize = 11.sp, color = Color.Green)
                                        Slider(value = customG.toFloat(), onValueChange = { customG = it.toInt() }, valueRange = 0f..255f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(activeTrackColor = Color.Green, thumbColor = Color.Green))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("B: $customB", modifier = Modifier.width(40.dp), fontSize = 11.sp, color = Color.Cyan)
                                        Slider(value = customB.toFloat(), onValueChange = { customB = it.toInt() }, valueRange = 0f..255f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(activeTrackColor = Color.Cyan, thumbColor = Color.Cyan))
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Color Secundario (RGB):", fontSize = 11.sp, color = Color.White)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("R: $customSecR", modifier = Modifier.width(40.dp), fontSize = 11.sp, color = Color.Red)
                                        Slider(value = customSecR.toFloat(), onValueChange = { customSecR = it.toInt() }, valueRange = 0f..255f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(activeTrackColor = Color.Red, thumbColor = Color.Red))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("G: $customSecG", modifier = Modifier.width(40.dp), fontSize = 11.sp, color = Color.Green)
                                        Slider(value = customSecG.toFloat(), onValueChange = { customSecG = it.toInt() }, valueRange = 0f..255f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(activeTrackColor = Color.Green, thumbColor = Color.Green))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("B: $customSecB", modifier = Modifier.width(40.dp), fontSize = 11.sp, color = Color.Cyan)
                                        Slider(value = customSecB.toFloat(), onValueChange = { customSecB = it.toInt() }, valueRange = 0f..255f, modifier = Modifier.weight(1f), colors = SliderDefaults.colors(activeTrackColor = Color.Cyan, thumbColor = Color.Cyan))
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
                                    val nextVal = !activeMinimalistMode
                                    ThemeManager.isMinimalistMode.value = nextVal
                                    prefs.edit().putBoolean("minimalist_mode_global", nextVal).apply()
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
                                checked = activeMinimalistMode,
                                onCheckedChange = { nextVal ->
                                    ThemeManager.isMinimalistMode.value = nextVal
                                    prefs.edit().putBoolean("minimalist_mode_global", nextVal).apply()
                                },
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
                                val isSelected = bottomBarColorChoice == presetKey
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .background(if (isSelected) Color(0xFF00FF85).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); bottomBarColorChoice = presetKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text(label, color = if (isSelected) Color(0xFF00FF85) else Color.White, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            colorPresets.drop(3).forEach { (presetKey, label) ->
                                val isSelected = bottomBarColorChoice == presetKey
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .background(if (isSelected) Color(0xFF00FF85).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); bottomBarColorChoice = presetKey }
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
                                val isSelected = bottomBarShapeChoice == presetKey
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .background(if (isSelected) Color(0xFF00FF85).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); bottomBarShapeChoice = presetKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) { Text(label, color = if (isSelected) Color(0xFF00FF85) else Color.White, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            shapePresets.drop(3).forEach { (presetKey, label) ->
                                val isSelected = bottomBarShapeChoice == presetKey
                                Box(
                                    modifier = Modifier.weight(1f)
                                        .background(if (isSelected) Color(0xFF00FF85).copy(alpha = 0.2f) else Color(0xFF101D24), RoundedCornerShape(8.dp))
                                        .border(if (isSelected) 1.5.dp else 1.dp, if (isSelected) Color(0xFF00FF85) else Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                        .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); bottomBarShapeChoice = presetKey }
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
"""
with open("app/src/main/java/com/example/ui/settings/screens/CustomizationCenterScreen.kt", "w") as f:
    f.write(new_content)
