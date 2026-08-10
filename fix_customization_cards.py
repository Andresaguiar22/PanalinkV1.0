ui_code = open("customization_ui.kt", "r").read()

new_content = f"""package com.example.ui.settings.screens

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
fun CustomizationCenterScreen(onBack: () -> Unit) {{
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val currentUid = SupabaseClient.currentUser?.id ?: ""
    val prefs = remember {{ context.getSharedPreferences("panalink_prefs", Context.MODE_PRIVATE) }}

    var profileThemeChoice by remember {{ mutableStateOf(prefs.getString("profile_theme_${{currentUid}}", "dark_teal") ?: "dark_teal") }}
    var bottomBarColorChoice by remember {{ mutableStateOf(prefs.getString("bottom_bar_color_preset", "tropical") ?: "tropical") }}
    var bottomBarShapeChoice by remember {{ mutableStateOf(prefs.getString("bottom_bar_shape_preset", "pill") ?: "pill") }}

    var customR by remember {{ mutableStateOf(((ThemeManager.customPrimary.value.red) * 255f).toInt()) }}
    var customG by remember {{ mutableStateOf(((ThemeManager.customPrimary.value.green) * 255f).toInt()) }}
    var customB by remember {{ mutableStateOf(((ThemeManager.customPrimary.value.blue) * 255f).toInt()) }}

    var customSecR by remember {{ mutableStateOf(((ThemeManager.customSecondary.value.red) * 255f).toInt()) }}
    var customSecG by remember {{ mutableStateOf(((ThemeManager.customSecondary.value.green) * 255f).toInt()) }}
    var customSecB by remember {{ mutableStateOf(((ThemeManager.customSecondary.value.blue) * 255f).toInt()) }}

    val activeMinimalistMode by ThemeManager.isMinimalistMode.collectAsState()

    LaunchedEffect(profileThemeChoice) {{
        prefs.edit().apply {{
            putString("profile_theme_${{currentUid}}", profileThemeChoice)
            putString("profile_theme_global", profileThemeChoice)
            apply()
        }}
        ThemeManager.themeKey.value = profileThemeChoice
    }}

    LaunchedEffect(customR, customG, customB) {{
        val colorInt = android.graphics.Color.rgb(customR, customG, customB)
        val newColor = Color(colorInt)
        ThemeManager.customPrimary.value = newColor
        prefs.edit().putInt("custom_primary", colorInt).apply()
    }}

    LaunchedEffect(customSecR, customSecG, customSecB) {{
        val colorInt = android.graphics.Color.rgb(customSecR, customSecG, customSecB)
        val newColor = Color(colorInt)
        ThemeManager.customSecondary.value = newColor
        prefs.edit().putInt("custom_secondary", colorInt).apply()
    }}

    LaunchedEffect(bottomBarColorChoice, bottomBarShapeChoice) {{
        prefs.edit().apply {{
            putString("bottom_bar_color_preset", bottomBarColorChoice)
            putString("bottom_bar_shape_preset", bottomBarShapeChoice)
            apply()
        }}
        ThemeManager.bottomBarColorPreset.value = bottomBarColorChoice
        ThemeManager.bottomBarShapePreset.value = bottomBarShapeChoice
    }}

    Scaffold(
        topBar = {{
            TopAppBar(
                title = {{ Text("Personalización", color = Color.White) }},
                navigationIcon = {{
                    IconButton(onClick = onBack) {{
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }}
                }},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121B22))
            )
        }},
        containerColor = Color(0xFF121B22)
    ) {{ padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {{
            item {{
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1E2B33)),
                    modifier = Modifier.fillMaxWidth()
                ) {{
                    Column(modifier = Modifier.padding(16.dp)) {{
{ui_code}
                    }}
                }}
            }}
        }}
    }}
}}
"""

with open("app/src/main/java/com/example/ui/settings/screens/CustomizationCenterScreen.kt", "w") as f:
    f.write(new_content)

