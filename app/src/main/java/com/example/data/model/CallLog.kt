package com.example.data.model

import com.squareup.moshi.JsonClass

enum class CallLogType {
    VOICE, VIDEO
}

enum class CallLogStatus {
    COMPLETED, MISSED, REJECTED, CANCELLED
}

@JsonClass(generateAdapter = true)
data class CallLog(
    val id: String = "",
    val callerId: String = "",
    val receiverId: String = "",
    val type: CallLogType = CallLogType.VOICE,
    val status: CallLogStatus = CallLogStatus.COMPLETED,
    val durationSeconds: Long = 0,
    val timestamp: String = ""
)
