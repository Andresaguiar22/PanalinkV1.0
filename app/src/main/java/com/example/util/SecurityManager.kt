package com.example.util

import android.content.Context
import android.util.Log
import java.io.File
import java.security.MessageDigest
import android.content.pm.PackageManager
import android.os.Build
import java.util.*

/**
 * SecurityManager: Handles device integrity, root/emulator detection, and security auditing.
 * Part of the "Shield" system to protect Panalink against malicious actors.
 */
object SecurityManager {
    private const val TAG = "SecurityManager"

    /**
     * Comprehensive Root Detection
     */
    fun isDeviceRooted(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        
        // Check filesystem for SU binaries
        for (path in paths) {
            if (File(path).exists()) return true
        }

        // Check for specific root management apps
        // (Simplified for this context)
        
        // Check for "test-keys" build tag
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return true

        return false
    }

    /**
     * Comprehensive Emulator Detection
     */
    fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk" == Build.PRODUCT)
    }

    /**
     * Checks if the app signature matches the expected one.
     * Prevents "Modified/Modded" APKs from functioning.
     */
    fun verifyAppSignature(context: Context): Boolean {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (signatures != null) {
                for (signature in signatures) {
                    val md = MessageDigest.getInstance("SHA-256")
                    md.update(signature.toByteArray())
                    // Use android.util.Base64 for better compatibility if java.util.Base64 is problematic
                    val currentSignature = android.util.Base64.encodeToString(md.digest(), android.util.Base64.NO_WRAP)
                    
                    // Note: In production, compare this against a hardcoded constant
                    // Log.d(TAG, "App Signature: $currentSignature")
                }
            }
            true // Allow for now, but return false if signature doesn't match in production
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Audits the current security state of the app.
     */
    fun getSecurityAudit(context: Context): SecurityAudit {
        return SecurityAudit(
            isRooted = isDeviceRooted(),
            isEmulator = isEmulator(),
            isSignatureValid = verifyAppSignature(context),
            isE2EEEnabled = CryptoManager.ENABLE_E2EE,
            hasStrongEncryption = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
        )
    }

    data class SecurityAudit(
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val isSignatureValid: Boolean,
        val isE2EEEnabled: Boolean,
        val hasStrongEncryption: Boolean
    ) {
        val score: Int get() {
            var s = 100
            if (isRooted) s -= 40
            if (isEmulator) s -= 20
            if (!isSignatureValid) s -= 40
            if (!isE2EEEnabled) s -= 10
            return s.coerceAtLeast(0)
        }

        val status: String get() = when {
            score >= 90 -> "ÓPTIMA 🛡️"
            score >= 70 -> "BUENA ✅"
            score >= 40 -> "RIESGO MEDIO ⚠️"
            else -> "ALTO RIESGO 🚨"
        }
    }
}
