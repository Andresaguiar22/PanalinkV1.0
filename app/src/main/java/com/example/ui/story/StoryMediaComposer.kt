package com.example.ui.story

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Converts a Story Studio draft into one MP4 file.
 * Images become timed video clips, slides are concatenated and the optional
 * soundtrack is mixed as a second audio-only sequence.
 */
@OptIn(UnstableApi::class)
class StoryMediaComposer(private val context: Context) {

    companion object {
        const val MAX_DURATION_MS = StoryStudioDraft.MAX_DURATION_MS
        private const val DEFAULT_PHOTO_MS = 5_000L
        private const val POLL_MS = 120L
    }

    suspend fun compose(
        draft: StoryStudioDraft,
        outputFile: File,
        onProgress: (Int) -> Unit = {}
    ): File {
        require(draft.slides.isNotEmpty()) { "La historia no contiene contenido" }
        require(draft.slides.size <= StoryStudioDraft.MAX_SLIDES) {
            "Máximo ${StoryStudioDraft.MAX_SLIDES} elementos"
        }

        val durationMs = draft.durationMs()
        require(durationMs in 1..MAX_DURATION_MS) {
            "La historia debe durar entre 1 segundo y 2 minutos"
        }

        draft.slides.forEach { slide ->
            require(!slide.uri.isNullOrBlank()) { "La historia contiene un recurso inválido" }
            require(File(requireNotNull(slide.uri)).exists()) {
                "No se encontró ${slide.uri}"
            }
        }
        draft.audioUri?.let { audioPath ->
            require(File(audioPath).exists()) { "No se encontró el audio" }
        }

        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) outputFile.delete()

        val videoItems = draft.slides.map { slide ->
            val source = MediaItem.Builder()
                .setUri(requireNotNull(slide.uri))
                .apply {
                    if (slide.kind != StoryStudioKind.VIDEO) {
                        setImageDurationMs(
                            slide.durationMs.takeIf { it > 0 } ?: DEFAULT_PHOTO_MS
                        )
                    }
                }
                .build()

            // Build text overlay if present (simplified)
            val videoEffects = if (slide.text.isNotBlank()) {
                listOf(
                    OverlayEffect(
                        listOf(
                            TextOverlay.createStaticTextOverlay(
                                SpannableString(slide.text)
                            )
                        )
                    )
                )
            } else {
                emptyList()
            }

            EditedMediaItem.Builder(source)
                .apply {
                    if (slide.kind != StoryStudioKind.VIDEO && slide.durationMs > 0) {
                        setDurationUs(slide.durationMs * 1_000L)
                    }
                    if (videoEffects.isNotEmpty()) {
                        setEffects(Effects(emptyList(), videoEffects))
                    }
                }
                .build()
        }

        // Build video sequence
        val videoSequence = EditedMediaItemSequence(videoItems)
        val sequences = mutableListOf(videoSequence)

        draft.audioUri?.let { audioPath ->
            val audioMediaItem = MediaItem.Builder()
                .setUri(audioPath)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(draft.audioStartMs.coerceAtLeast(0L))
                        .setEndPositionMs(draft.audioStartMs + durationMs)
                        .build()
                )
                .build()

            val audioEditedItem = EditedMediaItem.Builder(audioMediaItem).build()
            val audioSequence = EditedMediaItemSequence(listOf(audioEditedItem))

            sequences += audioSequence
        }

        val composition = Composition.Builder(sequences).build()
        val mainHandler = Handler(Looper.getMainLooper())

        return suspendCancellableCoroutine { continuation ->
            lateinit var transformer: Transformer

            val listener = object : Transformer.Listener {
                override fun onCompleted(
                    composition: Composition,
                    result: ExportResult
                ) {
                    onProgress(100)
                    if (continuation.isActive) continuation.resume(outputFile)
                }

                override fun onError(
                    composition: Composition,
                    result: ExportResult,
                    exportException: ExportException
                ) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(exportException)
                    }
                }
            }

            transformer = Transformer.Builder(context.applicationContext)
                .addListener(listener)
                .build()

            val progressHolder = ProgressHolder()
            val progressRunnable = object : Runnable {
                override fun run() {
                    if (!continuation.isActive) return

                    if (transformer.getProgress(progressHolder) ==
                        Transformer.PROGRESS_STATE_AVAILABLE
                    ) {
                        onProgress(progressHolder.progress.coerceIn(0, 99))
                    }
                    mainHandler.postDelayed(this, POLL_MS)
                }
            }

            continuation.invokeOnCancellation {
                mainHandler.removeCallbacks(progressRunnable)
                mainHandler.post { transformer.cancel() }
            }

            mainHandler.post {
                try {
                    transformer.start(composition, outputFile.absolutePath)
                    mainHandler.post(progressRunnable)
                } catch (t: Throwable) {
                    mainHandler.removeCallbacks(progressRunnable)
                    if (continuation.isActive) {
                        continuation.resumeWithException(t)
                    }
                }
            }
        }
    }
}
