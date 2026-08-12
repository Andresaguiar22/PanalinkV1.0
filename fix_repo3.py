import sys

with open('app/src/main/java/com/example/data/repository/FeedRepositoryImpl.kt', 'r') as f:
    content = f.read()

bad_str = "override suspend fun toggleLike(postId: String, userId: String, isLiked: Boolean): Result<Unit>\n    suspend fun sharePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {"
good_str = "override suspend fun toggleLike(postId: String, userId: String, isLiked: Boolean): Result<Unit> = withContext(Dispatchers.IO) {"

content = content.replace(bad_str, good_str)

with open('app/src/main/java/com/example/data/repository/FeedRepositoryImpl.kt', 'w') as f:
    f.write(content)
