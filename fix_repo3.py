import re

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "r") as f:
    content = f.read()

# Fix flow maps
content = content.replace("kotlinx.coroutines.flow.map {", "map {")
content = content.replace("kotlinx.coroutines.flow.map {", "map {")

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "w") as f:
    f.write(content)
