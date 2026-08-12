import sys

with open('app/src/main/java/com/example/ui/screen/ChatsListScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    "fun PendingPostCard(post: com.example.data.database.PendingPostEntity) {",
    "@Composable\nfun PendingPostCard(post: com.example.data.database.PendingPostEntity) {"
)

with open('app/src/main/java/com/example/ui/screen/ChatsListScreen.kt', 'w') as f:
    f.write(content)

