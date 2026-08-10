package com.example.media.quality

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

enum class QualityLevel {
    FULL_1080P,
    BALANCED_720P,
    SAVER_480P,
    OFFLINE_ONLY
}

object MediaQualityManager {

    fun getRecommendedQuality(context: Context): QualityLevel {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return QualityLevel.OFFLINE_ONLY

        val network = connectivityManager.activeNetwork ?: return QualityLevel.OFFLINE_ONLY
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return QualityLevel.OFFLINE_ONLY

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> QualityLevel.FULL_1080P

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
                    QualityLevel.FULL_1080P
                } else {
                    QualityLevel.BALANCED_720P
                }
            }

            else -> QualityLevel.SAVER_480P
        }
    }

    fun applyQualityToUrl(url: String, quality: QualityLevel): String {
        if (url.isBlank()) return url
        return when (quality) {
            QualityLevel.SAVER_480P -> if (url.contains("?")) "$url&res=480" else "$url?res=480"
            QualityLevel.BALANCED_720P -> if (url.contains("?")) "$url&res=720" else "$url?res=720"
            QualityLevel.FULL_1080P, QualityLevel.OFFLINE_ONLY -> url
        }
    }
}
