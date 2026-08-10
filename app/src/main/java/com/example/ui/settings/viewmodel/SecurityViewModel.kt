package com.example.ui.settings.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.settings.models.SecurityAction
import com.example.ui.settings.models.SecurityUiState
import com.example.ui.settings.repository.SecurityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SecurityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SecurityRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(SecurityUiState(isLoading = true))
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val loadedState = repository.loadSecuritySettings()
            _uiState.value = loadedState
        }
    }

    fun dispatch(action: SecurityAction) {
        when (action) {
            is SecurityAction.SetPin -> {
                if (action.newPin.length < 4) {
                    _uiState.update {
                        it.copy(errorMessage = "El PIN debe tener al menos 4 dígitos")
                    }
                    return
                }
                repository.savePin(action.newPin)
                _uiState.update {
                    it.copy(
                        hasPin = true,
                        pin = action.newPin,
                        isPinDialogVisible = false,
                        successMessage = "PIN de seguridad configurado exitosamente 🔐"
                    )
                }
            }
            is SecurityAction.RemovePin -> {
                repository.removePin()
                _uiState.update {
                    it.copy(
                        hasPin = false,
                        pin = "",
                        isPinDialogVisible = false,
                        successMessage = "PIN de seguridad eliminado"
                    )
                }
            }
            is SecurityAction.Toggle2Fa -> {
                repository.save2Fa(action.enabled)
                _uiState.update {
                    it.copy(
                        is2FaEnabled = action.enabled,
                        successMessage = if (action.enabled) "Autenticación en 2 Pasos activada 🛡️" else "Autenticación en 2 Pasos desactivada"
                    )
                }
            }
            is SecurityAction.ToggleBiometrics -> {
                repository.saveBiometrics(action.enabled)
                _uiState.update {
                    it.copy(
                        isBiometricsEnabled = action.enabled,
                        successMessage = if (action.enabled) "Desbloqueo Biométrico activado 👆" else "Desbloqueo Biométrico desactivado"
                    )
                }
            }
            is SecurityAction.ShowPinDialog -> {
                _uiState.update { it.copy(isPinDialogVisible = action.show) }
            }
            is SecurityAction.ShowQrDialog -> {
                _uiState.update { it.copy(isQrDialogVisible = action.show) }
            }
            is SecurityAction.ShowScanner -> {
                _uiState.update { it.copy(isScannerVisible = action.show) }
            }
            is SecurityAction.ProcessScannedQr -> {
                val payload = action.qrPayload.trim()
                if (payload.startsWith("panalink:")) {
                    _uiState.update {
                        it.copy(
                            isScannerVisible = false,
                            scannedQrResult = payload,
                            successMessage = "¡Código QR Pana validado correctamente! ⚡"
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            errorMessage = "El código QR escaneado no pertenece a la red PanaLink"
                        )
                    }
                }
            }
            is SecurityAction.ClearMessages -> {
                _uiState.update { it.copy(successMessage = null, errorMessage = null) }
            }
        }
    }
}
