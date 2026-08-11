package com.example.util

import android.util.Log

import com.example.util.TimeUtils

object MessageFilter {
    private const val TAG = "MessageFilter"

    fun parseToEpochMilli(ts: String?): Long {
        return TimeUtils.parseToEpochMilli(ts)
    }

    /**
     * Determines whether a message should be inserted or kept.
     * Returns true if the message is valid (should be saved/displayed),
     * and false if it should be filtered out (ignored/deleted).
     */
    fun shouldKeepMessage(
        messageId: String?,
        messageClientUuid: String?,
        messageCreatedAt: String?,
        lastClearedAt: String?,
        deletedMessageIds: Set<String>
    ): Boolean {
        if (messageId != null && deletedMessageIds.contains(messageId)) {
            Log.d(TAG, "Filtering message because it is marked as deleted by user (ID: $messageId)")
            return false
        }
        if (messageClientUuid != null && deletedMessageIds.contains(messageClientUuid)) {
            Log.d(TAG, "Filtering message because it is marked as deleted by user (UUID: $messageClientUuid)")
            return false
        }

        if (!lastClearedAt.isNullOrEmpty() && !messageCreatedAt.isNullOrEmpty()) {
            val msgEpoch = parseToEpochMilli(messageCreatedAt)
            val clearEpoch = parseToEpochMilli(lastClearedAt)
            if (msgEpoch > 0L && clearEpoch > 0L) {
                if (msgEpoch <= clearEpoch) {
                    Log.d(TAG, "Filtering message because it was created before or at lastClearedAt (Msg: $msgEpoch, Clear: $clearEpoch)")
                    return false
                }
            } else {
                // Fallback to lexicographical comparison if epoch parsing failed
                if (messageCreatedAt <= lastClearedAt) {
                    Log.d(TAG, "Filtering message using fallback comparison (Msg: $messageCreatedAt, Clear: $lastClearedAt)")
                    return false
                }
            }
        }

        return true
    }
}
