package com.example.creative.template

import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import java.util.UUID

data class CreativeTemplate(
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val layers: List<CreativeLayer>,
    val filterName: String = "none",
    val createdAt: Long = System.currentTimeMillis()
)

object CreativeTemplateManager {

    private val templates = mutableListOf<CreativeTemplate>()

    init {
        templates.add(
            CreativeTemplate(
                id = "tmpl_neon_vibes",
                name = "Neón Criollo 🌌",
                description = "Plantilla neón con texto resaltado y stickers",
                type = "STORY",
                layers = listOf(
                    CreativeLayer.Text(id = "txt_neon", text = "Vibra PanaLink ✨", colorHex = "#00E5FF", fontFamily = "Neon"),
                    CreativeLayer.Sticker(id = "stk_fire", stickerUrlOrPath = "🔥")
                ),
                filterName = "neon"
            )
        )
        templates.add(
            CreativeTemplate(
                id = "tmpl_cinematic_story",
                name = "Cine Urbano 🎬",
                description = "Estilo cinematográfico con filtro vintage",
                type = "STORY",
                layers = listOf(
                    CreativeLayer.Text(id = "txt_cine", text = "Momento Único 🍿", colorHex = "#FFEA00", fontFamily = "Playfair"),
                    CreativeLayer.Interactive(id = "inter_poll", interactiveType = "POLL", title = "¿Qué opina el equipo?", optionA = "Top 🔥", optionB = "Increíble 🌟")
                ),
                filterName = "cinematic"
            )
        )
    }

    fun createTemplateFromProject(
        project: CreativeProject,
        templateName: String,
        description: String
    ): CreativeTemplate {
        val filterName = project.layers.filterIsInstance<CreativeLayer.Filter>().firstOrNull()?.filterName ?: "none"
        val template = CreativeTemplate(
            id = "tmpl_${UUID.randomUUID()}",
            name = templateName,
            description = description,
            type = project.type.name,
            layers = project.layers,
            filterName = filterName
        )
        templates.add(template)
        return template
    }

    fun applyTemplateToProject(
        template: CreativeTemplate,
        targetProject: CreativeProject
    ): CreativeProject {
        return targetProject.copy(
            layers = template.layers
        )
    }

    fun getTemplates(): List<CreativeTemplate> {
        return templates.toList()
    }
}
