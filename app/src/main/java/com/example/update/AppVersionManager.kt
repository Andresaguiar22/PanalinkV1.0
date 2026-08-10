package com.example.update

import android.content.Context
import android.os.Build

open class AppVersionManager(private val context: Context) {

    open fun getCurrentVersionCode(): Long {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }
    }

    open fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    fun checkUpdateStatus(remote: AppVersionInfo): UpdateStatus {
        val currentCode = getCurrentVersionCode()
        
        // Prevent downgrade
        if (remote.versionCode <= currentCode) {
            return UpdateStatus.UP_TO_DATE
        }
        
        // If current version is less than the minimum supported version code, it's a mandatory update
        if (currentCode < remote.minimumSupportedVersionCode) {
            return UpdateStatus.MANDATORY_UPDATE
        }
        
        return if (remote.mandatory) {
            UpdateStatus.MANDATORY_UPDATE
        } else {
            UpdateStatus.UPDATE_AVAILABLE
        }
    }
}

enum class UpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    MANDATORY_UPDATE,
    ERROR
}
