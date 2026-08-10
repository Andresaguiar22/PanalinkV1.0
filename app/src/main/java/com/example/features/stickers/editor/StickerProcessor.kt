package com.example.features.stickers.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object StickerProcessor {
    private const val MAX_SIZE = 512

    suspend fun processImageToSticker(context: Context, imageUri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return@withContext null

            val width = originalBitmap.width
            val height = originalBitmap.height
            
            val scale = if (width > height) {
                MAX_SIZE.toFloat() / width
            } else {
                MAX_SIZE.toFloat() / height
            }

            val finalBitmap = if (scale < 1.0f) {
                val matrix = Matrix()
                matrix.postScale(scale, scale)
                Bitmap.createBitmap(originalBitmap, 0, 0, width, height, matrix, true)
            } else {
                originalBitmap
            }

            val outputFile = File(context.cacheDir, "sticker_${UUID.randomUUID()}.webp")
            FileOutputStream(outputFile).use { out ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    finalBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
                } else {
                    @Suppress("DEPRECATION")
                    finalBitmap.compress(Bitmap.CompressFormat.WEBP, 80, out)
                }
            }

            if (finalBitmap != originalBitmap) {
                finalBitmap.recycle()
            }
            originalBitmap.recycle()

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun processVideoToSticker(context: Context, videoUri: android.net.Uri): File? = withContext(Dispatchers.IO) {
        try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            val timeInMillis = time?.toLongOrNull() ?: 0L
            retriever.release()

            if (timeInMillis > 10000) {
                // Límite de 10 segundos
                return@withContext null
            }

            val inputStream = context.contentResolver.openInputStream(videoUri) ?: return@withContext null
            val outputFile = File(context.cacheDir, "sticker_${UUID.randomUUID()}.mp4")
            java.io.FileOutputStream(outputFile).use { out ->
                inputStream.copyTo(out)
            }
            inputStream.close()
            return@withContext outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}

