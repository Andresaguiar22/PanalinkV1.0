package com.example.ui.components.chat.emoji

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.BreakIterator

import com.example.ui.chat.emoji.AnimatedEmojiContent
import com.example.ui.chat.emoji.AnimatedEmojiResolver
import com.example.ui.chat.emoji.intelligent.RenderIntelligentEmojiAnimation

object EmojiHelper {
    /**
     * Extracts individual grapheme clusters (user-perceived characters/emojis) from text.
     */
    fun extractGraphemes(text: String): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        val list = mutableListOf<String>()
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(trimmed)
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val grapheme = trimmed.substring(start, end)
            if (grapheme.isNotBlank()) {
                list.add(grapheme)
            }
            start = end
            end = iterator.next()
        }
        return list
    }

    /**
     * Checks if a string contains ONLY emojis (1 to 8 emojis, no regular alphanumeric words).
     */
    fun isEmojiOnly(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        val graphemes = extractGraphemes(trimmed)
        if (graphemes.isEmpty() || graphemes.size > 8) return false

        return graphemes.all { grapheme ->
            val hasEmojiChar = grapheme.any { char ->
                val cp = char.code
                cp in 0x1F000..0x1FFFF || 
                cp in 0x2600..0x27BF || 
                cp in 0x2300..0x23FF || 
                cp in 0x2B00..0x2BFF ||
                cp in 0xFE00..0xFE0F ||
                cp == 0x200D ||
                (cp > 127 && !Character.isLetterOrDigit(char))
            }
            val hasAsciiLetterOrDigit = grapheme.any { char ->
                char.code < 128 && Character.isLetterOrDigit(char)
            }
            hasEmojiChar && !hasAsciiLetterOrDigit
        }
    }
}

enum class EmojiAnimType {
    HEARTBEAT,
    BOUNCE_JOY,
    FIRE_PULSE,
    WAVE_SWAY,
    POP_PULSE,
    GENERIC_FLOAT
}

@Composable
fun AnimatedEmojiEngine(
    text: String,
    modifier: Modifier = Modifier
) {
    val graphemes = remember(text) { EmojiHelper.extractGraphemes(text) }
    if (graphemes.isEmpty()) return

    val fontSize: TextUnit = when (graphemes.size) {
        1 -> 68.sp
        2 -> 52.sp
        3 -> 44.sp
        in 4..5 -> 36.sp
        else -> 30.sp
    }

    Row(
        modifier = modifier.padding(vertical = 4.dp, horizontal = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        graphemes.forEachIndexed { index, emojiStr ->
            SingleAnimatedEmoji(
                emoji = emojiStr,
                fontSize = fontSize,
                indexOffset = index
            )
        }
    }
}

@Composable
private fun SingleAnimatedEmoji(
    emoji: String,
    fontSize: TextUnit,
    indexOffset: Int
) {
    val intelligentMeaning = remember(emoji) {
        AnimatedEmojiResolver.resolveIntelligent(emoji)
    }

    if (intelligentMeaning != null) {
        RenderIntelligentEmojiAnimation(
            emoji = emoji,
            animation = intelligentMeaning.animation,
            fontSize = fontSize
        )
    } else {
        val animType = remember(emoji) {
            AnimatedEmojiResolver.resolve(emoji)
        }

        if (animType != null) {
            AnimatedEmojiContent(
                emoji = emoji,
                animationType = animType,
                fontSize = fontSize
            )
        } else {
            Text(
                text = emoji,
                fontSize = fontSize,
                textAlign = TextAlign.Center
            )
        }
    }
}
