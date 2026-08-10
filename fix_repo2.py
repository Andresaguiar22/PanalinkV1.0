import re

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "r") as f:
    content = f.read()

# Fix the map inside observeMessages - CryptoManager.decryptMessageIfNeeded is a suspend function
# so it needs to be called inside a coroutine. But map doesn't accept suspend functions by default,
# so we need to either not make it suspend or wrap it. Actually CryptoManager.decryptMessageIfNeeded is suspend.
# But wait, observeMessages uses map with it. Let's fix that.
# Let's change it to map from kotlinx.coroutines.flow which might support suspend, but wait, the error is:
# e: file:///app/src/main/java/com/example/data/repository/MessagesRepository.kt:904:51 Suspend function 'suspend fun decryptMessageIfNeeded(msg: Message): Message' can only be called from a coroutine or another suspend function.

# Let's see how observeMessages handles it.
content = content.replace(".map { com.example.util.CryptoManager.decryptMessageIfNeeded(it) }", ".kotlinx.coroutines.flow.map { com.example.util.CryptoManager.decryptMessageIfNeeded(it) }")
# No wait, I already imported kotlinx.coroutines.flow.map. The issue is that the map inside decryptMessages uses normal List.map which doesn't support suspend.

content = content.replace("return messages.map { com.example.util.CryptoManager.decryptMessageIfNeeded(it) }", 
                          "val result = mutableListOf<Message>()\n        for (m in messages) result.add(com.example.util.CryptoManager.decryptMessageIfNeeded(m))\n        return result")


with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "w") as f:
    f.write(content)
