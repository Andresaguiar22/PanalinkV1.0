package com.example.ui.call

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.identity.model.toIdentityUiState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.getAvatarGradient

/**
 * CallAvatarPulse draws a highly polished, pulsating circular contact avatar,
 * surrounded by translucent, multi-layered expanding wave/halo rings.
 */
@Composable
fun CallAvatarPulse(
    name: String,
    userId: String? = null,
    avatarUrl: String? = null,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 130.dp,
    pulseColor: Color = Color(0xFF22C55E), // Default to Green/Emerald
    isAnimating: Boolean = true
) {
    val transition = rememberInfiniteTransition(label = "pulseTransition")
    
    // Wave animations with varying delays and scales
    val scale1 by if (isAnimating) {
        transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scale1"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val alpha1 by if (isAnimating) {
        transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha1"
        )
    } else {
        remember { mutableStateOf(0.4f) }
    }

    val scale2 by if (isAnimating) {
        transition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.9f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, delayMillis = 700, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scale2"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val alpha2 by if (isAnimating) {
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, delayMillis = 700, easing = LinearOutSlowInEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha2"
        )
    } else {
        remember { mutableStateOf(0.3f) }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(avatarSize * 2.2f)
    ) {
        if (isAnimating) {
            // Outermost wave
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .scale(scale2)
                    .background(pulseColor.copy(alpha = alpha2), shape = CircleShape)
            )
            
            // Middle wave
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .scale(scale1)
                    .background(pulseColor.copy(alpha = alpha1), shape = CircleShape)
            )
        }

        // Beautiful Avatar Circle
        val context = androidx.compose.ui.platform.LocalContext.current
        val identityRepository = remember { com.example.identity.bridge.LegacyIdentityBridge(context).identityRepository }
        val initialCached = remember(userId) { com.example.identity.memory.IdentityMemoryCache.profiles.get(userId ?: "") }
        val identityUiState by identityRepository.observeIdentity(userId ?: "").collectAsStateWithLifecycle(initialValue = initialCached?.toIdentityUiState())

        val resolvedName = identityUiState?.displayName ?: name
        val resolvedAvatar = identityUiState?.avatarUrl ?: avatarUrl

        com.example.ui.components.PanaAvatar(
            avatarUrl = resolvedAvatar,
            userId = userId,
            placeholderName = resolvedName,
            size = avatarSize,
            modifier = Modifier.size(avatarSize)
        )
    }
}
