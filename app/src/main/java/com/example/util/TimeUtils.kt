package com.example.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    private const val TAG = "TimeUtils"

    // Patterns ordered from most specific to least specific
    private val patterns = arrayOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )

    fun parseToEpochMilli(ts: String?): Long {
        if (ts.isNullOrEmpty()) return 0L
        
        // Quick check if it's already a numeric timestamp
        if (ts.all { it.isDigit() }) {
            return try { ts.toLong() } catch (e: Exception) { 0L }
        }

        val cleanedTs = ts.replace(" ", "T")
        
        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                if (pattern.endsWith("Z")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                val date = sdf.parse(cleanedTs)
                if (date != null) return date.time
            } catch (e: Exception) {
                // Try next pattern
            }
        }
        
        Log.w(TAG, "Failed to parse timestamp with any known pattern: $ts")
        return 0L
    }

    fun getNowIsoString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
