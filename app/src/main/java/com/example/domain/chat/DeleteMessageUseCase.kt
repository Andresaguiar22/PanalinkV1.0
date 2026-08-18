package com.example.domain.chat

import android.content.Context
import com.example.data.repository.MessagesRepository
import com.example.core.error.ResultState
import com.example.core.error.ErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DeleteMessageUseCase(private val context: Context) {
    private val messagesRepository by lazy { MessagesRepository.getInstance() }

    suspend operator fun invoke(
        messageId: String,
        deleteForEveryone: Boolean = true
    ): ResultState<Unit> = withContext(Dispatchers.IO) {
        try {
            val result = if (deleteForEveryone) {
                messagesRepository.deleteMessageForEveryone(messageId)
            } else {
                messagesRepository.deleteMessageForMe(messageId)
            }

            if (result.isSuccess) {
                ResultState.Success(Unit)
            } else {
                val error = result.exceptionOrNull()
                    ?: Exception("No se pudo eliminar el mensaje")
                ResultState.Error(ErrorMapper.map(error))
            }
        } catch (e: Exception) {
            ResultState.Error(ErrorMapper.map(e))
        }
    }
}
