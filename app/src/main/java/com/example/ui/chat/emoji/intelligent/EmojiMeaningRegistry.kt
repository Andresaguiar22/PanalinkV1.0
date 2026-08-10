package com.example.ui.chat.emoji.intelligent

data class EmojiMeaning(
    val emoji: String,
    val category: EmojiCategory,
    val animation: EmojiPremiumAnimation,
    val description: String
)

object EmojiMeaningRegistry {
    private val meanings: List<EmojiMeaning> = listOf(
        // Gestos
        EmojiMeaning("👋", EmojiCategory.GESTURE, EmojiPremiumAnimation.WAVE_GESTURE, "Saludo / Despedida"),
        EmojiMeaning("🙋", EmojiCategory.GESTURE, EmojiPremiumAnimation.WAVE_GESTURE, "Mano levantada"),
        EmojiMeaning("🙋‍♂️", EmojiCategory.GESTURE, EmojiPremiumAnimation.WAVE_GESTURE, "Hombre levantando mano"),
        EmojiMeaning("🙋‍♀️", EmojiCategory.GESTURE, EmojiPremiumAnimation.WAVE_GESTURE, "Mujer levantando mano"),
        EmojiMeaning("👍", EmojiCategory.GESTURE, EmojiPremiumAnimation.THUMBS_UP_BOUNCE, "Aprobación / Pulgar arriba"),
        EmojiMeaning("👎", EmojiCategory.GESTURE, EmojiPremiumAnimation.THUMBS_DOWN_DROP, "Desaprobación / Pulgar abajo"),
        EmojiMeaning("👏", EmojiCategory.GESTURE, EmojiPremiumAnimation.CLAP_RHYTHM, "Aplauso / Felicitación"),
        EmojiMeaning("🙌", EmojiCategory.GESTURE, EmojiPremiumAnimation.CLAP_RHYTHM, "Manos arriba / Celebración"),
        EmojiMeaning("🙏", EmojiCategory.GESTURE, EmojiPremiumAnimation.PRAY_SWAY, "Por favor / Gracias / Oración"),
        EmojiMeaning("👌", EmojiCategory.GESTURE, EmojiPremiumAnimation.OK_CONFIRM, "OK / Perfecto"),

        // Emociones
        EmojiMeaning("😂", EmojiCategory.EMOTION, EmojiPremiumAnimation.LAUGH_VIBRATE, "Risa con lágrimas"),
        EmojiMeaning("🤣", EmojiCategory.EMOTION, EmojiPremiumAnimation.LAUGH_VIBRATE, "Risa en el suelo"),
        EmojiMeaning("😆", EmojiCategory.EMOTION, EmojiPremiumAnimation.LAUGH_VIBRATE, "Risa guiñando ojos"),
        EmojiMeaning("😭", EmojiCategory.EMOTION, EmojiPremiumAnimation.CRY_TEARS_FLOAT, "Llanto desconsolado"),
        EmojiMeaning("🥺", EmojiCategory.EMOTION, EmojiPremiumAnimation.CRY_TEARS_FLOAT, "Ojos suplicantes"),
        EmojiMeaning("😍", EmojiCategory.EMOTION, EmojiPremiumAnimation.LOVE_HEART_BEAT, "Ojos de corazón"),
        EmojiMeaning("❤️", EmojiCategory.EMOTION, EmojiPremiumAnimation.LOVE_HEART_BEAT, "Corazón rojo"),
        EmojiMeaning("💖", EmojiCategory.EMOTION, EmojiPremiumAnimation.LOVE_HEART_BEAT, "Corazón con destellos"),
        EmojiMeaning("💕", EmojiCategory.EMOTION, EmojiPremiumAnimation.LOVE_HEART_BEAT, "Dos corazones"),
        EmojiMeaning("😘", EmojiCategory.EMOTION, EmojiPremiumAnimation.LOVE_HEART_BEAT, "Beso volado"),
        EmojiMeaning("🥰", EmojiCategory.EMOTION, EmojiPremiumAnimation.LOVE_HEART_BEAT, "Cara con corazones"),
        EmojiMeaning("😡", EmojiCategory.EMOTION, EmojiPremiumAnimation.ANGRY_SHAKE, "Cara muy enojada"),
        EmojiMeaning("🤬", EmojiCategory.EMOTION, EmojiPremiumAnimation.ANGRY_SHAKE, "Cara con símbolos"),
        EmojiMeaning("👿", EmojiCategory.EMOTION, EmojiPremiumAnimation.ANGRY_SHAKE, "Diablillo enojado"),
        EmojiMeaning("😴", EmojiCategory.EMOTION, EmojiPremiumAnimation.SLEEP_FLOAT, "Cara durmiendo"),
        EmojiMeaning("💤", EmojiCategory.EMOTION, EmojiPremiumAnimation.SLEEP_FLOAT, "Símbolo de sueño"),

        // Animales
        EmojiMeaning("🐶", EmojiCategory.ANIMAL, EmojiPremiumAnimation.DOG_WAG, "Cara de perro"),
        EmojiMeaning("🐕", EmojiCategory.ANIMAL, EmojiPremiumAnimation.DOG_WAG, "Perro"),
        EmojiMeaning("🐱", EmojiCategory.ANIMAL, EmojiPremiumAnimation.CAT_PURR, "Cara de gato"),
        EmojiMeaning("🐈", EmojiCategory.ANIMAL, EmojiPremiumAnimation.CAT_PURR, "Gato"),
        EmojiMeaning("🐸", EmojiCategory.ANIMAL, EmojiPremiumAnimation.FROG_HOP, "Rana"),

        // Objetos y Acciones
        EmojiMeaning("🚗", EmojiCategory.OBJECT, EmojiPremiumAnimation.CAR_DRIVE, "Automóvil"),
        EmojiMeaning("🚘", EmojiCategory.OBJECT, EmojiPremiumAnimation.CAR_DRIVE, "Automóvil de frente"),
        EmojiMeaning("✈️", EmojiCategory.OBJECT, EmojiPremiumAnimation.PLANE_FLY, "Avión"),
        EmojiMeaning("🛫", EmojiCategory.OBJECT, EmojiPremiumAnimation.PLANE_FLY, "Avión despegando"),
        EmojiMeaning("🚀", EmojiCategory.OBJECT, EmojiPremiumAnimation.ROCKET_LAUNCH, "Cohete espacia"),
        EmojiMeaning("⚽", EmojiCategory.OBJECT, EmojiPremiumAnimation.BALL_BOUNCE, "Pelota de fútbol"),
        EmojiMeaning("🏀", EmojiCategory.OBJECT, EmojiPremiumAnimation.BALL_BOUNCE, "Pelota de baloncesto"),
        EmojiMeaning("🎂", EmojiCategory.OBJECT, EmojiPremiumAnimation.PARTY_BURST, "Pastel de cumpleaños"),
        EmojiMeaning("🎉", EmojiCategory.OBJECT, EmojiPremiumAnimation.PARTY_BURST, "Cornucopia de fiesta"),
        EmojiMeaning("🎊", EmojiCategory.OBJECT, EmojiPremiumAnimation.PARTY_BURST, "Bola de confeti"),
        EmojiMeaning("🔥", EmojiCategory.OBJECT, EmojiPremiumAnimation.FIRE_ORGANIC, "Fuego / Llama"),
        EmojiMeaning("💥", EmojiCategory.OBJECT, EmojiPremiumAnimation.FIRE_ORGANIC, "Colisión / Explosión")
    )

    private val emojiToMeaningMap: Map<String, EmojiMeaning> = meanings.associateBy { it.emoji }

    fun findMeaning(emoji: String): EmojiMeaning? {
        val trimmed = emoji.trim()
        if (trimmed.isEmpty()) return null
        return emojiToMeaningMap[trimmed] ?: meanings.firstOrNull { trimmed.contains(it.emoji) }
    }

    fun getAllSupportedMeanings(): List<EmojiMeaning> = meanings
}
