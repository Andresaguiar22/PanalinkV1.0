package com.example.creative.ai.assistant

enum class CaptionTone(val displayName: String) {
    INSPIRATIONAL("Inspiracional"),
    FUNNY("Divertido / Humo"),
    PROFESSIONAL("Profesional"),
    ROMANTIC("Romántico"),
    VIRAL("Viral / Enganche"),
    STORYTELLING("Storytelling")
}

data class GeneratedCaption(
    val text: String,
    val tone: CaptionTone,
    val suggestedHashtags: List<String>
)

/**
 * P6.6.5 - Smart Caption Generator
 * Local AI caption generator providing contextual hooks, storytelling, and hashtag recommendations.
 */
object SmartCaptionGenerator {

    fun generateCaptions(topic: String, categoryName: String): List<GeneratedCaption> {
        val baseTopic = if (topic.isBlank()) "momentos increíbles" else topic

        return listOf(
            GeneratedCaption(
                text = "Crea la vida que no puedes esperar para vivir ✨ $baseTopic.",
                tone = CaptionTone.INSPIRATIONAL,
                suggestedHashtags = listOf("PanaLink", "Inspiracion", "EstiloDeVida", "Motivacion")
            ),
            GeneratedCaption(
                text = "Guarda este post antes de que se pierda en el feed 🚀 $baseTopic.",
                tone = CaptionTone.VIRAL,
                suggestedHashtags = listOf("Viral", "Tendencia", "PanaLinkCreative", "ParaTi")
            ),
            GeneratedCaption(
                text = "Elevando los estándares día a día. $baseTopic 💼",
                tone = CaptionTone.PROFESSIONAL,
                suggestedHashtags = listOf("Pro", "Creador", "Business", "Innovacion")
            ),
            GeneratedCaption(
                text = "Un pequeño resumen de lo que ha sido $baseTopic 📖",
                tone = CaptionTone.STORYTELLING,
                suggestedHashtags = listOf("Diario", "Experiencias", "Recuerdos")
            )
        )
    }
}
