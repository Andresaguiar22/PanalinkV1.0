package com.example.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

class AndroidPackageInstaller(
    private val context: Context,
    private val versionManager: AppVersionManager
) {
    companion object {
        private const val TAG = "AndroidPackageInstaller"
    }

    fun installApk(file: File, expectedSha256: String): Result<Unit> {
        // 1. Verify existence of the APK
        if (!file.exists()) {
            return Result.failure(IllegalStateException("APK file does not exist"))
        }

        // 2. Verify SHA-256
        if (!ApkIntegrityVerifier.verifySha256(file, expectedSha256)) {
            return Result.failure(SecurityException("APK SHA-256 hash does not match expected signature"))
        }

        // 3 & 4. Verify packageName and cryptographic signature
        if (!ApkIntegrityVerifier.verifySignatureAndPackage(context, file)) {
            return Result.failure(SecurityException("APK package name or cryptographic signature verification failed"))
        }

        // 5 & 6. Verify versionCode and reject downgrade
        try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(file.absolutePath, 0)
                ?: return Result.failure(IllegalStateException("Unable to parse APK manifest details"))

            val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            val currentVersionCode = versionManager.getCurrentVersionCode()
            if (apkVersionCode <= currentVersionCode) {
                return Result.failure(IllegalArgumentException("Attempting to downgrade or install identical version code ($apkVersionCode <= $currentVersionCode)"))
            }

            // 7. Only then generate FileProvider URI
            val authority = "${context.packageName}.provider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, file)

            // 8. Only then launch Intent of installation
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Installation flow error", e)
            return Result.failure(e)
        }
    }
}
