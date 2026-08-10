package com.example.ui.call

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

/**
 * LocalVideoView renders the local front or back camera feed using WebRTC SurfaceViewRenderer.
 */
@Composable
fun LocalVideoView(
    eglContext: EglBase.Context,
    onViewReady: (SurfaceViewRenderer) -> Unit,
    modifier: Modifier = Modifier
) {
    var rendererRef by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(eglContext, null)
                setEnableHardwareScaler(true)
                setMirror(true)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                rendererRef = this
                onViewReady(this)
            }
        },
        modifier = modifier
    )

    DisposableEffect(rendererRef) {
        onDispose {
            LogRelease("LocalVideoView: releasing SurfaceViewRenderer")
            rendererRef?.release()
        }
    }
}

/**
 * RemoteVideoView renders the other peer's camera feed using WebRTC SurfaceViewRenderer.
 */
@Composable
fun RemoteVideoView(
    eglContext: EglBase.Context,
    onViewReady: (SurfaceViewRenderer) -> Unit,
    modifier: Modifier = Modifier
) {
    var rendererRef by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    AndroidView(
        factory = { context ->
            SurfaceViewRenderer(context).apply {
                init(eglContext, null)
                setEnableHardwareScaler(true)
                setMirror(false)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                rendererRef = this
                onViewReady(this)
            }
        },
        modifier = modifier
    )

    DisposableEffect(rendererRef) {
        onDispose {
            LogRelease("RemoteVideoView: releasing SurfaceViewRenderer")
            rendererRef?.release()
        }
    }
}

private fun LogRelease(message: String) {
    android.util.Log.d("VideoRenderers", message)
}
