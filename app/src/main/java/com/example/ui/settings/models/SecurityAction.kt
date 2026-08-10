package com.example.ui.settings.models

sealed interface SecurityAction {
    data class SetPin(val newPin: String) : SecurityAction
    object RemovePin : SecurityAction
    data class Toggle2Fa(val enabled: Boolean) : SecurityAction
    data class ToggleBiometrics(val enabled: Boolean) : SecurityAction
    data class ShowPinDialog(val show: Boolean) : SecurityAction
    data class ShowQrDialog(val show: Boolean) : SecurityAction
    data class ShowScanner(val show: Boolean) : SecurityAction
    data class ProcessScannedQr(val qrPayload: String) : SecurityAction
    object ClearMessages : SecurityAction
}
