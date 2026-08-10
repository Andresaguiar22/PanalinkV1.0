package com.example

import com.example.ui.chat.emoji.AnimatedEmojiResolver
import com.example.ui.chat.emoji.intelligent.EmojiCategory
import com.example.ui.chat.emoji.intelligent.EmojiMeaningRegistry
import com.example.ui.chat.emoji.intelligent.EmojiPremiumAnimation
import org.junit.Assert.*
import org.junit.Test

class IntelligentEmojiTest {

    @Test
    fun testGesturesCategoryResolution() {
        val wave = AnimatedEmojiResolver.resolveIntelligent("👋")
        assertNotNull(wave)
        assertEquals(EmojiCategory.GESTURE, wave?.category)
        assertEquals(EmojiPremiumAnimation.WAVE_GESTURE, wave?.animation)

        val thumbsUp = AnimatedEmojiResolver.resolveIntelligent("👍")
        assertNotNull(thumbsUp)
        assertEquals(EmojiCategory.GESTURE, thumbsUp?.category)
        assertEquals(EmojiPremiumAnimation.THUMBS_UP_BOUNCE, thumbsUp?.animation)
    }

    @Test
    fun testEmotionsCategoryResolution() {
        val laugh = AnimatedEmojiResolver.resolveIntelligent("😂")
        assertNotNull(laugh)
        assertEquals(EmojiCategory.EMOTION, laugh?.category)
        assertEquals(EmojiPremiumAnimation.LAUGH_VIBRATE, laugh?.animation)

        val cry = AnimatedEmojiResolver.resolveIntelligent("😭")
        assertNotNull(cry)
        assertEquals(EmojiCategory.EMOTION, cry?.category)
        assertEquals(EmojiPremiumAnimation.CRY_TEARS_FLOAT, cry?.animation)
    }

    @Test
    fun testAnimalsCategoryResolution() {
        val dog = AnimatedEmojiResolver.resolveIntelligent("🐶")
        assertNotNull(dog)
        assertEquals(EmojiCategory.ANIMAL, dog?.category)
        assertEquals(EmojiPremiumAnimation.DOG_WAG, dog?.animation)

        val cat = AnimatedEmojiResolver.resolveIntelligent("🐱")
        assertNotNull(cat)
        assertEquals(EmojiCategory.ANIMAL, cat?.category)
        assertEquals(EmojiPremiumAnimation.CAT_PURR, cat?.animation)
    }

    @Test
    fun testObjectsCategoryResolution() {
        val rocket = AnimatedEmojiResolver.resolveIntelligent("🚀")
        assertNotNull(rocket)
        assertEquals(EmojiCategory.OBJECT, rocket?.category)
        assertEquals(EmojiPremiumAnimation.ROCKET_LAUNCH, rocket?.animation)

        val fire = AnimatedEmojiResolver.resolveIntelligent("🔥")
        assertNotNull(fire)
        assertEquals(EmojiCategory.OBJECT, fire?.category)
        assertEquals(EmojiPremiumAnimation.FIRE_ORGANIC, fire?.animation)
    }

    @Test
    fun testUnsupportedEmojiReturnsNull() {
        val alien = AnimatedEmojiResolver.resolveIntelligent("🪐")
        assertNull(alien)
    }
}
