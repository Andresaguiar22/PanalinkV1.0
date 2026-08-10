import re

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "r") as f:
    content = f.read()

# Fix the getMessagesFlow map
content = content.replace("messageDao.getMessagesForChatFlow(chatId).map { entities ->", "messageDao.getMessagesForChatFlow(chatId).map { entities: List<MessageEntity> ->")

# Fix observeMessages flow map
content = content.replace(".map { com.example.util.CryptoManager.decryptMessageIfNeeded(it) }", ".map { it: Message -> com.example.util.CryptoManager.decryptMessageIfNeeded(it) }")

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "w") as f:
    f.write(content)
