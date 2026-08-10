package com.example.ui.components

import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

object TextAnnotator {
    @Composable
    fun AnnotatedClickableText(
        text: String,
        modifier: Modifier = Modifier,
        style: TextStyle = TextStyle.Default,
        hashtagColor: Color = Color(0xFF00FF85),
        mentionColor: Color = Color(0xFFE040FB),
        onHashtagClick: (String) -> Unit,
        onMentionClick: (String) -> Unit
    ) {
        val annotatedString = buildAnnotatedString {
            val words = text.split(Regex("(?<=\\s)|(?=\\s)"))
            var currentIndex = 0
            
            for (word in words) {
                when {
                    word.startsWith("#") && word.length > 1 -> {
                        val tag = word.substring(1).filter { it.isLetterOrDigit() || it == '_' }
                        pushStringAnnotation(tag = "HASHTAG", annotation = tag)
                        withStyle(style = SpanStyle(color = hashtagColor, fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                        pop()
                    }
                    word.startsWith("@") && word.length > 1 -> {
                        val username = word.substring(1).filter { it.isLetterOrDigit() || it == '_' || it == '.' }
                        pushStringAnnotation(tag = "MENTION", annotation = username)
                        withStyle(style = SpanStyle(color = mentionColor, fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                        pop()
                    }
                    else -> {
                        append(word)
                    }
                }
                currentIndex += word.length
            }
        }

        ClickableText(
            text = annotatedString,
            modifier = modifier,
            style = style,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "HASHTAG", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        onHashtagClick(annotation.item)
                    }
                annotatedString.getStringAnnotations(tag = "MENTION", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        onMentionClick(annotation.item)
                    }
            }
        )
    }
}
