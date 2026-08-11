package com.example.ui.components.chat.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.chat.media.DownloadProgressOverlay

/**
 * Componente principal para visualizar imágenes y videos en las burbujas de chat.
 * Soporta elementos individuales, cuadrículas de múltiples imágenes, subtítulos/links y overlays de carga.
 */
@Composable
fun MediaMessageBubble(
    mediaUrls: List<String>,
    isVideo: Boolean = false,
    thumbnailUrl: String? = null,
    durationLabel: String? = null, // ej. "1:06"
    captionText: String? = null,
    bubbleColor: Color = Color(0xFF1F2C34),
    isDownloading: Boolean = false,
    isUploading: Boolean = false,
    progress: Float? = null,
    onMediaClick: (index: Int, url: String) -> Unit = { _, _ -> },
    onCancelProgress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bubbleColor)
            .padding(3.dp)
    ) {
        Column {
            if (mediaUrls.size > 1 && !isVideo) {
                // Cuadrícula de múltiples imágenes
                MultiImageGridBubble(
                    imageUrls = mediaUrls,
                    onImageClick = { index, url -> onMediaClick(index, url) }
                )
            } else if (mediaUrls.isNotEmpty()) {
                // Imagen o Video Individual
                SingleMediaView(
                    url = mediaUrls.first(),
                    isVideo = isVideo,
                    thumbnailUrl = thumbnailUrl ?: mediaUrls.first(),
                    durationLabel = durationLabel,
                    onMediaClick = { onMediaClick(0, mediaUrls.first()) }
                )
            }

            // Indicador de Subtítulo / Enlace (ej. TikTok, YouTube o texto descriptivo)
            if (!captionText.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = captionText!!,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Overlay de Progreso de Carga / Descarga
        DownloadProgressOverlay(
            isVisible = isDownloading || isUploading || progress != null,
            progress = progress,
            isUploading = isUploading,
            onCancelOrRetryClick = onCancelProgress,
            statusText = if (isUploading) "Subiendo..." else null
        )
    }
}

/**
 * Vista previa para un solo archivo multimedia (Imagen o Video grande)
 */
@Composable
private fun SingleMediaView(
    url: String,
    isVideo: Boolean,
    thumbnailUrl: String,
    durationLabel: String?,
    onMediaClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 320.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable { onMediaClick() },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = if (isVideo) thumbnailUrl else url,
            contentDescription = if (isVideo) "Video preview" else "Imagen",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 320.dp),
            contentScale = ContentScale.Crop
        )

        if (isVideo) {
            // Capa semitransparente oscura sobre la miniatura del video
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            )

            // Botón Central de Play
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproducir Video",
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Duración del video en la esquina inferior izquierda
            if (!durationLabel.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = durationLabel!!,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cuadrícula adaptable para múltiples imágenes
 * - 2 imágenes: 2 columnas iguales
 * - 3 imágenes: 1 grande a la izquierda, 2 pequeñas alineadas a la derecha
 * - 4+ imágenes: Cuadrícula 2x2. Si hay más de 4, la 4ª tiene overlay "+N"
 */
@Composable
fun MultiImageGridBubble(
    imageUrls: List<String>,
    onImageClick: (index: Int, url: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val count = imageUrls.size

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        when {
            count == 2 -> {
                // 2 Columnas iguales
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GridImageItem(
                        url = imageUrls[0],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onImageClick(0, imageUrls[0]) }
                    )
                    GridImageItem(
                        url = imageUrls[1],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onImageClick(1, imageUrls[1]) }
                    )
                }
            }
            count == 3 -> {
                // 1 grande izquierda, 2 pequeñas derecha
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GridImageItem(
                        url = imageUrls[0],
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = { onImageClick(0, imageUrls[0]) }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        GridImageItem(
                            url = imageUrls[1],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            onClick = { onImageClick(1, imageUrls[1]) }
                        )
                        GridImageItem(
                            url = imageUrls[2],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            onClick = { onImageClick(2, imageUrls[2]) }
                        )
                    }
                }
            }
            else -> {
                // 4 o más: Cuadrícula 2x2
                val remainingCount = count - 4
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        GridImageItem(
                            url = imageUrls[0],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { onImageClick(0, imageUrls[0]) }
                        )
                        GridImageItem(
                            url = imageUrls[1],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { onImageClick(1, imageUrls[1]) }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        GridImageItem(
                            url = imageUrls[2],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { onImageClick(2, imageUrls[2]) }
                        )
                        GridImageItem(
                            url = imageUrls[3],
                            overlayCount = if (remainingCount > 0) remainingCount else null,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onClick = { onImageClick(3, imageUrls[3]) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Elemento individual dentro de la cuadrícula de imágenes
 */
@Composable
private fun GridImageItem(
    url: String,
    modifier: Modifier = Modifier,
    overlayCount: Int? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        if (overlayCount != null && overlayCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overlayCount",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
