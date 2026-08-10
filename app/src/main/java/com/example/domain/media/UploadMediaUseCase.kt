package com.example.domain.media

import android.content.Context
import com.example.data.repository.UploadRepository
import com.example.data.model.UploadMediaResult
import com.example.core.error.ResultState
import com.example.core.error.ErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UploadMediaUseCase(private val context: Context) {
    private val uploadRepository by lazy { UploadRepository() }

    suspend operator fun invoke(
        file: File,
        mimeType: String,
        caption: String,
        userId: String
    ): ResultState<UploadMediaResult> = withContext(Dispatchers.IO) {
        try {
            val result = uploadRepository.uploadVideo(file, mimeType, caption, userId)
            if (result.isSuccess) {
                ResultState.Success(result.getOrThrow())
            } else {
                ResultState.Error(result.exceptionOrNull() ?: Exception("Error al subir multimedia"))
            }
        } catch (e: Exception) {
            ResultState.Error(ErrorMapper.map(e))
        }
    }
}
