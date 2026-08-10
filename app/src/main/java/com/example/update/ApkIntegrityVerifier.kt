package com.example.update

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object ApkIntegrityVerifier {
    private const val TAG = "ApkIntegrityVerifier"

    fun verifySha256(file: File, expectedSha256: String): Boolean {
        if (!file.exists() || expectedSha256.isBlank()) return false
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { input ->
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            val sha256 = digest.digest().joinToString("") { "%02x".format(it) }
            sha256.equals(expectedSha256, ignoreCase = true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute SHA-256", e)
            false
        }
    }

    fun verifySignatureAndPackage(context: Context, file: File): Boolean {
        if (!file.exists()) {
            Log.e(TAG, "File does not exist: ${file.absolutePath}")
            return false
        }
        val pm = context.packageManager

        // 1. Get package name and signature from downloaded APK
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }

        val archiveInfo = try {
            pm.getPackageArchiveInfo(file.absolutePath, flags)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse archive info", e)
            null
        } ?: return false

        // Verify package name
        val archivePackageName = archiveInfo.packageName
        if (archivePackageName != context.packageName) {
            Log.e(TAG, "Package name mismatch: $archivePackageName != ${context.packageName}")
            return false
        }

        // 2. Get currently installed app signatures
        val installedSignatures = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                val signers = info.signingInfo?.apkContentsSigners
                if (signers != null && signers.isNotEmpty()) {
                    signers
                } else {
                    // Fallback to older signatures if needed, since Robolectric or some platforms don't populate signingInfo
                    @Suppress("DEPRECATION")
                    val fallbackInfo = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                    @Suppress("DEPRECATION")
                    fallbackInfo?.signatures
                }
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
                @Suppress("DEPRECATION")
                info.signatures
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get installed signatures", e)
            return false
        }

        if (installedSignatures == null || installedSignatures.isEmpty()) {
            Log.e(TAG, "No installed signatures found")
            return false
        }

        // 3. Get archive signatures
        val archiveSignatures = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signers = archiveInfo.signingInfo?.apkContentsSigners
                if (signers != null && signers.isNotEmpty()) {
                    signers
                } else {
                    // Fallback to older signatures if needed, since Robolectric or some platforms don't populate signingInfo
                    @Suppress("DEPRECATION")
                    val fallbackInfo = pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_SIGNATURES)
                    @Suppress("DEPRECATION")
                    fallbackInfo?.signatures
                }
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.signatures
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get archive signatures", e)
            return false
        }

        if (archiveSignatures == null || archiveSignatures.isEmpty()) {
            Log.e(TAG, "No signatures found in downloaded APK")
            return false
        }

        // Compare signatures
        for (archiveSig in archiveSignatures) {
            var matchFound = false
            for (installedSig in installedSignatures) {
                if (archiveSig.toByteArray().contentEquals(installedSig.toByteArray())) {
                    matchFound = true
                    break
                }
            }
            if (!matchFound) {
                Log.e(TAG, "Signature mismatch detected")
                return false
            }
        }

        return true
    }
}
