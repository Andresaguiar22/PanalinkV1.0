import re

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "r") as f:
    content = f.read()

# Modify loadChatHistory to collect flow from MessagesRepository
replacement_load = """
    private var chatJob: kotlinx.coroutines.Job? = null

    fun loadChatHistory(chatId: String, otherUserId: String) {
        currentChatId = chatId
        currentOtherUserId = otherUserId
        
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            _uiState.value = ChatUiState.Loading
            
            // Collect the Flow instead of single get
            messagesRepo.getMessagesFlow(chatId).collect { messages ->
                val profileResult = profilesRepo.getProfile(otherUserId)
                _uiState.value = ChatUiState.Success(
                    messages = messages,
                    otherUser = profileResult.getOrNull()
                )
            }
        }
    }
"""

content = re.sub(r'    fun loadChatHistory\(chatId: String, otherUserId: String\) \{[\s\S]*?    fun loadMoreMessages', replacement_load.strip() + '\n\n    fun loadMoreMessages', content)

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "w") as f:
    f.write(content)
