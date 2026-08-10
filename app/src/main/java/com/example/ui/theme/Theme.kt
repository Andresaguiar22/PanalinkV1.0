package com.example.ui.theme

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.animation.core.*
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape

@Stable
class AppColors(
    primary: Color,
    secondary: Color,
    background: Color,
    surface: Color,
    bubbleMe: Color,
    bubbleOther: Color,
    topBar: Color,
    bottomBar: Color,
    accent: Color,
    isDark: Boolean,
    onPrimary: Color,
    onSecondary: Color,
    onBackground: Color,
    onSurface: Color
) {
    var primary by mutableStateOf(primary)
    var secondary by mutableStateOf(secondary)
    var background by mutableStateOf(background)
    var surface by mutableStateOf(surface)
    var bubbleMe by mutableStateOf(bubbleMe)
    var bubbleOther by mutableStateOf(bubbleOther)
    var topBar by mutableStateOf(topBar)
    var bottomBar by mutableStateOf(bottomBar)
    var accent by mutableStateOf(accent)
    var isDark by mutableStateOf(isDark)
    var onPrimary by mutableStateOf(onPrimary)
    var onSecondary by mutableStateOf(onSecondary)
    var onBackground by mutableStateOf(onBackground)
    var onSurface by mutableStateOf(onSurface)

    fun updateColorsFrom(other: AppColors) {
        primary = other.primary
        secondary = other.secondary
        background = other.background
        surface = other.surface
        bubbleMe = other.bubbleMe
        bubbleOther = other.bubbleOther
        topBar = other.topBar
        bottomBar = other.bottomBar
        accent = other.accent
        isDark = other.isDark
        onPrimary = other.onPrimary
        onSecondary = other.onSecondary
        onBackground = other.onBackground
        onSurface = other.onSurface
    }
}

