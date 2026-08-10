package com.example.ui.components.chat.bubble

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * BubbleShapeFactory
 * Dynamic shape generator for Panalink Chat Premium 3.0 message bubbles.
 * Produces clean rounded corner shapes with or without subtle directional tails based on
 * message group position (SINGLE, FIRST, MIDDLE, LAST) and sender status (isMe).
 */
@Immutable
object BubbleShapeFactory {

    private const val DEFAULT_CORNER = 14f
    private const val REDUCED_CORNER = 4f
    private const val TAIL_CORNER = 2f

    /**
     * Returns a Compose Shape for the message bubble depending on whether it's
     * sent by the local user (isMe) and its relative position in a consecutive message cluster.
     */
    fun createShape(
        groupPosition: MessageGroupPosition,
        isMe: Boolean,
        cornerDp: Dp = 14.dp
    ): Shape {
        val cornerPx = cornerDp.value
        return if (isMe) {
            when (groupPosition) {
                MessageGroupPosition.SINGLE -> {
                    createOutgoingTailShape(cornerPx)
                }
                MessageGroupPosition.FIRST -> {
                    createOutgoingTailShape(cornerPx)
                }
                MessageGroupPosition.MIDDLE -> {
                    RoundedCornerShape(
                        topStart = cornerDp,
                        topEnd = cornerDp,
                        bottomStart = cornerDp,
                        bottomEnd = cornerDp
                    )
                }
                MessageGroupPosition.LAST -> {
                    RoundedCornerShape(
                        topStart = cornerDp,
                        topEnd = cornerDp,
                        bottomStart = cornerDp,
                        bottomEnd = cornerDp
                    )
                }
            }
        } else {
            when (groupPosition) {
                MessageGroupPosition.SINGLE -> {
                    createIncomingTailShape(cornerPx)
                }
                MessageGroupPosition.FIRST -> {
                    createIncomingTailShape(cornerPx)
                }
                MessageGroupPosition.MIDDLE -> {
                    RoundedCornerShape(
                        topStart = cornerDp,
                        topEnd = cornerDp,
                        bottomStart = cornerDp,
                        bottomEnd = cornerDp
                    )
                }
                MessageGroupPosition.LAST -> {
                    RoundedCornerShape(
                        topStart = cornerDp,
                        topEnd = cornerDp,
                        bottomStart = cornerDp,
                        bottomEnd = cornerDp
                    )
                }
            }
        }
    }

    /**
     * Outgoing bubble shape with an integrated top-right tail path.
     */
    private fun createOutgoingTailShape(cornerPx: Float): Shape {
        return GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val tailSize = 10f

            // Start top left
            moveTo(cornerPx, 0f)
            lineTo(w - cornerPx, 0f)
            
            // Tail path at top right
            lineTo(w + tailSize, 0f)
            lineTo(w, tailSize)
            
            // Right side
            lineTo(w, h - cornerPx)
            quadraticTo(w, h, w - cornerPx, h)
            
            // Bottom side
            lineTo(cornerPx, h)
            quadraticTo(0f, h, 0f, h - cornerPx)
            
            // Left side
            lineTo(0f, cornerPx)
            quadraticTo(0f, 0f, cornerPx, 0f)
            close()
        }
    }

    /**
     * Incoming bubble shape with an integrated top-left tail path.
     */
    private fun createIncomingTailShape(cornerPx: Float): Shape {
        return GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val tailSize = 10f

            // Start from tail tip at top left
            moveTo(-tailSize, 0f)
            lineTo(cornerPx, 0f)
            
            // Top side
            lineTo(w - cornerPx, 0f)
            quadraticTo(w, 0f, w, cornerPx)
            
            // Right side
            lineTo(w, h - cornerPx)
            quadraticTo(w, h, w - cornerPx, h)
            
            // Bottom side
            lineTo(cornerPx, h)
            quadraticTo(0f, h, 0f, h - cornerPx)
            
            // Left side up to tail base
            lineTo(0f, tailSize)
            lineTo(-tailSize, 0f)
            close()
        }
    }
}
