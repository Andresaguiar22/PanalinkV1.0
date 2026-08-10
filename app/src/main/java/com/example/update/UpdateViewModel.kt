package com.example.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    // Configure the default manifest URL using BACKEND_URL or GITHUB
    private val defaultManifestUrl = if (BuildConfig.DEBUG) {
        "${BuildConfig.BACKEND_URL}/updates/manifest.json"
    } else {
        val owner = try { BuildConfig.GITHUB_OWNER } catch (e: Exception) { "placeholder-owner" }
        val repo = try { BuildConfig.GITHUB_REPOSITORY } catch (e: Exception) { "placeholder-repo" }
        "https://github.com/$owner/$repo/releases/latest/download/manifest.json"
    }
    
    private val defaultProviderType = if (BuildConfig.DEBUG) {
        UpdateConfig.ProviderType.CUSTOM_SERVER
    } else {
        UpdateConfig.ProviderType.GITHUB_RELEASES
    }

    private val config = UpdateConfig(
        manifestUrl = defaultManifestUrl,
        providerType = defaultProviderType,
        githubOwner = try { BuildConfig.GITHUB_OWNER } catch (e: Exception) { "placeholder-owner" },
        githubRepo = try { BuildConfig.GITHUB_REPOSITORY } catch (e: Exception) { "placeholder-repo" },
        githubReleaseTag = try { BuildConfig.GITHUB_RELEASE_TAG } catch (e: Exception) { "v2.0.0" }
    )

    private val repository: UpdateManifestRepository = if (config.providerType == UpdateConfig.ProviderType.GITHUB_RELEASES) {
        GitHubUpdateManifestRepository(config)
    } else {
        HttpUpdateManifestRepository(config)
    }
    private val versionManager = AppVersionManager(context)
    private val checker = UpdateChecker(repository, versionManager)
    private val downloader = ApkDownloadManager(context)
    private val installer = AndroidPackageInstaller(context, versionManager)

    val updateStatus: StateFlow<UpdateStatus> = checker.state
    val latestVersionInfo: StateFlow<AppVersionInfo?> = checker.latestVersionInfo
    val downloadState: StateFlow<DownloadState> = downloader.downloadState

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun checkForUpdates(force: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _errorMessage.value = null
            checker.checkForUpdates(force)
        }
    }

    fun startDownloadAndInstall() {
        val info = latestVersionInfo.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _errorMessage.value = null
            val downloadedFile = downloader.downloadApk(
                downloadUrl = info.downloadUrl,
                expectedSha256 = info.sha256,
                versionCode = info.versionCode
            )

            if (downloadedFile != null) {
                val installResult = installer.installApk(downloadedFile, info.sha256)
                installResult.onFailure { throwable ->
                    _errorMessage.value = throwable.localizedMessage ?: "Installation failed"
                }
            } else {
                // If downloader failed, errorMessage will be handled via DownloadState.Error
                val currentState = downloader.downloadState.value
                if (currentState is DownloadState.Error) {
                    _errorMessage.value = currentState.message
                }
            }
        }
    }

    fun cancelDownload() {
        downloader.cancelDownload()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun getInstalledVersionName(): String {
        return versionManager.getCurrentVersionName()
    }

    fun getInstalledVersionCode(): Long {
        return versionManager.getCurrentVersionCode()
    }
}
