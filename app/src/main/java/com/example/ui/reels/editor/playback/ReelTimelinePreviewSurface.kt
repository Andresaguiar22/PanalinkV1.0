package com.example.ui.reels.editor.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.example.ui.reels.editor.model.ReelProject

@Composable
fun ReelTimelinePreviewSurface(
    project: ReelProject,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = remember(context) { ReelTimelinePlayer(context) }

    LaunchedEffect(project.timeline.tracks) {
        player.setProject(project)
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                player = player.playerForView()
            }
        },
        update = { view -> view.player = player.playerForView() }
    )
}
