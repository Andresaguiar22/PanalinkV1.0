package com.example.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.UnknownHostException

class UpdateChecker(
    private val repository: UpdateManifestRepository,
    private val versionManager: AppVersionManager
) {

    private val _state = MutableStateFlow<UpdateStatus>(UpdateStatus.IDLE)
    val state: StateFlow<UpdateStatus> = _state.asStateFlow()

    private val _latestVersionInfo = MutableStateFlow<AppVersionInfo?>(null)
    val latestVersionInfo: StateFlow<AppVersionInfo?> = _latestVersionInfo.asStateFlow()

    private val mutex = Mutex()
    private var lastCheckedTime = 0L
    private val cacheDurationMs = 5 * 60 * 1000 // 5 minutes cache

    suspend fun checkForUpdates(force: Boolean = false): UpdateStatus {
        if (_state.value == UpdateStatus.CHECKING) {
            return UpdateStatus.CHECKING
        }

        mutex.withLock {
            val now = System.currentTimeMillis()
            if (!force && lastCheckedTime > 0 && (now - lastCheckedTime) < cacheDurationMs) {
                // Return cached state if valid
                val currentInfo = _latestVersionInfo.value
                if (currentInfo != null) {
                    val status = versionManager.checkUpdateStatus(currentInfo)
                    _state.value = status
                    return status
                }
            }

            _state.value = UpdateStatus.CHECKING
            val result = repository.fetchUpdateManifest()

            result.fold(
                onSuccess = { info ->
                    _latestVersionInfo.value = info
                    val status = versionManager.checkUpdateStatus(info)
                    _state.value = status
                    lastCheckedTime = System.currentTimeMillis()
                    return status
                },
                onFailure = { throwable ->
                    if (throwable is UnknownHostException) {
                        // Offline state: keep last known info if exists, otherwise fallback to IDLE/UP_TO_DATE
                        val currentInfo = _latestVersionInfo.value
                        if (currentInfo != null) {
                            val status = versionManager.checkUpdateStatus(currentInfo)
                            _state.value = status
                            return status
                        } else {
                            _state.value = UpdateStatus.UP_TO_DATE // Don't block screen offline
                            return UpdateStatus.UP_TO_DATE
                        }
                    } else {
                        _state.value = UpdateStatus.ERROR
                        return UpdateStatus.ERROR
                    }
                }
            )
        }
    }
    
    fun reset() {
        _state.value = UpdateStatus.IDLE
        _latestVersionInfo.value = null
        lastCheckedTime = 0L
    }
}
