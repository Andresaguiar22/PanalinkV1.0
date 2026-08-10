package com.example.ui.components.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Bottom Input / Subscription Wall component (`CommentInputBar`).
 * Alternates conditionally between a comment text composer (when `isSubscribed == true`)
 * and a membership wall asking user to join the channel (when `isSubscribed == false`).
 */
@Composable
fun CommentInputBar(
    isSubscribed: Boolean,
    replyingToComment: ChannelCommentItem? = null,
    onSendMessage: (text: String) -> Unit,
    onJoinChannelClick: () -> Unit,
    onCancelReplyClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val barBg = Color(0xFF17212B) // Telegram Dark Bar Surface
    val inputBg = Color(0xFF242F3D)
    val accentBlue = Color(0xFF2AABEE)
    val primaryText = Color.White
    val secondaryText = Color(0xFF8E959B)

    var textState by remember { mutableStateOf("") }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = barBg,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (isSubscribed) {
                // 1. Reply Banner (If replying to a specific comment)
                AnimatedVisibility(
                    visible = replyingToComment != null,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    if (replyingToComment != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E2C3A))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(28.dp)
                                    .background(accentBlue, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Respondiendo a ${replyingToComment.senderName}",
                                    color = accentBlue,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = replyingToComment.textContent,
                                    color = secondaryText,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = onCancelReplyClick) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cancelar respuesta",
                                    tint = secondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Active Comment Input Field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Text Box Container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(inputBg)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (textState.isEmpty()) {
                            Text(
                                text = "Escribe un comentario...",
                                color = secondaryText,
                                fontSize = 14.5.sp
                            )
                        }

                        BasicTextField(
                            value = textState,
                            onValueChange = { textState = it },
                            textStyle = TextStyle(
                                color = primaryText,
                                fontSize = 14.5.sp
                            ),
                            cursorBrush = SolidColor(accentBlue),
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Send Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (textState.isNotBlank()) accentBlue else Color(0xFF2B3A4A))
                            .clickable(enabled = textState.isNotBlank()) {
                                onSendMessage(textState.trim())
                                textState = ""
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar comentario",
                            tint = if (textState.isNotBlank()) Color.White else secondaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // 3. Membership Wall (When `isSubscribed == false`)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Canal restringido",
                        tint = secondaryText,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Solo los miembros de este canal pueden comentar",
                        color = secondaryText,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onJoinChannelClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Unirme al canal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
