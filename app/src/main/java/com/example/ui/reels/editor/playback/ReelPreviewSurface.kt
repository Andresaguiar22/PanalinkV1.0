package com.example.ui.reels.editor.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView

@Composable
fun ReelPreviewSurface(controller: ReelPreviewController) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                player = controller.playerForView()
            }
        },
        update = { view -> view.player = controller.playerForView() }
    )
}

@Composable
fun rememberReelPreviewController(): ReelPreviewController {
    val context = LocalContext.current
    val controller = remember(context) { ReelPreviewController(context) }
    DisposableEffect(controller) {
        onDispose { controller.release() }
    }
    return controller
}
