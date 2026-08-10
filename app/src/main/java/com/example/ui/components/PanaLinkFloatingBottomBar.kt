package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

@Composable
fun PanaLinkFloatingBottomBar(
    currentPage: Int,
    onPageSelected: (Int) -> Unit,
    totalUnreadCount: Int = 0
) {
    val barShape = RoundedCornerShape(32.dp)
    
    // Gradient colors for the animated border
    val gradientColors = listOf(
        Color(0xFF00E5FF), // Cyan
        Color(0xFF7C4DFF), // Purple
        Color(0x0000E5FF), // Transparent for gaps
        Color(0xFF00E5FF)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "halo_transition")
    
    // Easing for rotation
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_angle"
    )
    
    // Pulsating opacity for border
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_alpha"
    )

    // Remember the X position of the selected tab for the underground glow
    var selectedTabX by remember { mutableFloatStateOf(0f) }
    val animatedGlowX by animateFloatAsState(
        targetValue = selectedTabX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "glow_x"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .shadow(elevation = 20.dp, shape = barShape, spotColor = Color(0xFF00E5FF).copy(alpha = 0.3f))
            .background(Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        // Main container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .clip(barShape)
                .background(Color(0xCC0D1322)) // Glassmorphism dark background
        ) {
            
            // Underground Glow (Radial gradient)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF00E5FF).copy(alpha = 0.4f), Color.Transparent),
                                center = Offset(animatedGlowX, size.height / 2),
                                radius = size.height * 1.5f
                            ),
                            center = Offset(animatedGlowX, size.height / 2),
                            radius = size.height * 1.5f
                        )
                    }
            )

            // Animated Gradient Border
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        rotate(angle) {
                            // Draw a rect that sweeps around the edges
                            // We need it larger than the bounds to cover corners during rotation
                            val radius = size.width
                            drawRect(
                                brush = Brush.sweepGradient(gradientColors, center = Offset(size.width/2, size.height/2)),
                                topLeft = Offset(size.width/2 - radius, size.height/2 - radius),
                                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                                alpha = pulseAlpha
                            )
                        }
                    }
            )

            // Inner Background (slightly smaller to reveal the animated border)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.5.dp) // Border thickness
                    .clip(barShape)
                    .background(Color(0xFA0D1322)) // Solid dark inner part
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val sections = listOf(
                    Triple(0, "Chats", Icons.Default.Chat),
                    Triple(1, "Momentos", Icons.Default.Star),
                    Triple(2, "Clips", Icons.Default.PlayArrow),
                    Triple(3, "Llamadas", Icons.Default.Call),
                    Triple(4, "Gente", Icons.Default.Person)
                )

                sections.forEach { (index, label, icon) ->
                    val selected = currentPage == index
                    
                    val animatedWeight by animateFloatAsState(
                        targetValue = if (selected) 1.8f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "tab_weight"
                    )
                    
                    val animatedBgColor by animateColorAsState(
                        targetValue = if (selected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.Transparent,
                        animationSpec = tween(durationMillis = 350),
                        label = "tab_bg"
                    )
                    
                    val animatedBorderColor by animateColorAsState(
                        targetValue = if (selected) Color(0xFF00E5FF).copy(alpha = 0.3f) else Color.Transparent,
                        animationSpec = tween(durationMillis = 350),
                        label = "tab_border"
                    )
                    
                    val animatedContentColor by animateColorAsState(
                        targetValue = if (selected) Color.White else Color.Gray,
                        animationSpec = tween(durationMillis = 350),
                        label = "tab_content"
                    )

                    Box(
                        modifier = Modifier
                            .weight(animatedWeight)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(animatedBgColor)
                            .border(1.dp, animatedBorderColor, RoundedCornerShape(24.dp))
                            .clickable(
                                onClick = { onPageSelected(index) },
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            )
                            .onGloballyPositioned { coordinates ->
                                if (selected) {
                                    selectedTabX = coordinates.positionInParent().x + (coordinates.size.width / 2f)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Box {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = animatedContentColor,
                                    modifier = Modifier.size(22.dp).zIndex(2f)
                                )
                                
                                // Badge
                                if (index == 0 && totalUnreadCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-4).dp)
                                            .size(16.dp)
                                            .background(Color.Red, CircleShape)
                                            .zIndex(3f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (totalUnreadCount > 99) "99+" else totalUnreadCount.toString(),
                                            color = Color.White,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            
                            AnimatedVisibility(
                                visible = selected,
                                enter = fadeIn(animationSpec = tween(200, delayMillis = 100)) + expandHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)),
                                exit = fadeOut(animationSpec = tween(150)) + shrinkHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
