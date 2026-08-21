package com.example.ui.settings.models

data class SecurityUiState(
    val hasPin: Boolean = false,
    val hasPattern: Boolean = false,
    val pin: String = "",
    val is2FaEnabled: Boolean = false,
    val isBiometricsEnabled: Boolean = false,
    val biometricsAvailable: Boolean = false,
    val autoLockMs: Long = 0L,
    val userPinCode: String = "",
    val userUid: String = "",
    val isPinDialogVisible: Boolean = false,
    val isPatternDialogVisible: Boolean = false,
    val isQrDialogVisible: Boolean = false,
    val isScannerVisible: Boolean = false,
    val scannedQrResult: String? = null,
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)
