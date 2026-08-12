import sys

with open('app/src/main/java/com/example/ui/components/FeedPostCard.kt', 'r') as f:
    content = f.read()

content = content.replace("import com.example.media.ui.VideoPreviewPlayer\n", "")
content = content.replace("com.example.media.ui.VideoPreviewPlayer", "SimpleVideoPreviewPlayer")

with open('app/src/main/java/com/example/ui/components/FeedPostCard.kt', 'w') as f:
    f.write(content)

