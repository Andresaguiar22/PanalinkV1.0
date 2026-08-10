package com.example.ui.settings.models

sealed interface DashboardAction {
    object RefreshDashboard : DashboardAction
    object ClearError : DashboardAction
}
