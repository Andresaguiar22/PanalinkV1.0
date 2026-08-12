import sys

with open('app/src/main/java/com/example/data/repository/FeedRepositoryImpl.kt', 'r') as f:
    content = f.read()

# I need to fix the implementation header for toggleLike.
content = content.replace(
    'override suspend fun toggleLike(postId: String, userId: String, isLiked: Boolean): Result<Unit>\\n    suspend fun sharePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {',
    'override suspend fun toggleLike(postId: String, userId: String, isLiked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {'
)

with open('app/src/main/java/com/example/data/repository/FeedRepositoryImpl.kt', 'w') as f:
    f.write(content)

