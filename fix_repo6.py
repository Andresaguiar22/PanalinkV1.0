import re

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "r") as f:
    content = f.read()

content = content.replace("import map\n", "import kotlinx.coroutines.flow.map\n")

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "w") as f:
    f.write(content)
