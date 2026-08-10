import re

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "r") as f:
    content = f.read()

# Fix the map inside decryptMessages which is a suspend function
content = content.replace("return messages.map { com.example.util.CryptoManager.decryptMessageIfNeeded(it) }", """        val result = mutableListOf<Message>()
        for (m in messages) {
            result.add(com.example.util.CryptoManager.decryptMessageIfNeeded(m))
        }
        return result""")

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "w") as f:
    f.write(content)