val DarkTealColors = AppColors(
    primary = Color(0xFF00E5FF),      // Electric Cyan
    secondary = Color(0xFF161618),    // Dark Gray surface
    background = Color(0xFF000000),   // Deep Black
    surface = Color(0xFF121212),      // Dark Gray Surface
    bubbleMe = Color(0xFF1C1C1E),     // Graphite Gray Card
    bubbleOther = Color(0xFF262629),  // Light Graphite Gray Card
    topBar = Color(0xFF000000),
    bottomBar = Color(0xFF000000),
    accent = Color(0xFF8B5CF6),       // Electric Violet
    isDark = true,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

val RoyalPurpleColors = AppColors(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF4A148C),
    background = Color(0xFF120024),
    surface = Color(0xFF2E1C4B),
    bubbleMe = Color(0xFF6200EE),
    bubbleOther = Color(0xFF3B2E5C),
    topBar = Color(0xFF4A148C),
    bottomBar = Color(0xFF4A148C),
    accent = Color(0xFFBB86FC),
    isDark = true,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

val WhatsAppLightColors = AppColors(
    primary = WhatsAppPrimary,
    secondary = WhatsAppSurface,
    background = WhatsAppBackground,
    surface = WhatsAppSurface,
    bubbleMe = WhatsAppPrimary.copy(alpha = 0.2f),
    bubbleOther = WhatsAppSurface,
    topBar = WhatsAppSurface,
    bottomBar = WhatsAppSurface,
    accent = WhatsAppPrimary,
    isDark = false,
    onPrimary = Color.White,
    onSecondary = WhatsAppTextDark,
    onBackground = WhatsAppTextDark,
    onSurface = WhatsAppTextDark
)

val ClassicDarkColors = AppColors(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF1E1E1E),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    bubbleMe = Color(0xFFBB86FC),
    bubbleOther = Color(0xFF333333),
    topBar = Color(0xFF121212),
    bottomBar = Color(0xFF121212),
    accent = Color(0xFFBB86FC),
    isDark = true,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

val ElegantGreyColors = AppColors(
    primary = ElegantPrimary,
    secondary = ElegantSurface,
    background = ElegantBackground,
    surface = ElegantSurface,
    bubbleMe = ElegantPrimary.copy(alpha = 0.2f),
    bubbleOther = ElegantSurface,
    topBar = ElegantSurface,
    bottomBar = ElegantSurface,
    accent = ElegantPrimary,
    isDark = true,
    onPrimary = Color.Black,
    onSecondary = ElegantTextLight,
    onBackground = ElegantTextLight,
    onSurface = ElegantTextLight
)

val VividOceanColors = AppColors(
    primary = OceanPrimary,
    secondary = OceanSurface,
    background = OceanBackground,
    surface = OceanSurface,
    bubbleMe = OceanPrimary.copy(alpha = 0.2f),
    bubbleOther = OceanSurface,
    topBar = OceanSurface,
    bottomBar = OceanSurface,
    accent = OceanPrimary,
    isDark = true,
    onPrimary = Color.Black,
    onSecondary = OceanTextLight,
    onBackground = OceanTextLight,
    onSurface = OceanTextLight
)

val NordicIceColors = AppColors(
    primary = Color(0xFF80DEEA),
    secondary = Color(0xFF006064),
    background = Color(0xFF0E1A20),
    surface = Color(0xFF21323C),
    bubbleMe = Color(0xFF00838F),
    bubbleOther = Color(0xFF263C46),
    topBar = Color(0xFF006064),
    bottomBar = Color(0xFF006064),
    accent = Color(0xFF80DEEA),
    isDark = true,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

val CyberpunkColors = AppColors(
    primary = Color(0xFF00F0FF),
    secondary = Color(0xFFBC00DD),
    background = Color(0xFF05050A),
    surface = Color(0xFF10101F),
    bubbleMe = Color(0xFFBC00DD),
    bubbleOther = Color(0xFF151525),
    topBar = Color(0xFF10101F),
    bottomBar = Color(0xFF10101F),
    accent = Color(0xFFFFE600),
    isDark = true,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

val NeonVibeColors = AppColors(
    primary = Color(0xFF39FF14),
    secondary = Color(0xFF1F1F1F),
    background = Color(0xFF000000),
    surface = Color(0xFF0D0D0D),
    bubbleMe = Color(0xFF1F1F1F),
    bubbleOther = Color(0xFF121212),
    topBar = Color(0xFF000000),
    bottomBar = Color(0xFF000000),
    accent = Color(0xFFFF007F),
    isDark = true,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

val MinimalWhiteColors = AppColors(
    primary = Color(0xFF111111),
    secondary = Color(0xFFEEEEEE),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF7F7F7),
    bubbleMe = Color(0xFFE2E2E2),
    bubbleOther = Color(0xFFF0F0F0),
    topBar = Color(0xFFFFFFFF),
    bottomBar = Color(0xFFFFFFFF),
    accent = Color(0xFF666666),
    isDark = false,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

val HaloLightColors = AppColors(
    primary = Color(0xFF4FA37D),      // Elegant Soft Mint Green
    secondary = Color(0xFFE8F2ED),    // Soft minty cream
    background = Color(0xFFF4FAF7),   // Warm minty white
    surface = Color(0xFFFAFDFD),      // Clean white card
    bubbleMe = Color(0xFF4FA37D),     // Soft Mint Green
    bubbleOther = Color(0xFFE8F2ED),  // Warm cream bubble
    topBar = Color(0xFFF4FAF7),
    bottomBar = Color(0xFFF4FAF7),
    accent = Color(0xFFD4EFE3),       // Soft minty accent
    isDark = false,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

val HaloDarkColors = AppColors(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFF161618), // Neutral dark gray for inputs/surfaces
    background = Color(0xFF020617),
    surface = Color(0xFF0F172A),
    bubbleMe = Color(0xFF7C3AED),
    bubbleOther = Color(0xFF161618),
    topBar = Color(0xFF020617),
    bottomBar = Color(0xFF020617),
    accent = Color(0xFF00E5FF),
    isDark = true,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

object ThemeManager {
    val themeKey = kotlinx.coroutines.flow.MutableStateFlow("halo_dark")
    
    val customPrimary = kotlinx.coroutines.flow.MutableStateFlow(Color(0xFF76CE9F))
    val customBackground = kotlinx.coroutines.flow.MutableStateFlow(Color(0xFF0F1412))
    val customAccent = kotlinx.coroutines.flow.MutableStateFlow(Color(0xFF2E483E))
    val customSurface = kotlinx.coroutines.flow.MutableStateFlow(Color(0xFF1B2420))
    val customSecondary = kotlinx.coroutines.flow.MutableStateFlow(Color(0xFF1B2420))

    val isMinimalistMode = kotlinx.coroutines.flow.MutableStateFlow(false)

    val bottomBarColorPreset = kotlinx.coroutines.flow.MutableStateFlow("tropical")
    val bottomBarShapePreset = kotlinx.coroutines.flow.MutableStateFlow("pill")

    fun getBottomBarColors(preset: String): List<Color> {
        return when (preset) {
            "neon_cyber" -> listOf(
                Color(0xFF00F0FF), // Cyan
                Color(0xFFBC00DD), // Magenta
                Color(0xFFFF007F), // Neon Pink
                Color(0xFF39FF14), // Lime Green
                Color(0xFF00F0FF)
            )
            "monochrome" -> listOf(
                Color(0xFFFFFFFF),
                Color(0xFFB0B0B0),
                Color(0xFF606060),
                Color(0xFF202020),
                Color(0xFFFFFFFF)
            )
            "sunset" -> listOf(
                Color(0xFFFF3D00), // Deep Orange
                Color(0xFFFF9100), // Orange
                Color(0xFFFFEA00), // Yellow
                Color(0xFFFF007F), // Pink
                Color(0xFFFF3D00)
            )
            "aurora" -> listOf(
                Color(0xFF00E5FF), // Cyan
                Color(0xFF1DE9B6), // Teal
                Color(0xFF00E676), // Lime Accent
                Color(0xFF651FFF), // Purple Accent
                Color(0xFF00E5FF)
            )
            else -> listOf( // "tropical" / default elegant mint
                Color(0xFF76CE9F), // Soft Mint Green
                Color(0xFF4FA37D), // Medium Mint Green
                Color(0xFF86D2B1), // Light Mint Green
                Color(0xFF59AC88), // Forest Mint
                Color(0xFF2E483E), // Deep Forest Mint
                Color(0xFF76CE9F)  // Soft Mint Green
            )
        }
    }

    fun getBottomBarShape(preset: String): Shape {
        return when (preset) {
            "rounded_rect" -> RoundedCornerShape(16.dp)
            "cut_corners" -> CutCornerShape(12.dp)
            "wave" -> RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 6.dp,
                bottomStart = 6.dp,
                bottomEnd = 24.dp
            )
            "sharp" -> RectangleShape
            else -> CircleShape // "pill"
        }
    }
}

fun getColorsForTheme(themeKey: String?, customColors: AppColors? = null): AppColors {
    return when (themeKey) {
        "whatsapp_light" -> WhatsAppLightColors
        "classic_dark" -> ClassicDarkColors
        "elegant_grey" -> ElegantGreyColors
        "vivid_ocean" -> VividOceanColors
        "halo_light" -> HaloLightColors
        "halo_dark" -> HaloDarkColors
        "royal_purple" -> RoyalPurpleColors
        "nordic_ice" -> NordicIceColors
        "cyberpunk" -> CyberpunkColors
        "neon" -> NeonVibeColors
        "minimal_white" -> MinimalWhiteColors
        "custom" -> customColors ?: HaloDarkColors
        else -> HaloDarkColors
    }
}

val LocalAppColors = staticCompositionLocalOf { HaloDarkColors }

@Composable
fun MyApplicationTheme(
    themeKey: String = "dark_teal",
    customColors: AppColors? = null,
    content: @Composable () -> Unit,
) {
    val activeColors = remember(themeKey, customColors) { 
        getColorsForTheme(themeKey, customColors) 
    }

    val isDark = activeColors.isDark
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = activeColors.primary,
            secondary = activeColors.secondary,
            background = activeColors.background,
            surface = activeColors.surface,
            onPrimary = activeColors.onPrimary,
            onSecondary = activeColors.onSecondary,
            onBackground = activeColors.onBackground,
            onSurface = activeColors.onSurface
        )
    } else {
        lightColorScheme(
            primary = activeColors.primary,
            secondary = activeColors.secondary,
            background = activeColors.background,
            surface = activeColors.surface,
            onPrimary = activeColors.onPrimary,
            onSecondary = activeColors.onSecondary,
            onBackground = activeColors.onBackground,
            onSurface = activeColors.onSurface
        )
    }

    CompositionLocalProvider(LocalAppColors provides activeColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// 🦴 SHIMMER SKELETON LOADERS
fun Modifier.shimmerEffect(): Modifier = composed {
    var size by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val startOffsetX by transition.animateFloat(
        initialValue = -2 * size.width.toFloat() - 1f,
        targetValue = 2 * size.width.toFloat() + 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffsetX"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF263238).copy(alpha = 0.7f),
                Color(0xFF37474F).copy(alpha = 0.4f),
                Color(0xFF263238).copy(alpha = 0.7f),
            ),
            start = androidx.compose.ui.geometry.Offset(startOffsetX, 0f),
            end = androidx.compose.ui.geometry.Offset(startOffsetX + size.width.toFloat(), size.height.toFloat())
        )
    ).onGloballyPositioned {
        size = it.size
    }
}

// 🎨 AVATARES CON GRADIENTE DINÁMICO (AvatarRing)
fun getAvatarGradient(name: String): Brush {
    val gradients = listOf(
        listOf(Color(0xFF3F51B5), Color(0xFF9C27B0)),
        listOf(Color(0xFF009688), Color(0xFF4CAF50)),
        listOf(Color(0xFFFF5722), Color(0xFFE91E63)),
        listOf(Color(0xFF2196F3), Color(0xFF00BCD4)),
        listOf(Color(0xFFFFC107), Color(0xFFFF5722)),
        listOf(Color(0xFFE91E63), Color(0xFF673AB7)),
        listOf(Color(0xFFCDDC39), Color(0xFF4CAF50)),
        listOf(Color(0xFF673AB7), Color(0xFFE040FB))
    )
    val charCode = if (name.isNotEmpty()) name[0].code else 0
    val index = Math.abs(charCode) % gradients.size
    val colors = gradients[index]
    return Brush.linearGradient(
        colors = colors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
}

// 🔄 ANIMACIONES EN CADA ACCIÓN (Bounce scale)
fun Modifier.bounceClick(onClick: () -> Unit = {}): Modifier = composed {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "bounceScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interactionSource,
        indication = androidx.compose.foundation.LocalIndication.current,
        onClick = onClick
    )
}

// 🔮 PREMIUM GRADIENT (Electric Blue, Violet, Cyan)
fun getPremiumGradient(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF0052FF), // Electric Blue
            Color(0xFF7C3AED), // Violet
            Color(0xFF00E5FF)  // Cyan
        ),
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset.Infinite
    )
}

fun getPremiumActiveIconGradient(): Brush {
    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF00E5FF), // Cyan
            Color(0xFF8B5CF6)  // Violet
        )
    )
}

