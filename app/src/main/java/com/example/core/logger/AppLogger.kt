package com.example.core.logger

import android.util.Log

object AppLogger {
    private const val TAG = "PanaLinkCore"
    private var isDebug: Boolean = true

    fun init(debugMode: Boolean) {
        isDebug = debugMode
    }

    fun d(tag: String = TAG, message: String) {
        if (isDebug) {
            Log.d(tag, sanitize(message))
        }
    }

    fun i(tag: String = TAG, message: String) {
        if (isDebug) {
            Log.i(tag, sanitize(message))
        }
    }

    fun w(tag: String = TAG, message: String) {
        if (isDebug) {
            Log.w(tag, sanitize(message))
        }
    }

    fun e(tag: String = TAG, message: String, throwable: Throwable? = null) {
        Log.e(tag, sanitize(message), throwable)
    }

    private fun sanitize(message: String): String {
        return message.replace(Regex("(?i)(bearer|token|password|secret)=[^&\\s]+"), "$1=REDACTED")
    }
}
