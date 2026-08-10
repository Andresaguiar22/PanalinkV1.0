package com.example.ui.settings.models

data class CustomizationUiState(
    val themeMode: String = "system",
    val profileThemeChoice: String = "dark_teal",
    val bottomBarColorChoice: String = "tropical",
    val bottomBarShapeChoice: String = "pill",
    val customR: Int = 0,
    val customG: Int = 211,
    val customB: Int = 102,
    val customSecR: Int = 0,
    val customSecG: Int = 188,
    val customSecB: Int = 212,
    val isMinimalistMode: Boolean = false,
    val isLoading: Boolean = false
)
