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
            if (deleteForEveryone) {
                messagesRepository.deleteMessageForEveryone(messageId)
            } else {
                messagesRepository.deleteMessageForMe(messageId)
            }
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(ErrorMapper.map(e))
        }
    }
}
