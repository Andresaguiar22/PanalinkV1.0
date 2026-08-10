package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun QrCodeView(pin: String, modifier: Modifier = Modifier, payload: String? = null) {
    val contentToEncode = remember(pin, payload) {
        when {
            !payload.isNullOrEmpty() -> payload
            pin.startsWith("panalink:") -> pin
            else -> "panalink:pin:$pin"
        }
    }
    val qrBitmapState = remember(contentToEncode) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(contentToEncode) {
        if (contentToEncode.isNotEmpty()) {
            try {
                val bitMatrix: com.google.zxing.common.BitMatrix = com.google.zxing.MultiFormatWriter().encode(
                    contentToEncode,
                    com.google.zxing.BarcodeFormat.QR_CODE,
                    512,
                    512
                )
                val width = bitMatrix.width
                val height = bitMatrix.height
                val pixels = IntArray(width * height)
                for (y in 0 until height) {
                    val offset = y * width
                    for (x in 0 until width) {
                        pixels[offset + x] = if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                    }
                }
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
                qrBitmapState.value = bitmap
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val bitmap = qrBitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Código QR Real de tu Pana",
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF25D366))
        }
    }
}
