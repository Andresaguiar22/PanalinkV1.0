package com.example.creative.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class StudioToolTab {
    TEXT,
    STICKERS,
    MUSIC,
    AI_SUITE,
    FILTERS,
    BEAUTY,
    TRIM,
    SPEED,
    EFFECTS
}

@Composable
fun StudioLayout(
    onCloseClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onSaveDraftClick: () -> Unit,
    onExportClick: () -> Unit,
    activeToolTab: StudioToolTab?,
    onSelectToolTab: (StudioToolTab?) -> Unit,
    previewContent: @Composable BoxScope.() -> Unit,
    timelineContent: @Composable () -> Unit,
    activeToolDrawer: @Composable (StudioToolTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F14))
    ) {
        // ZONA SUPERIOR: Acciones del Proyecto
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color(0xFF16161E))
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onCloseClick,
                    modifier = Modifier.testTag("btn_close_studio")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Cerrar",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onUndoClick,
                    modifier = Modifier.testTag("btn_undo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Undo,
                        contentDescription = "Deshacer",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = onRedoClick,
                    modifier = Modifier.testTag("btn_redo")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Redo,
                        contentDescription = "Rehacer",
                        tint = Color.White
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onSaveDraftClick,
                    modifier = Modifier.testTag("btn_save_draft")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Borrador", color = Color(0xFF00E5FF), fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onExportClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_export_studio")
                ) {
                    Text("Exportar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // ZONA CENTRAL & BARRA LATERAL DERECHA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // ZONA CENTRAL: Vista previa grande
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                previewContent()

                // Tool Drawer Overlay if active
                if (activeToolTab != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .background(Color(0xEE16161E), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                            .padding(16.dp)
                    ) {
                        activeToolDrawer(activeToolTab)
                    }
                }
            }

            // BARRA LATERAL DERECHA: Herramientas Rápidas
            Column(
                modifier = Modifier
                    .width(64.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF16161E))
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                StudioToolIconButton(
                    icon = Icons.Default.TextFields,
                    label = "Texto",
                    isSelected = activeToolTab == StudioToolTab.TEXT,
                    onClick = { onSelectToolTab(if (activeToolTab == StudioToolTab.TEXT) null else StudioToolTab.TEXT) }
                )
                StudioToolIconButton(
                    icon = Icons.Default.EmojiEmotions,
                    label = "Stickers",
                    isSelected = activeToolTab == StudioToolTab.STICKERS,
                    onClick = { onSelectToolTab(if (activeToolTab == StudioToolTab.STICKERS) null else StudioToolTab.STICKERS) }
                )
                StudioToolIconButton(
                    icon = Icons.Default.MusicNote,
                    label = "Música",
                    isSelected = activeToolTab == StudioToolTab.MUSIC,
                    onClick = { onSelectToolTab(if (activeToolTab == StudioToolTab.MUSIC) null else StudioToolTab.MUSIC) }
                )
                StudioToolIconButton(
                    icon = Icons.Default.AutoAwesome,
                    label = "IA Studio",
                    isSelected = activeToolTab == StudioToolTab.AI_SUITE,
                    onClick = { onSelectToolTab(if (activeToolTab == StudioToolTab.AI_SUITE) null else StudioToolTab.AI_SUITE) }
                )
                StudioToolIconButton(
                    icon = Icons.Default.Filter,
                    label = "Filtros",
                    isSelected = activeToolTab == StudioToolTab.FILTERS,
                    onClick = { onSelectToolTab(if (activeToolTab == StudioToolTab.FILTERS) null else StudioToolTab.FILTERS) }
                )
                StudioToolIconButton(
                    icon = Icons.Default.Face,
                    label = "Belleza",
                    isSelected = activeToolTab == StudioToolTab.BEAUTY,
                    onClick = { onSelectToolTab(if (activeToolTab == StudioToolTab.BEAUTY) null else StudioToolTab.BEAUTY) }
                )
                StudioToolIconButton(
                    icon = Icons.Default.ContentCut,
                    label = "Recorte",
                    isSelected = activeToolTab == StudioToolTab.TRIM,
                    onClick = { onSelectToolTab(if (activeToolTab == StudioToolTab.TRIM) null else StudioToolTab.TRIM) }
                )
                StudioToolIconButton(
                    icon = Icons.Default.Speed,
                    label = "Velocidad",
                    isSelected = activeToolTab == StudioToolTab.SPEED,
                    onClick = { onSelectToolTab(if (activeToolTab == StudioToolTab.SPEED) null else StudioToolTab.SPEED) }
                )
            }
        }

        // ZONA INFERIOR: Línea de Tiempo Profesional
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color(0xFF16161E))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            timelineContent()
        }
    }
}

@Composable
private fun StudioToolIconButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) Color(0xFF00E5FF) else Color.Transparent
    val contentColor = if (isSelected) Color.Black else Color.White

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = label,
            color = if (isSelected) Color(0xFF00E5FF) else Color.Gray,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
