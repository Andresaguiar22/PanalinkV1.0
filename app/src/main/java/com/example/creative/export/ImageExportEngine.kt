package com.example.creative.export

import android.content.Context
import android.graphics.*
import android.util.Log
import com.example.creative.core.CreativeLayer
import com.example.creative.core.CreativeProject
import com.example.media.storage.MediaStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class ExportResult {
    data class Success(val exportedFile: File, val durationMs: Long) : ExportResult()
    data class Error(val message: String, val throwable: Throwable? = null) : ExportResult()
}

object ImageExportEngine {
    private const val TAG = "ImageExportEngine"

    suspend fun exportProjectToImage(
        context: Context,
        project: CreativeProject,
        outputWidth: Int = 1080,
        outputHeight: Int = 1920
    ): ExportResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val baseBitmap = if (project.sourceMedia.isNotBlank() && File(project.sourceMedia).exists()) {
                BitmapFactory.decodeFile(project.sourceMedia)
            } else {
                Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888).apply {
                    Canvas(this).drawColor(android.graphics.Color.BLACK)
                }
            }

            val finalBitmap = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(finalBitmap)

            // Draw background image scaled to fill canvas
            val srcRect = Rect(0, 0, baseBitmap.width, baseBitmap.height)
            val destRect = Rect(0, 0, outputWidth, outputHeight)
            canvas.drawBitmap(baseBitmap, srcRect, destRect, null)

            // Draw Filter overlay
            val filterLayer = project.layers.filterIsInstance<CreativeLayer.Filter>().firstOrNull()
            if (filterLayer != null) {
                val overlayPaint = Paint().apply {
                    color = when (filterLayer.filterName.lowercase()) {
                        "cinematic" -> android.graphics.Color.argb(50, 0, 229, 255)
                        "vintage" -> android.graphics.Color.argb(50, 255, 167, 38)
                        "neon" -> android.graphics.Color.argb(50, 233, 30, 99)
                        "warm" -> android.graphics.Color.argb(35, 255, 112, 67)
                        "black_white" -> android.graphics.Color.argb(100, 0, 0, 0)
                        else -> android.graphics.Color.TRANSPARENT
                    }
                }
                if (overlayPaint.color != android.graphics.Color.TRANSPARENT) {
                    canvas.drawRect(destRect, overlayPaint)
                }
            }

            // Draw Drawings
            val drawingPaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            project.layers.filterIsInstance<CreativeLayer.Drawing>().forEach { drawing ->
                if (drawing.points.size >= 2) {
                    drawingPaint.color = try {
                        android.graphics.Color.parseColor(drawing.strokeColorHex)
                    } catch (e: Exception) {
                        android.graphics.Color.RED
                    }
                    drawingPaint.strokeWidth = drawing.strokeWidthDp * (outputWidth / 360f)

                    val path = android.graphics.Path()
                    val first = drawing.points.first()
                    path.moveTo(first.first * outputWidth, first.second * outputHeight)
                    for (i in 1 until drawing.points.size) {
                        val pt = drawing.points[i]
                        path.lineTo(pt.first * outputWidth, pt.second * outputHeight)
                    }
                    canvas.drawPath(path, drawingPaint)
                }
            }

            // Draw Text Layers
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 48f * (outputWidth / 360f)
                typeface = Typeface.DEFAULT_BOLD
            }

            project.layers.filterIsInstance<CreativeLayer.Text>().forEach { textLayer ->
                textPaint.color = try {
                    android.graphics.Color.parseColor(textLayer.colorHex)
                } catch (e: Exception) {
                    android.graphics.Color.WHITE
                }

                val x = textLayer.xFraction * outputWidth
                val y = textLayer.yFraction * outputHeight

                canvas.save()
                canvas.rotate(textLayer.rotation, x, y)
                canvas.drawText(textLayer.text, x, y, textPaint)
                canvas.restore()
            }

            val storage = MediaStorageManager(context)
            val outputFile = File(context.filesDir, "media/feed/export_${UUID.randomUUID()}.jpg")
            outputFile.parentFile?.mkdirs()

            FileOutputStream(outputFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            val duration = System.currentTimeMillis() - startTime
            Log.i(TAG, "Image export completed in ${duration}ms: ${outputFile.absolutePath}")
            ExportResult.Success(outputFile, duration)
        } catch (e: Exception) {
            Log.e(TAG, "Image export failed", e)
            ExportResult.Error("Export failed: ${e.message}", e)
        }
    }
}
