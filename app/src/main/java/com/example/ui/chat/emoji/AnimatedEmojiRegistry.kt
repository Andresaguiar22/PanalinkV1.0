package com.example.ui.chat.emoji

object AnimatedEmojiRegistry {
    private val emojiMap: Map<String, EmojiAnimationType> = mapOf(
        "❤️" to EmojiAnimationType.HEART_BEAT,
        "💖" to EmojiAnimationType.HEART_BEAT,
        "💕" to EmojiAnimationType.HEART_BEAT,
        "💓" to EmojiAnimationType.HEART_BEAT,
        "💗" to EmojiAnimationType.HEART_BEAT,
        "💘" to EmojiAnimationType.HEART_BEAT,
        "💝" to EmojiAnimationType.HEART_BEAT,
        "💞" to EmojiAnimationType.HEART_BEAT,
        "❣️" to EmojiAnimationType.HEART_BEAT,
        "💌" to EmojiAnimationType.HEART_BEAT,
        "😍" to EmojiAnimationType.HEART_BEAT,
        "😘" to EmojiAnimationType.KISS_THROW,
        "🥰" to EmojiAnimationType.KISS_THROW,
        "😗" to EmojiAnimationType.KISS_THROW,
        "😙" to EmojiAnimationType.KISS_THROW,
        "😚" to EmojiAnimationType.KISS_THROW,
        "😂" to EmojiAnimationType.LAUGH_SHAKE,
        "🤣" to EmojiAnimationType.LAUGH_SHAKE,
        "😆" to EmojiAnimationType.LAUGH_SHAKE,
        "🤪" to EmojiAnimationType.LAUGH_SHAKE,
        "😜" to EmojiAnimationType.LAUGH_SHAKE,
        "🔥" to EmojiAnimationType.FIRE_EFFECT,
        "💥" to EmojiAnimationType.FIRE_EFFECT,
        "⚡" to EmojiAnimationType.FIRE_EFFECT,
        "🚀" to EmojiAnimationType.FIRE_EFFECT,
        "👍" to EmojiAnimationType.BOUNCE_EFFECT,
        "👎" to EmojiAnimationType.BOUNCE_EFFECT,
        "👏" to EmojiAnimationType.BOUNCE_EFFECT,
        "🙌" to EmojiAnimationType.BOUNCE_EFFECT
    )

    fun getAnimationType(emoji: String): EmojiAnimationType? {
        val trimmed = emoji.trim()
        if (trimmed.isEmpty()) return null
        return emojiMap[trimmed] ?: emojiMap.entries.firstOrNull { trimmed.contains(it.key) }?.value
    }
}
