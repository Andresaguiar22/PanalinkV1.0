package com.example.creative.effects

data class ARFilterItem(
    val id: String,
    val name: String,
    val maskType: ARMaskType,
    val iconResOrUrl: String? = null
)

object ARFilterRepository {
    fun getAvailableFilters(): List<ARFilterItem> {
        return listOf(
            ARFilterItem("none", "Ninguno", ARMaskType.NONE),
            ARFilterItem("big_eyes", "Ojos Grandes", ARMaskType.BIG_EYES),
            ARFilterItem("funny_face", "Cara Divertida", ARMaskType.FUNNY_FACE),
            ARFilterItem("makeup", "Maquillaje Pro", ARMaskType.MAKEUP),
            ARFilterItem("neon_mask", "Máscara Neón", ARMaskType.NEON_MASK)
        )
    }
}
