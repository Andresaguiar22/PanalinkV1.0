package com.example.creative.crop

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class CropState(
    val rotationDegrees: Float = 0f,
    val cropRectFraction: RectF = RectF(0f, 0f, 1f, 1f), // 0..1 scale
    val isCircular: Boolean = false
)

object ImageCropEngine {
    private const val TAG = "ImageCropEngine"

    suspend fun cropImage(
        sourceFile: File,
        targetFile: File,
        cropState: CropState
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (!sourceFile.exists()) return@withContext null

            val originalBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath) ?: return@withContext null

            val matrix = Matrix()
            if (cropState.rotationDegrees != 0f) {
                matrix.postRotate(cropState.rotationDegrees)
            }

            val rotatedBitmap = if (cropState.rotationDegrees != 0f) {
                Bitmap.createBitmap(
                    originalBitmap,
                    0, 0,
                    originalBitmap.width, originalBitmap.height,
                    matrix, true
                )
            } else {
                originalBitmap
            }

            val left = (cropState.cropRectFraction.left * rotatedBitmap.width).toInt().coerceIn(0, rotatedBitmap.width - 1)
            val top = (cropState.cropRectFraction.top * rotatedBitmap.height).toInt().coerceIn(0, rotatedBitmap.height - 1)
            val width = (cropState.cropRectFraction.width() * rotatedBitmap.width).toInt().coerceIn(1, rotatedBitmap.width - left)
            val height = (cropState.cropRectFraction.height() * rotatedBitmap.height).toInt().coerceIn(1, rotatedBitmap.height - top)

            val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, left, top, width, height)

            FileOutputStream(targetFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            if (originalBitmap != rotatedBitmap) originalBitmap.recycle()
            if (rotatedBitmap != croppedBitmap) rotatedBitmap.recycle()

            Log.i(TAG, "Cropped image saved successfully to ${targetFile.absolutePath}")
            return@withContext targetFile
        } catch (e: Exception) {
            Log.e(TAG, "Error cropping image", e)
            return@withContext null
        }
    }
}
