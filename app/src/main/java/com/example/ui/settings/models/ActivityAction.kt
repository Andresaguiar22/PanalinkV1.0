package com.example.ui.settings.models

sealed interface ActivityAction {
    object RefreshSummary : ActivityAction
    object LoadDiagnostics : ActivityAction
    object RefreshStorage : ActivityAction
    object RefreshDevices : ActivityAction
    object ClearError : ActivityAction
}
