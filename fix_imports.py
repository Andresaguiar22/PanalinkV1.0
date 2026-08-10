import re

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "r") as f:
    content = f.read()

content = content.replace("import kotlinx.coroutines.flow.Flow", "import kotlinx.coroutines.flow.Flow\nimport kotlinx.coroutines.flow.map\nimport kotlinx.coroutines.flow.onEach\nimport kotlinx.coroutines.flow.filter")

with open("app/src/main/java/com/example/data/repository/MessagesRepository.kt", "w") as f:
    f.write(content)
