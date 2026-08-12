import sys

with open('app/src/main/java/com/example/ui/screen/ChatsListScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'onLikeClick = { feedViewModel.toggleLike(post) },',
    'onLikeClick = { feedViewModel.toggleLike(post) },\n                            onShareClick = { feedViewModel.sharePost(post) },'
)

with open('app/src/main/java/com/example/ui/screen/ChatsListScreen.kt', 'w') as f:
    f.write(content)
