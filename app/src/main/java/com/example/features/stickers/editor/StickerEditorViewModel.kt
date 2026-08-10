package com.example.features.stickers.editor

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.StickerRepository
import com.example.data.model.StickerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StickerEditorViewModel : ViewModel() {

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _processedImageUri = MutableStateFlow<Uri?>(null)
    val processedImageUri: StateFlow<Uri?> = _processedImageUri.asStateFlow()

    private val _mediaType = MutableStateFlow("image/webp")

    private val _successUrl = MutableStateFlow<String?>(null)
    val successUrl: StateFlow<String?> = _successUrl.asStateFlow()

    fun processImage(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            val file = StickerProcessor.processImageToSticker(context, uri)
            if (file != null) {
                _processedImageUri.value = Uri.fromFile(file)
                _mediaType.value = "image/webp"
            }
            _isProcessing.value = false
        }
    }


    fun processVideo(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            val file = StickerProcessor.processVideoToSticker(context, uri)
            if (file != null) {
                _processedImageUri.value = Uri.fromFile(file)
                _mediaType.value = "video/mp4"
            }
            _isProcessing.value = false
        }
    }


    fun saveSticker(context: Context, emoji: String) {
        val uri = _processedImageUri.value ?: return
        val file = java.io.File(uri.path!!)
        
        viewModelScope.launch {
            _isProcessing.value = true
            val result = StickerCreationRepository.uploadAndCreateSticker(
                context = context,
                file = file,
                name = "Sticker",
                emoji = emoji,
                mimeType = _mediaType.value
            )
            
            if (result.isSuccess) {
                val url = result.getOrThrow()
                // Save locally so it appears in recent/saved
                val stickerResult = StickerResult(url = url, preview = url)
                StickerRepository.saveSticker(context, stickerResult)
                _successUrl.value = url
            } else {
                val err = result.exceptionOrNull()?.message ?: "Error desconocido"
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Error: $err", android.widget.Toast.LENGTH_LONG).show()
                }
            }
            _isProcessing.value = false
        }
    }

    fun reset() {
        _processedImageUri.value = null
        _successUrl.value = null
        _isProcessing.value = false
    }
}
