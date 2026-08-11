package com.example.ui.components.chat.bubble

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.PanalinkMediaManager
import com.example.ui.components.PanaAvatar
import kotlinx.coroutines.launch

@Composable
fun DocumentMessageBubble(
    docUrl: String,
    fileName: String? = null,
    mediaSize: Long? = null,
    bubbleColor: Color = Color(0xFFE7FFDB),
    senderAvatarUrl: String? = null,
    isSender: Boolean = true,
    messageStatus: String? = "sent",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val actualFileName = fileName ?: docUrl.split("/").lastOrNull()?.substringBefore("?") ?: "Documento"
    val extension = actualFileName.split(".").lastOrNull()?.lowercase() ?: "file"
    val (icon, typeColor) = getDocumentFileInfo(extension)

    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var isDownloading by remember { mutableStateOf(false) }
    var localFile by remember { mutableStateOf(PanalinkMediaManager.isFileDownloaded(context, actualFileName)) }

    // Intelligent contrast logic
    val isBubbleLight = bubbleColor.luminance() > 0.45f
    val contentColor = if (isBubbleLight) Color(0xFF111B21) else Color.White
    val secondaryColor = contentColor.copy(alpha = 0.6f)

    val isSending = messageStatus == "sending"
    val isFailed = messageStatus == "failed"

    Surface(
        modifier = modifier
            .widthIn(max = 310.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable {
                if (localFile != null) {
                    PanalinkMediaManager.openFile(context, localFile!!)
                } else if (!isDownloading && !isSending) {
                    isDownloading = true
                    scope.launch {
                        val downloaded = PanalinkMediaManager.downloadMedia(context, docUrl, actualFileName) { 
                            downloadProgress = it 
                        }
                        isDownloading = false
                        localFile = downloaded
                    }
                }
            },
        color = bubbleColor,
        tonalElevation = 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sender Avatar integration for Documents
                if (!isSender && senderAvatarUrl != null) {
                    PanaAvatar(
                        avatarUrl = senderAvatarUrl,
                        modifier = Modifier.size(34.dp).padding(end = 8.dp),
                        size = 34.dp
                    )
                }

                // Polished Icon Box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(typeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFailed) Icons.Default.Error else icon,
                        contentDescription = null,
                        tint = if (isFailed) Color.Red else typeColor,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = actualFileName,
                        color = contentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val formattedSize = if (mediaSize != null && mediaSize > 0) {
                        val mb = mediaSize / (1024f * 1024f)
                        if (mb > 1) String.format("%.1f MB", mb) else String.format("%.0f KB", mediaSize / 1024f)
                    } else null

                    Text(
                        text = buildString {
                            append(extension.uppercase())
                            if (formattedSize != null) append(" • $formattedSize")
                            if (localFile != null) append(" • Descargado")
                        },
                        color = secondaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // Status/Download icon
                Box(
                    modifier = Modifier.size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isDownloading -> {
                            CircularProgressIndicator(
                                progress = { downloadProgress },
                                modifier = Modifier.size(20.dp),
                                color = typeColor,
                                strokeWidth = 2.dp
                            )
                        }
                        isSending -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = typeColor,
                                strokeWidth = 2.dp
                            )
                        }
                        localFile != null -> {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Abrir",
                                tint = secondaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Descargar",
                                tint = secondaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            if (isDownloading || isSending) {
                LinearProgressIndicator(
                    progress = { if (isDownloading) downloadProgress else 0.5f }, // Indeterminate-ish for upload if no progress flow
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = typeColor,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

private fun getDocumentFileInfo(extension: String): Pair<ImageVector, Color> {
    return when (extension) {
        "pdf" -> Icons.Default.PictureAsPdf to Color(0xFFF44336)
        "doc", "docx" -> Icons.Default.Description to Color(0xFF2196F3)
        "xls", "xlsx" -> Icons.Default.TableChart to Color(0xFF4CAF50)
        "ppt", "pptx" -> Icons.Default.PresentToAll to Color(0xFFFF5722)
        "zip", "rar", "7z" -> Icons.Default.FolderZip to Color(0xFFFFC107)
        "apk" -> Icons.Default.Android to Color(0xFF3DDC84)
        "txt" -> Icons.Default.Article to Color(0xFF9E9E9E)
        else -> Icons.Default.InsertDriveFile to Color(0xFF607D8B)
    }
}
