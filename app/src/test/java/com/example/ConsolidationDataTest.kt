package com.example

import com.example.data.database.PendingSocialActionEntity
import com.example.util.LogSanitizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ConsolidationDataTest {

    @Test
    fun testLogSanitizerRedactsBearerAndJwt() {
        val input = "Header: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature token=secret_token_123"
        val sanitized = LogSanitizer.sanitize(input)

        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9"))
        assertFalse(sanitized.contains("secret_token_123"))
        assertEquals("Header: Bearer [REDACTED_JWT] token=[REDACTED]", sanitized)
    }

    @Test
    fun testLogSanitizerHandlesNullOrEmpty() {
        assertEquals("", LogSanitizer.sanitize(null))
        assertEquals("", LogSanitizer.sanitize(""))
    }

    private fun action(
        id: String,
        targetId: String,
        actionType: String,
        userId: String = "user_456",
        isReel: Boolean = true
    ) = PendingSocialActionEntity(
        localActionId = id,
        userId = userId,
        targetId = targetId,
        actionType = actionType,
        payload = null,
        isReel = isReel
    )

    // Regression contract for the v42 queue: this intentionally fails until
    // LIKE/UNLIKE is converted from event persistence to desired-state persistence.
    @Test
    fun current_behavior_does_not_collapse_interactions() {
        val targetId = "reel_A"

        val actions = listOf(
            action("1", targetId, "LIKE"),
            action("2", targetId, "UNLIKE"),
            action("3", targetId, "LIKE"),
            action("4", targetId, "UNLIKE")
        )

        // The v42 DAO currently persists each action as a distinct row because
        // localActionId is the primary key. The desired post-refactor contract
        // is one logical LIKE-family intent for this target.
        assertEquals(1, actions.size)
    }
}
