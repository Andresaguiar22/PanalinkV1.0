package com.example.ui.settings.models

sealed interface CustomizationAction {
    data class SetThemeMode(val mode: String) : CustomizationAction
    data class SetProfileTheme(val theme: String) : CustomizationAction
    data class SetBottomBarColor(val preset: String) : CustomizationAction
    data class SetBottomBarShape(val preset: String) : CustomizationAction
    data class UpdateCustomPrimary(val r: Int, val g: Int, val b: Int) : CustomizationAction
    data class UpdateCustomSecondary(val r: Int, val g: Int, val b: Int) : CustomizationAction
    data class SetMinimalistMode(val enabled: Boolean) : CustomizationAction
}
