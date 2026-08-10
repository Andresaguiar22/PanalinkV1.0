package com.example.creative.export

enum class ExportResolution(val width: Int, val height: Int, val label: String) {
    RES_720P(720, 1280, "720p HD"),
    RES_1080P(1080, 1920, "1080p Full HD"),
    RES_2K(1440, 2560, "2K Quad HD"),
    RES_4K(2160, 3840, "4K Ultra HD")
}

enum class ExportFrameRate(val fps: Int) {
    FPS_24(24),
    FPS_30(30),
    FPS_60(60)
}

enum class ExportCodec(val mimeType: String, val displayName: String) {
    H264("video/avc", "H.264 (Compatible)"),
    HEVC("video/hevc", "H.265 / HEVC (Eficiente)")
}

enum class ExportPreset(val displayName: String) {
    FAST_EXPORT("Exportación Rápida"),
    MAX_QUALITY("Máxima Calidad Pro")
}

data class AdvancedExportConfig(
    val resolution: ExportResolution = ExportResolution.RES_1080P,
    val frameRate: ExportFrameRate = ExportFrameRate.FPS_30,
    val bitrateBps: Int = 10_000_000,
    val codec: ExportCodec = ExportCodec.H264,
    val preset: ExportPreset = ExportPreset.MAX_QUALITY
)
