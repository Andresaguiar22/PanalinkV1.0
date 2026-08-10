import re

with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'r') as f:
    content = f.read()

target = """                        val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
                        db.messageDao().insertMessage(com.example.data.database.MessageEntity.fromMessage(optimisticMsg))"""

replacement = """                        val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
                        db.messageDao().insertMessage(com.example.data.database.MessageEntity.fromMessage(optimisticMsg))
                        com.example.data.repository.StickerRepository.addRecentSticker(
                            com.example.PanaApplication.instance,
                            com.example.data.model.StickerResult(url = stickerUrl, preview = previewUrl)
                        )"""

if target in content:
    with open('app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt', 'w') as f:
        f.write(content.replace(target, replacement, 1))
    print("Success")
else:
    print("Target not found")
