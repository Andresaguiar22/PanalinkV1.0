import sys

with open('app/src/main/java/com/example/ui/screen/PostDetailScreen.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'onLikeClick = { viewModel.toggleLike(post) },',
    'onLikeClick = { viewModel.toggleLike(post) },\n                            onShareClick = { viewModel.sharePost(post) },'
)

with open('app/src/main/java/com/example/ui/screen/PostDetailScreen.kt', 'w') as f:
    f.write(content)
