package com.example.ui.components.chat.interaction

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

/**
 * SwipeReplyHandler
 * Handles elastic horizontal drag gesture for swipe-to-reply with progressive resistance,
 * spring return animation, and haptic feedback upon reaching threshold.
 */
@Composable
fun rememberSwipeReplyState(
    thresholdDp: Dp = 80.dp,
    onSwipeToReply: () -> Unit
): SwipeReplyState {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val thresholdPx = with(density) { thresholdDp.toPx() }
    val currentOnReply by rememberUpdatedState(onSwipeToReply)

    return remember(thresholdPx) {
        SwipeReplyState(
            thresholdPx = thresholdPx,
            onReply = currentOnReply,
            haptic = haptic,
            coroutineScope = coroutineScope
        )
    }
}

class SwipeReplyState(
    val thresholdPx: Float,
    private val onReply: () -> Unit,
    private val haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    private val coroutineScope: kotlinx.coroutines.CoroutineScope
) {
    val offset = Animatable(0f)
    var isSwiping by mutableStateOf(false)
        private set
    private var replyTriggered = false

    fun onDrag(dragAmount: Float) {
        isSwiping = true
        val currentOffset = offset.value
        val rawOffset = currentOffset + dragAmount
        
        // Elastic resistance: progressive dampening beyond 0
        val newOffset = if (rawOffset <= 0f) {
            0f
        } else if (rawOffset <= thresholdPx) {
            rawOffset
        } else {
            // Apply elastic logarithmic curve past threshold
            thresholdPx + (rawOffset - thresholdPx) * 0.35f
        }

        coroutineScope.launch {
            offset.snapTo(newOffset)
        }

        if (newOffset >= thresholdPx && !replyTriggered) {
            replyTriggered = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    fun onDragEnd() {
        val triggered = offset.value >= thresholdPx
        if (triggered) {
            onReply()
        }
        coroutineScope.launch {
            offset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioMediumBouncy
                )
            )
            replyTriggered = false
            isSwiping = false
        }
    }

    fun onDragCancel() {
        coroutineScope.launch {
            offset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioMediumBouncy
                )
            )
            replyTriggered = false
            isSwiping = false
        }
    }
}

fun Modifier.swipeReplyGesture(state: SwipeReplyState): Modifier = composed {
    this.pointerInput(state) {
        detectHorizontalDragGestures(
            onDragEnd = { state.onDragEnd() },
            onDragCancel = { state.onDragCancel() },
            onHorizontalDrag = { change, dragAmount ->
                if (dragAmount > 0 || state.offset.value > 0) {
                    change.consume()
                    state.onDrag(dragAmount)
                }
            }
        )
    }
}
