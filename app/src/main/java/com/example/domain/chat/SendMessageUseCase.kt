package com.example.domain.chat

import android.content.Context
import com.example.data.repository.MessagesRepository
import com.example.core.error.ResultState
import com.example.core.error.ErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SendMessageUseCase(private val context: Context) {
    private val messagesRepository by lazy { MessagesRepository.getInstance() }

    suspend operator fun invoke(
        chatId: String,
        receiverId: String,
        content: String,
        messageType: String = "text"
    ): ResultState<Unit> = withContext(Dispatchers.IO) {
        try {
            messagesRepository.sendMessage(
                chatId = chatId,
                receiverUid = receiverId,
                content = content,
                messageType = messageType
            )
            ResultState.Success(Unit)
        } catch (e: Exception) {
            ResultState.Error(ErrorMapper.map(e))
        }
    }
}
