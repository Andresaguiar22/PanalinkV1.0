package com.example.ui.components.chat.voice

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull

fun Modifier.voiceGestureDetector(
    enabled: Boolean = true,
    isLocked: Boolean = false,
    lockThresholdY: Float = -220f,
    cancelThresholdX: Float = -250f,
    onPermissionRequired: (() -> Unit)? = null,
    onDrag: ((offsetX: Float, offsetY: Float) -> Unit)? = null,
    onEvent: (VoiceGestureEvent) -> Unit
): Modifier = if (!enabled) this else this.pointerInput(enabled, isLocked) {
    awaitPointerEventScope {
        while (true) {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (isLocked) {
                continue
            }

            if (onPermissionRequired != null) {
                onPermissionRequired()
                // Wait for touch release before looping
                do {
                    val event = awaitPointerEvent()
                } while (event.changes.any { it.pressed })
                continue
            }

            down.consume()

            // Touch-and-hold delay (200ms threshold)
            val isLongPress = withTimeoutOrNull(200) {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.changes.any { !it.pressed }) return@withTimeoutOrNull false
                }
                true
            } ?: true

            if (!isLongPress) continue

            // Emit StartRecording
            onEvent(VoiceGestureEvent.StartRecording)

            var gestureHandledLocally = false
            var totalY = 0f
            var totalX = 0f

            try {
                do {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull() ?: break
                    if (change.pressed) {
                        change.consume()
                        val pos = change.position
                        val prev = change.previousPosition
                        totalY += (pos.y - prev.y)
                        totalX += (pos.x - prev.x)

                        val clampedY = totalY.coerceAtMost(0f).coerceAtLeast(-350f)
                        val clampedX = totalX.coerceAtMost(0f).coerceAtLeast(-450f)

                        onDrag?.invoke(clampedX, clampedY)

                        // Drag UP -> Lock
                        if (totalY < lockThresholdY && !gestureHandledLocally) {
                            gestureHandledLocally = true
                            onDrag?.invoke(0f, 0f)
                            onEvent(VoiceGestureEvent.LockRecording)
                            break
                        }

                        // Drag LEFT -> Cancel
                        if (totalX < cancelThresholdX && !gestureHandledLocally) {
                            gestureHandledLocally = true
                            onDrag?.invoke(0f, 0f)
                            onEvent(VoiceGestureEvent.CancelRecording)
                            break
                        }
                    }
                } while (event.changes.any { it.pressed })

                // Consume remaining pointer events if broke early
                while (true) {
                    val currentEvent = awaitPointerEvent()
                    if (!currentEvent.changes.any { it.pressed }) {
                        break
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                onDrag?.invoke(0f, 0f)
                onEvent(VoiceGestureEvent.CancelRecording)
                throw e
            } catch (_: Exception) {
            }

            onDrag?.invoke(0f, 0f)

            // Finger released without triggering lock or cancel
            if (!gestureHandledLocally) {
                onEvent(VoiceGestureEvent.FinishRecording)
            }
        }
    }
}
