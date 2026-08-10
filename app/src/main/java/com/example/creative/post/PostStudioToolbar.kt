package com.example.creative.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PostToolItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val tint: Color = Color(0xFF38BDF8)
)

/**
 * P6.6.3 - Post Studio Toolbar
 * Bottom action tools bar for adding layers, applying filters, crop, text, stickers, drawing, inspector.
 */
@Composable
fun PostStudioToolbar(
    onToolSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tools = listOf(
        PostToolItem("media", "Multimedia", Icons.Default.PhotoLibrary, Color(0xFF38BDF8)),
        PostToolItem("ai", "Asistente AI", Icons.Default.AutoAwesome, Color(0xFFFF007A)),
        PostToolItem("text", "Texto", Icons.Default.TextFields, Color(0xFFA855F7)),
        PostToolItem("sticker", "Stickers", Icons.Default.EmojiEmotions, Color(0xFFF59E0B)),
        PostToolItem("filter", "Filtros", Icons.Default.ColorLens, Color(0xFF10B981)),
        PostToolItem("draw", "Dibujar", Icons.Default.Edit, Color(0xFFEC4899)),
        PostToolItem("ratio", "Aspecto", Icons.Default.AspectRatio, Color(0xFF6366F1)),
        PostToolItem("inspector", "Inspector", Icons.Default.Tune, Color(0xFF00E5FF)),
        PostToolItem("caption", "Texto Post", Icons.Default.Article, Color(0xFF14B8A6))
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF090D16))
            .padding(vertical = 8.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(tools) { tool ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToolSelected(tool.id) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(tool.tint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.name,
                            tint = tool.tint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tool.name,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
