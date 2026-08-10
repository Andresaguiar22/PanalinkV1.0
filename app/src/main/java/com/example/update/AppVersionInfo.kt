package com.example.update

data class AppVersionInfo(
    val versionCode: Long,
    val versionName: String,
    val downloadUrl: String,
    val changelog: List<String>,
    val mandatory: Boolean,
    val sha256: String,
    val minimumSupportedVersionCode: Long
)
