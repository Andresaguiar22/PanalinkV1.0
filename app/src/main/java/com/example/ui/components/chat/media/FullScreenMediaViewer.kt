package com.example.ui.components.chat.media

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

/**
 * Visor a Pantalla Completa para Fotos y Videos en el Chat.
 * Fondo negro puro, pinch-to-zoom para fotos, ExoPlayer para videos, barra de controles,
 * velocidad de reproducción y acciones de guardar en galería y compartir.
 */
@OptIn(UnstableApi::class)
@Composable
fun FullScreenMediaViewer(
    mediaUrl: String,
    isVideo: Boolean = false,
    title: String? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            if (isVideo) {
                VideoViewerContent(
                    videoUrl = mediaUrl,
                    showControls = showControls,
                    onToggleControls = { showControls = !showControls }
                )
            } else {
                ImageViewerContent(
                    imageUrl = mediaUrl,
                    onToggleControls = { showControls = !showControls }
                )
            }

            // Top Action Bar con Botón de Cerrar, Guardar y Compartir
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White
                            )
                        }
                        if (!title.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title!!,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botón Guardar / Descargar en Galería Local
                        IconButton(
                            onClick = {
                                downloadMediaToGallery(context, mediaUrl, isVideo)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Guardar en Galería",
                                tint = Color.White
                            )
                        }

                        // Botón Compartir
                        IconButton(
                            onClick = {
                                shareMediaUrl(context, mediaUrl, isVideo)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Contenido interactivo para la visualización de imágenes con zoom táctil y gesto de doble toque
 */
@Composable
private fun ImageViewerContent(
    imageUrl: String,
    onToggleControls: () -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val animatedScale by animateFloatAsState(targetValue = scale, label = "zoomScale")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Foto en pantalla completa",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = animatedScale,
                    scaleY = animatedScale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Reproductor de video integrado con ExoPlayer, SeekBar, controles y selector de velocidad
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoViewerContent(
    videoUrl: String,
    showControls: Boolean,
    onToggleControls: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUrl))
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        durationMs = duration.coerceAtLeast(0L)
                    }
                }
            })
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(300)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onToggleControls() },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay de Controles Reproductor
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Botón central de Play / Pausa
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Barra inferior con SeekBar, tiempo e indicador de velocidad (1.0x, 1.5x, 2.0x)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Formato de Tiempo (ej. 00:03 / 01:06)
                        val formattedCurrent = formatDurationMs(currentPositionMs)
                        val formattedTotal = formatDurationMs(durationMs)
                        Text(
                            text = "$formattedCurrent / $formattedTotal",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Selector de Velocidad de Reproducción
                        TextButton(
                            onClick = {
                                playbackSpeed = when (playbackSpeed) {
                                    1.0f -> 1.25f
                                    1.25f -> 1.5f
                                    1.5f -> 2.0f
                                    else -> 1.0f
                                }
                                exoPlayer.setPlaybackSpeed(playbackSpeed)
                            },
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${playbackSpeed}x",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Slider de reproducción
                    val sliderValue = if (durationMs > 0) currentPositionMs.toFloat() / durationMs.toFloat() else 0f
                    Slider(
                        value = sliderValue.coerceIn(0f, 1f),
                        onValueChange = { fraction ->
                            val targetMs = (fraction * durationMs).toLong()
                            exoPlayer.seekTo(targetMs)
                            currentPositionMs = targetMs
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00A884),
                            activeTrackColor = Color(0xFF00A884),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Función auxiliar para formatear milisegundos en MM:SS
 */
private fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

/**
 * Guarda o descarga una imagen/video en el almacenamiento local con DownloadManager
 */
private fun downloadMediaToGallery(context: Context, mediaUrl: String, isVideo: Boolean) {
    try {
        val extension = if (isVideo) "mp4" else "jpg"
        val fileName = "PanaLink_${System.currentTimeMillis()}.$extension"
        
        if (mediaUrl.startsWith("/") || mediaUrl.startsWith("file://")) {
            val sourceFile = java.io.File(mediaUrl.replace("file://", ""))
            if (sourceFile.exists()) {
                val targetDir = Environment.getExternalStoragePublicDirectory(
                    if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                )
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = java.io.File(targetDir, fileName)
                sourceFile.copyTo(targetFile, overwrite = true)
                Toast.makeText(context, "Guardado en la galería...", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val request = DownloadManager.Request(Uri.parse(mediaUrl)).apply {
            setTitle(fileName)
            setDescription("Guardando multimedia de PanaLink...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(
                if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES,
                fileName
            )
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Guardando en la galería...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Error al guardar el archivo", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Comparte la URL del archivo multimedia utilizando el Intent nativo de Android
 */
private fun shareMediaUrl(context: Context, mediaUrl: String, isVideo: Boolean) {
    try {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (isVideo) "video/*" else "image/*"
            if (mediaUrl.startsWith("/") || mediaUrl.startsWith("file://")) {
                val file = java.io.File(mediaUrl.replace("file://", ""))
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                putExtra(Intent.EXTRA_TEXT, mediaUrl)
            }
        }
        context.startActivity(Intent.createChooser(shareIntent, "Compartir multimedia con..."))
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo compartir el archivo", Toast.LENGTH_SHORT).show()
    }
}
