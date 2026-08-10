package com.example.creative.templates

import com.example.creative.core.CreativeLayer
import com.example.creative.post.PostPage
import com.example.creative.post.PostStudioProject
import java.util.UUID

data class PostTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: TemplateCategory,
    val style: TemplateStyle,
    val applyTemplate: (project: PostStudioProject, variables: Map<TemplateVariable, String>) -> PostStudioProject
)

/**
 * P6.6.4.1 - Post Template Engine
 * Professional, dynamic template system for PanaLink Post Studio Pro.
 * Applies TemplateStyle, TemplateVariables, FilterPresets, and CreativeAssets onto PostStudioProjects.
 */
object PostTemplateEngine {

    val availableTemplates: List<PostTemplate> = listOf(
        PostTemplate(
            id = "template_travel_cinematic",
            name = "Travel Cinematic",
            description = "Estilo cinematográfico con filtro azul océano y tipografía elegante.",
            category = TemplateCategory.TRAVEL,
            style = TemplateStyle(
                name = "Travel Cinematic Style",
                category = TemplateCategory.TRAVEL,
                typography = TypographyPreset(
                    titleFontFamily = "Serif",
                    titleFontSizeSp = 32f,
                    subtitleFontFamily = "SansSerif",
                    subtitleFontSizeSp = 16f
                ),
                colors = ColorPalette(
                    primaryHex = "#38BDF8",
                    secondaryHex = "#0284C7",
                    textPrimaryHex = "#FFFFFF",
                    textSecondaryHex = "#E2E8F0"
                ),
                layout = LayoutPreset(aspectRatio = "4:5", titleYFraction = 0.25f, subtitleYFraction = 0.82f),
                filter = FilterPreset.OCEAN_CINEMATIC
            ),
            applyTemplate = { project, vars ->
                val updatedPages = project.pages.mapIndexed { idx, page ->
                    val mainMedia = page.getMainMediaLayer()
                    val filteredLayers = page.layers.filter { l ->
                        l is CreativeLayer.Image || l is CreativeLayer.Video
                    }.map { l ->
                        when (l) {
                            is CreativeLayer.Image -> l.copy(filterName = FilterPreset.OCEAN_CINEMATIC.filterName)
                            is CreativeLayer.Video -> l.copy(filterName = FilterPreset.OCEAN_CINEMATIC.filterName)
                            else -> l
                        }
                    }.toMutableList()

                    val titleText = TemplateVariable.replacePlaceholders(
                        "{TITLE}",
                        vars
                    )
                    val subtitleText = TemplateVariable.replacePlaceholders(
                        "{LOCATION} • {DATE}",
                        vars
                    )

                    filteredLayers.add(
                        CreativeLayer.Text(
                            id = "tpl_title_$idx",
                            text = titleText,
                            colorHex = "#FFFFFF",
                            fontFamily = "Serif",
                            fontSizeSp = 32f,
                            xFraction = 0.5f,
                            yFraction = 0.25f
                        )
                    )

                    filteredLayers.add(
                        CreativeLayer.Text(
                            id = "tpl_sub_$idx",
                            text = subtitleText,
                            colorHex = "#E2E8F0",
                            fontFamily = "SansSerif",
                            fontSizeSp = 16f,
                            xFraction = 0.5f,
                            yFraction = 0.82f
                        )
                    )

                    page.copy(
                        aspectRatio = "4:5",
                        layers = filteredLayers
                    )
                }

                project.copy(
                    title = "Borrador - Travel Cinematic",
                    pages = updatedPages
                )
            }
        ),

        PostTemplate(
            id = "template_business_promo",
            name = "Business Launch",
            description = "Estilo corporativo con colores neón/azules y badges promocionales.",
            category = TemplateCategory.BUSINESS,
            style = TemplateStyle(
                name = "Business Pro Style",
                category = TemplateCategory.BUSINESS,
                typography = TypographyPreset(
                    titleFontFamily = "SansSerif",
                    titleFontSizeSp = 30f,
                    subtitleFontFamily = "Monospace",
                    subtitleFontSizeSp = 14f
                ),
                colors = ColorPalette(
                    primaryHex = "#3B82F6",
                    secondaryHex = "#1D4ED8",
                    accentHex = "#F59E0B"
                ),
                layout = LayoutPreset(aspectRatio = "1:1", titleYFraction = 0.18f, subtitleYFraction = 0.88f),
                filter = FilterPreset.VIVID
            ),
            applyTemplate = { project, vars ->
                val updatedPages = project.pages.mapIndexed { idx, page ->
                    val filteredLayers = page.layers.filter { l ->
                        l is CreativeLayer.Image || l is CreativeLayer.Video
                    }.map { l ->
                        when (l) {
                            is CreativeLayer.Image -> l.copy(filterName = FilterPreset.VIVID.filterName)
                            is CreativeLayer.Video -> l.copy(filterName = FilterPreset.VIVID.filterName)
                            else -> l
                        }
                    }.toMutableList()

                    val titleText = TemplateVariable.replacePlaceholders("{TITLE}", vars)
                    val subText = TemplateVariable.replacePlaceholders("{SUBTITLE}", vars)

                    filteredLayers.add(
                        CreativeLayer.Text(
                            id = "tpl_biz_title_$idx",
                            text = titleText,
                            colorHex = "#FFFFFF",
                            fontFamily = "SansSerif",
                            fontSizeSp = 30f,
                            xFraction = 0.5f,
                            yFraction = 0.18f
                        )
                    )

                    filteredLayers.add(
                        CreativeLayer.Text(
                            id = "tpl_biz_sub_$idx",
                            text = subText,
                            colorHex = "#F59E0B",
                            fontFamily = "Monospace",
                            fontSizeSp = 14f,
                            xFraction = 0.5f,
                            yFraction = 0.88f
                        )
                    )

                    page.copy(
                        aspectRatio = "1:1",
                        layers = filteredLayers
                    )
                }

                project.copy(
                    title = "Borrador - Business Launch",
                    pages = updatedPages
                )
            }
        ),

        PostTemplate(
            id = "template_cyberpunk_influencer",
            name = "Cyberpunk Influencer",
            description = "Filtro neón cyberpunk con stickers dinámicos y tipografía moderna.",
            category = TemplateCategory.INFLUENCER,
            style = TemplateStyle(
                name = "Cyberpunk Style",
                category = TemplateCategory.INFLUENCER,
                typography = TypographyPreset(
                    titleFontFamily = "Monospace",
                    titleFontSizeSp = 34f
                ),
                colors = ColorPalette(
                    primaryHex = "#EC4899",
                    secondaryHex = "#8B5CF6",
                    accentHex = "#00E5FF"
                ),
                layout = LayoutPreset(aspectRatio = "4:5"),
                filter = FilterPreset.CYBERPUNK
            ),
            applyTemplate = { project, vars ->
                val updatedPages = project.pages.mapIndexed { idx, page ->
                    val filteredLayers = page.layers.filter { l ->
                        l is CreativeLayer.Image || l is CreativeLayer.Video
                    }.map { l ->
                        when (l) {
                            is CreativeLayer.Image -> l.copy(filterName = FilterPreset.CYBERPUNK.filterName)
                            is CreativeLayer.Video -> l.copy(filterName = FilterPreset.CYBERPUNK.filterName)
                            else -> l
                        }
                    }.toMutableList()

                    val titleText = TemplateVariable.replacePlaceholders("{TITLE}", vars)
                    val authorText = TemplateVariable.replacePlaceholders("by {AUTHOR}", vars)

                    filteredLayers.add(
                        CreativeLayer.Text(
                            id = "tpl_cyber_title_$idx",
                            text = titleText,
                            colorHex = "#00E5FF",
                            fontFamily = "Monospace",
                            fontSizeSp = 34f,
                            xFraction = 0.5f,
                            yFraction = 0.2f
                        )
                    )

                    filteredLayers.add(
                        CreativeLayer.Text(
                            id = "tpl_cyber_author_$idx",
                            text = authorText,
                            colorHex = "#EC4899",
                            fontFamily = "SansSerif",
                            fontSizeSp = 14f,
                            xFraction = 0.5f,
                            yFraction = 0.85f
                        )
                    )

                    val stickerAsset = CreativeAsset(
                        id = "asset_neon_badge",
                        type = CreativeAssetType.STICKER,
                        assetUriOrPath = "🔥"
                    )

                    filteredLayers.add(
                        CreativeLayer.Sticker(
                            id = "tpl_cyber_sticker_$idx",
                            stickerUrlOrPath = stickerAsset.assetUriOrPath,
                            xFraction = 0.85f,
                            yFraction = 0.15f
                        )
                    )

                    page.copy(
                        aspectRatio = "4:5",
                        layers = filteredLayers
                    )
                }

                project.copy(
                    title = "Borrador - Cyberpunk",
                    pages = updatedPages
                )
            }
        )
    )

    fun getTemplatesByCategory(category: TemplateCategory): List<PostTemplate> {
        return availableTemplates.filter { it.category == category }
    }

    fun findTemplateById(id: String): PostTemplate? {
        return availableTemplates.firstOrNull { it.id == id }
    }
}
