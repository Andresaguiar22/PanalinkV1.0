import sys

with open('app/src/main/java/com/example/ui/screen/PostDetailScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("PostCard(", "com.example.ui.components.FeedPostCard(")

with open('app/src/main/java/com/example/ui/screen/PostDetailScreen.kt', 'w') as f:
    f.write(content)

