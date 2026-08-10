package com.example

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
}
