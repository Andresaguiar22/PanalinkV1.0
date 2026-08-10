import re

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("messageType = mimeType,", "messageType = typeLabel.lowercase(java.util.Locale.ROOT),")

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "w") as f:
    f.write(content)
