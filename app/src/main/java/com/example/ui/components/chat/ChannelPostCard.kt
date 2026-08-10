package com.example.ui.components.chat

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Message
import com.example.data.supabase.SupabaseClient

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChannelPostCard(
    message: Message,
    reactions: Map<String, String>?,
    onReact: (String) -> Unit,
    onForward: (Message) -> Unit,
    onOpenComments: (String) -> Unit,
    onImageClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentUid = SupabaseClient.currentUser?.id ?: ""

    // Detect if content has forwarded source
    val forwardedFrom = remember(message.textContent) {
        if (message.textContent.contains("Reenviado de", ignoreCase = true) || message.textContent.contains("Forwarded from", ignoreCase = true)) {
            val lines = message.textContent.lines()
            lines.firstOrNull { it.contains("Reenviado", ignoreCase = true) || it.contains("Forwarded", ignoreCase = true) }
        } else null
    }

    // Detect if message has an APK or downloadable file
    val fileInfo = remember(message) {
        extractFileInfo(message)
    }

    // Format text with clickable links and hashtags
    val formattedText = remember(message.textContent) {
        formatChannelMessageText(message.textContent)
    }

    // Reaction counts calculation (combining defaults with user reactions)
    val reactionCounts = remember(reactions, message.id) {
        val defaultReactions = mapOf(
            "⭐" to 7, "❤️" to 365, "👍" to 42, "😍" to 40,
            "👻" to 24, "🔑" to 15, "🆒" to 14, "💔" to 8, "🥰" to 7
        )
        val counts = defaultReactions.toMutableMap()
        
        // Add real user reactions
        reactions?.values?.forEach { emoji ->
            counts[emoji] = (counts[emoji] ?: 0) + 1
        }
        counts
    }

    var showReactionPicker by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Main Post Card Box
        Surface(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp)),
            color = Color(0xFF1F2C34), // Telegram / WhatsApp dark channel card color
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // 1. Forwarded Header if present
                if (forwardedFrom != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "↪ $forwardedFrom",
                            color = Color(0xFF00E5FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 2. APK / File Box Attachment
                if (fileInfo != null) {
                    ApkFileAttachmentCard(
                        fileInfo = fileInfo,
                        onDownloadClick = {
                            Toast.makeText(context, "Descargando ${fileInfo.fileName}...", Toast.LENGTH_SHORT).show()
                            if (fileInfo.downloadUrl.isNotBlank()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fileInfo.downloadUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 3. Image Media Banner if available
                if (!message.mediaUrl.isNullOrBlank() && message.messageType != "audio") {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Post Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(message.mediaUrl) },
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 4. Main Body Text
                Text(
                    text = formattedText,
                    color = Color.White,
                    fontSize = 14.5.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Reaction Pills Row (FlowRow style)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    reactionCounts.forEach { (emoji, count) ->
                        val mySelectedEmoji = reactions?.get(currentUid)
                        val isMyReaction = mySelectedEmoji == emoji

                        val bgColor by animateColorAsState(
                            targetValue = if (isMyReaction) Color(0xFF0088CC).copy(alpha = 0.35f) else Color(0xFF2A3942),
                            label = "pillBg"
                        )
                        val borderColor by animateColorAsState(
                            targetValue = if (isMyReaction) Color(0xFF00E5FF) else Color.Transparent,
                            label = "pillBorder"
                        )

                        Surface(
                            onClick = { onReact(emoji) },
                            color = bgColor,
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = emoji, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$count",
                                    color = if (isMyReaction) Color(0xFF00E5FF) else Color(0xFF8596A0),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Add Reaction Button
                    Surface(
                        onClick = { showReactionPicker = !showReactionPicker },
                        color = Color(0xFF2A3942),
                        shape = CircleShape,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Reaccionar",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Quick Emoji Picker Row
                AnimatedVisibility(visible = showReactionPicker) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(Color(0xFF182229), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val quickEmojis = listOf("👍", "❤️", "🔥", "😍", "👏", "😮", "🎉", "💯")
                        quickEmojis.forEach { emoji ->
                            Text(
                                text = emoji,
                                fontSize = 20.sp,
                                modifier = Modifier
                                    .clickable {
                                        onReact(emoji)
                                        showReactionPicker = false
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 6. Footer (Views Count + Timestamp + Comments)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onOpenComments(message.id) }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Comentarios",
                            tint = Color(0xFF00E5FF),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Comentarios",
                            color = Color(0xFF00E5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.RemoveRedEye,
                            contentDescription = "Vistas",
                            tint = Color(0xFF8596A0),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "168.8K",
                            color = Color(0xFF8596A0),
                            fontSize = 11.sp
                        )
                        Text(
                            text = "•",
                            color = Color(0xFF8596A0),
                            fontSize = 11.sp
                        )
                        Text(
                            text = formatTimeOnly(message.createdAt),
                            color = Color(0xFF8596A0),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // 7. Share / Forward Floating Action Button next to Card
        Surface(
            onClick = { onForward(message) },
            color = Color(0xFF1F2C34).copy(alpha = 0.85f),
            shape = CircleShape,
            modifier = Modifier
                .size(36.dp)
                .padding(bottom = 2.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Reply,
                    contentDescription = "Reenviar",
                    tint = Color(0xFF8596A0),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ApkFileAttachmentCard(
    fileInfo: ApkFileInfo,
    onDownloadClick: () -> Unit
) {
    var isDownloaded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        color = Color(0xFF263843),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Download Button
            IconButton(
                onClick = {
                    isDownloaded = !isDownloaded
                    onDownloadClick()
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0088CC))
            ) {
                Icon(
                    imageVector = if (isDownloaded) Icons.Default.Check else Icons.Default.ArrowDownward,
                    contentDescription = "Descargar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileInfo.fileName,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = fileInfo.fileSize,
                    color = Color(0xFF8596A0),
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = { /* File options */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opciones",
                    tint = Color(0xFF8596A0)
                )
            }
        }
    }
}

data class ApkFileInfo(
    val fileName: String,
    val fileSize: String,
    val downloadUrl: String
)

private fun extractFileInfo(message: Message): ApkFileInfo? {
    val text = message.textContent
    if (text.contains(".apk", ignoreCase = true) || text.contains("Download", ignoreCase = true) || message.mediaMime?.contains("apk") == true) {
        val lines = text.lines()
        val apkLine = lines.firstOrNull { it.contains(".apk", ignoreCase = true) }
            ?: "YouTube v21.21.91 (PREMIUM).apk"
        val sizeLine = lines.firstOrNull { it.contains("MB") || it.contains("GB") || it.contains("KB") }
            ?: "121,3 MB APK"
        val urlLine = lines.firstOrNull { it.startsWith("http") } ?: message.mediaUrl ?: ""

        return ApkFileInfo(
            fileName = apkLine.replace("Reenviado de", "").trim(),
            fileSize = sizeLine.trim(),
            downloadUrl = urlLine
        )
    }
    return null
}

private fun formatChannelMessageText(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        lines.forEachIndexed { index, line ->
            when {
                line.startsWith("http://") || line.startsWith("https://") -> {
                    withStyle(SpanStyle(color = Color(0xFF00E5FF), textDecoration = TextDecoration.Underline)) {
                        append(line)
                    }
                }
                line.startsWith("#") -> {
                    withStyle(SpanStyle(color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)) {
                        append(line)
                    }
                }
                line.contains("Note:", ignoreCase = true) || line.contains("Title:", ignoreCase = true) || line.contains("Fast Download", ignoreCase = true) -> {
                    val parts = line.split(":", limit = 2)
                    if (parts.size == 2) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White)) {
                            append("${parts[0]}:")
                        }
                        append(parts[1])
                    } else {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(line)
                        }
                    }
                }
                else -> {
                    append(line)
                }
            }
            if (index < lines.size - 1) append("\n")
        }
    }
}

private fun formatTimeOnly(dateStr: String): String {
    return try {
        if (dateStr.length >= 16) {
            dateStr.substring(11, 16)
        } else "10:50 AM"
    } catch (e: Exception) {
        "10:50 AM"
    }
}
