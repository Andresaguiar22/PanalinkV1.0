package com.example.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

interface UpdateManifestRepository {
    suspend fun fetchUpdateManifest(): Result<AppVersionInfo>
}

class HttpUpdateManifestRepository(
    private val config: UpdateConfig,
    private val okHttpClient: OkHttpClient = OkHttpClient()
) : UpdateManifestRepository {

    override suspend fun fetchUpdateManifest(): Result<AppVersionInfo> = withContext(Dispatchers.IO) {
        if (config.manifestUrl.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Manifest URL is empty"))
        }

        if (config.isProduction && !config.manifestUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext Result.failure(SecurityException("Production manifest URL must use HTTPS: ${config.manifestUrl}"))
        }

        val request = Request.Builder()
            .url(config.manifestUrl)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("Server returned error code: ${response.code}"))
                }

                val bodyString = response.body?.string()
                    ?: return@withContext Result.failure(IOException("Empty response body"))

                val json = JSONObject(bodyString)
                val versionCode = json.getLong("versionCode")
                if (versionCode <= 0) {
                    return@withContext Result.failure(IllegalArgumentException("Invalid versionCode in manifest: $versionCode"))
                }
                
                val versionName = json.getString("versionName")
                if (versionName.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("versionName is empty"))
                }

                val downloadUrl = json.getString("downloadUrl")
                
                if (downloadUrl.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Download URL is empty"))
                }

                // Enforce HTTPS for download url in production
                if (config.isProduction && !downloadUrl.startsWith("https://", ignoreCase = true)) {
                    return@withContext Result.failure(SecurityException("Production download URL must use HTTPS: $downloadUrl"))
                }
                
                val sha256 = json.getString("sha256")
                if (sha256.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("SHA-256 hash is empty"))
                }

                // Enforce valid SHA-256 format (64 hex characters)
                if (!sha256.matches(Regex("^[0-9a-fA-F]{64}$"))) {
                    return@withContext Result.failure(IllegalArgumentException("Invalid SHA-256 hex format: $sha256"))
                }

                val mandatory = json.optBoolean("mandatory", false)
                val minimumSupportedVersionCode = json.optLong("minimumSupportedVersionCode", 0L)

                val changelogArray = json.optJSONArray("changelog") ?: JSONArray()
                val changelogList = mutableListOf<String>()
                for (i in 0 until changelogArray.length()) {
                    changelogList.add(changelogArray.getString(i))
                }

                val info = AppVersionInfo(
                    versionCode = versionCode,
                    versionName = versionName,
                    downloadUrl = downloadUrl,
                    changelog = changelogList,
                    mandatory = mandatory,
                    sha256 = sha256,
                    minimumSupportedVersionCode = minimumSupportedVersionCode
                )
                Result.success(info)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
