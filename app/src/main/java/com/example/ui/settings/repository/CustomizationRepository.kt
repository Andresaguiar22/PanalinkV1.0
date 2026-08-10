package com.example.ui.settings.repository

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.ui.settings.models.CustomizationUiState
import com.example.ui.theme.ThemeManager

class CustomizationRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("panalink_prefs", Context.MODE_PRIVATE)

    fun loadCustomization(uid: String): CustomizationUiState {
        val themeMode = prefs.getString("theme_mode_global", "system") ?: "system"
        val profileTheme = prefs.getString("profile_theme_${uid}", "dark_teal") ?: "dark_teal"
        val bottomBarColor = prefs.getString("bottom_bar_color_preset", "tropical") ?: "tropical"
        val bottomBarShape = prefs.getString("bottom_bar_shape_preset", "pill") ?: "pill"
        val minimalist = prefs.getBoolean("minimalist_mode_global", false)

        val primaryInt = prefs.getInt("custom_primary", android.graphics.Color.rgb(0, 211, 102))
        val secondaryInt = prefs.getInt("custom_secondary", android.graphics.Color.rgb(0, 188, 212))

        val pColor = android.graphics.Color.valueOf(primaryInt)
        val sColor = android.graphics.Color.valueOf(secondaryInt)

        return CustomizationUiState(
            themeMode = themeMode,
            profileThemeChoice = profileTheme,
            bottomBarColorChoice = bottomBarColor,
            bottomBarShapeChoice = bottomBarShape,
            customR = (pColor.red() * 255).toInt(),
            customG = (pColor.green() * 255).toInt(),
            customB = (pColor.blue() * 255).toInt(),
            customSecR = (sColor.red() * 255).toInt(),
            customSecG = (sColor.green() * 255).toInt(),
            customSecB = (sColor.blue() * 255).toInt(),
            isMinimalistMode = minimalist
        )
    }

    fun saveThemeMode(mode: String) {
        prefs.edit().putString("theme_mode_global", mode).apply()
    }

    fun saveProfileTheme(uid: String, theme: String) {
        prefs.edit().apply {
            putString("profile_theme_${uid}", theme)
            putString("profile_theme_global", theme)
            apply()
        }
        ThemeManager.themeKey.value = theme
    }

    fun saveBottomBarPreset(colorPreset: String, shapePreset: String) {
        prefs.edit().apply {
            putString("bottom_bar_color_preset", colorPreset)
            putString("bottom_bar_shape_preset", shapePreset)
            apply()
        }
        ThemeManager.bottomBarColorPreset.value = colorPreset
        ThemeManager.bottomBarShapePreset.value = shapePreset
    }

    fun saveCustomPrimary(r: Int, g: Int, b: Int) {
        val colorInt = android.graphics.Color.rgb(r, g, b)
        prefs.edit().putInt("custom_primary", colorInt).apply()
        ThemeManager.customPrimary.value = Color(colorInt)
    }

    fun saveCustomSecondary(r: Int, g: Int, b: Int) {
        val colorInt = android.graphics.Color.rgb(r, g, b)
        prefs.edit().putInt("custom_secondary", colorInt).apply()
        ThemeManager.customSecondary.value = Color(colorInt)
    }

    fun saveMinimalistMode(enabled: Boolean) {
        prefs.edit().putBoolean("minimalist_mode_global", enabled).apply()
        ThemeManager.isMinimalistMode.value = enabled
    }
}
