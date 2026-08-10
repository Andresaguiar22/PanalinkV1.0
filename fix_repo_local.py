import re

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "r") as f:
    content = f.read()

funcs = """
    suspend fun insertLocalMessage(msg: Message) = withContext(Dispatchers.IO) {
        messageDao.insertMessage(MessageEntity.fromMessage(msg))
    }

    suspend fun updateLocalMessageStatus(id: String, status: String) = withContext(Dispatchers.IO) {
        messageDao.updateMessageStatus(id, status)
    }
"""

content = re.sub(r'    fun getMessagesFlow\(chatId: String\):', funcs.strip() + '\n\n    fun getMessagesFlow(chatId: String):', content)

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "w") as f:
    f.write(content)
