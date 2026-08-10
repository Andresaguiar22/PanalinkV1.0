package com.example.util

import java.util.regex.Pattern

object YouTubeUrlParser {
    fun extractYouTubeVideoId(text: String): String? {
        val pattern = "(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|\\/v\\/|\\/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F|shorts\\/)[^#\\&\\?\\n]*"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(text)
        
        if (matcher.find()) {
            return matcher.group()
        }
        return null
    }

    fun extractYouTubeUrl(text: String): String? {
        val pattern = "(https?:\\/\\/)?(www\\.)?(youtube\\.com|youtu\\.?be)\\/.+"
        val compiledPattern = Pattern.compile(pattern)
        val matcher = compiledPattern.matcher(text)
        if (matcher.find()) {
            val url = matcher.group()
            return url.split(" ", "\\n").firstOrNull()
        }
        return null
    }
}
