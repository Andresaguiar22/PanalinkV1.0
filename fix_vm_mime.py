import re

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "r") as f:
    content = f.read()

# Replace messageType = safeTypeLabel with messageType = mimeType
content = content.replace("messageType = safeTypeLabel,", "messageType = mimeType,")
content = content.replace("val safeTypeLabel = typeLabel.lowercase(java.util.Locale.ROOT)", "")

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "w") as f:
    f.write(content)
