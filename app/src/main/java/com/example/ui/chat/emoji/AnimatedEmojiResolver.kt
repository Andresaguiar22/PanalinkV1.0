package com.example.ui.chat.emoji

import com.example.ui.chat.emoji.intelligent.EmojiMeaning
import com.example.ui.chat.emoji.intelligent.EmojiMeaningRegistry

object AnimatedEmojiResolver {
    fun resolve(emoji: String): EmojiAnimationType? {
        return AnimatedEmojiRegistry.getAnimationType(emoji)
    }

    fun resolveIntelligent(emoji: String): EmojiMeaning? {
        return EmojiMeaningRegistry.findMeaning(emoji)
    }
}
