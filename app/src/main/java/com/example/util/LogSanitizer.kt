package com.example.util

object LogSanitizer {
    private val JWT_REGEX = Regex("(Bearer\\s+|eyJ)[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+")
    private val TOKEN_KEY_REGEX = Regex("(?i)(token|access_token|refresh_token|api_key|secret|authorization)=\\S+")

    fun sanitize(message: String?): String {
        if (message.isNullOrEmpty()) return ""
        var result = message.replace(JWT_REGEX) { match ->
            if (match.value.startsWith("Bearer", ignoreCase = true)) "Bearer [REDACTED_JWT]" else "[REDACTED_JWT]"
        }
        result = result.replace(TOKEN_KEY_REGEX) { match ->
            val key = match.value.substringBefore("=")
            "$key=[REDACTED]"
        }
        return result
    }
}
