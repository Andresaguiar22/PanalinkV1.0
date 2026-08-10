import re

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "r") as f:
    content = f.read()

replacement_load = """
    private var chatJob: kotlinx.coroutines.Job? = null
    private var realtimeJob: kotlinx.coroutines.Job? = null

    fun loadChatHistory(chatId: String, otherUserId: String) {
        currentChatId = chatId
        currentOtherUserId = otherUserId
        
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            
            // Collect the Flow from Room local database
            messagesRepo.getMessagesFlow(chatId).collect { messages ->
                val profileResult = profilesRepo.getProfile(otherUserId)
                _uiState.value = ChatUiState.Success(
                    messages = messages,
                    otherUser = profileResult.getOrNull()
                )
            }
        }
        
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch(Dispatchers.IO) {
             // Let MessagesRepository stream realtime updates into Room
             messagesRepo.observeMessages(chatId).collect {
                 // Nothing to do here, observer updates DB, and DB updates the Flow
             }
        }
    }
"""

content = re.sub(r'private var chatJob: kotlinx.coroutines.Job\? = null\n\n    fun loadChatHistory\(chatId: String, otherUserId: String\) \{[\s\S]*?    fun loadMoreMessages', replacement_load.strip() + '\n\n    fun loadMoreMessages', content)

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "w") as f:
    f.write(content)
