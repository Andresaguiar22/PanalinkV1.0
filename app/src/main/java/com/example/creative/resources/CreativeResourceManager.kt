package com.example.creative.resources

enum class ResourceCategory {
    STICKERS,
    EMOJIS,
    GIFS,
    MUSIC,
    EFFECTS,
    FONTS,
    TEMPLATES
}

data class CreativeResourceItem(
    val id: String,
    val title: String,
    val category: ResourceCategory,
    val tagList: List<String>,
    val assetUriOrUrl: String,
    val isFavorite: Boolean = false,
    val previewIcon: String? = null
)

object CreativeResourceManager {

    private val sampleResources = listOf(
        CreativeResourceItem(
            id = "res_stk_1",
            title = "Fuego Neón",
            category = ResourceCategory.STICKERS,
            tagList = listOf("fire", "neon", "trend"),
            assetUriOrUrl = "file:///android_asset/stickers/fire_neon.png"
        ),
        CreativeResourceItem(
            id = "res_stk_2",
            title = "Corazón Pulsante",
            category = ResourceCategory.STICKERS,
            tagList = listOf("heart", "love", "animated"),
            assetUriOrUrl = "file:///android_asset/stickers/heart_pulse.png"
        ),
        CreativeResourceItem(
            id = "res_mus_1",
            title = "Summer Vibe Beats",
            category = ResourceCategory.MUSIC,
            tagList = listOf("summer", "pop", "upbeat"),
            assetUriOrUrl = "file:///android_asset/audio/summer_vibe.mp3"
        ),
        CreativeResourceItem(
            id = "res_fnt_1",
            title = "Montserrat Bold",
            category = ResourceCategory.FONTS,
            tagList = listOf("clean", "modern", "bold"),
            assetUriOrUrl = "montserrat_bold"
        ),
        CreativeResourceItem(
            id = "res_fnt_2",
            title = "Playfair Display",
            category = ResourceCategory.FONTS,
            tagList = listOf("serif", "elegant", "header"),
            assetUriOrUrl = "playfair_display"
        )
    )

    fun searchResources(
        query: String,
        category: ResourceCategory? = null,
        favoritesOnly: Boolean = false
    ): List<CreativeResourceItem> {
        return sampleResources.filter { item ->
            val matchesCategory = category == null || item.category == category
            val matchesQuery = query.isEmpty() || item.title.contains(query, ignoreCase = true) || item.tagList.any { it.contains(query, ignoreCase = true) }
            val matchesFav = !favoritesOnly || item.isFavorite
            matchesCategory && matchesQuery && matchesFav
        }
    }
}
