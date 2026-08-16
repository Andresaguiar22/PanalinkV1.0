package com.example.ui.reels

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Comment
import com.example.data.model.UserStateWithUser
import com.example.ui.viewmodel.ReelsViewModel
import java.util.Locale

@Composable
fun ReelActionButtonV2(
    icon: ImageVector,
    count: String,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
            Icon(icon, contentDescription = null, tint = if (selected) Color.Red else Color.White, modifier = Modifier.size(30.dp))
        }
        if (count.isNotBlank()) {
            Text(count, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

fun compactCountV2(value: Int): String = when {
    value >= 1_000_000 -> String.format(Locale.US, "%.1fM", value / 1_000_000f).removeSuffix(".0M") + "M"
    value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000f).removeSuffix(".0K") + "K"
    else -> value.toString()
}

fun formatTimeV2(milliseconds: Long): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0L) / 1000L).toInt()
    return String.format(Locale.US, "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

private fun reelShareLink(reel: UserStateWithUser): String = "panalink://reel/${reel.state.id}"

fun shareReelV2(context: Context, reel: UserStateWithUser) {
    val link = reelShareLink(reel)
    val caption = reel.state.caption?.takeIf { it.isNotBlank() }
    val text = if (caption != null) "$caption\n$link" else link
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }, "Compartir Reel"))
}

fun copyReelLinkV2(context: Context, reel: UserStateWithUser) {
    val link = reelShareLink(reel)
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    clipboard?.setPrimaryClip(ClipData.newPlainText("Enlace del Reel", link))
    Toast.makeText(context, "Enlace copiado", Toast.LENGTH_SHORT).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsCommentsSheet(
    viewModel: ReelsViewModel,
    reelId: String,
    comments: List<Comment>,
    onDismiss: () -> Unit,
) {
    var text by remember(reelId) { mutableStateOf("") }

    LaunchedEffect(reelId) {
        viewModel.loadComments(reelId)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Comentarios", fontWeight = FontWeight.Bold)

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(comments, key = { it.id }) { comment ->
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(comment.toString(), modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.deleteComment(reelId, comment.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Escribe un comentario…") },
                singleLine = false,
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cerrar") }
                Spacer(Modifier.padding(4.dp))
                Button(
                    enabled = text.isNotBlank(),
                    onClick = {
                        val clean = text.trim()
                        viewModel.addComment(reelId, clean)
                        text = ""
                    },
                ) { Text("Enviar") }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
