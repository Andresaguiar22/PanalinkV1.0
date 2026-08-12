import sys

with open('app/src/main/java/com/example/data/repository/FeedRepositoryImpl.kt', 'r') as f:
    content = f.read()

# Add to interface
content = content.replace(
    'suspend fun toggleLike(postId: String, userId: String, isLiked: Boolean): Result<Unit>',
    'suspend fun toggleLike(postId: String, userId: String, isLiked: Boolean): Result<Unit>\n    suspend fun sharePost(postId: String, userId: String): Result<Unit>'
)

# Add implementation
share_impl = """    override suspend fun sharePost(postId: String, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val database = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
        val postDao = database.postDao()
        val pendingDao = database.pendingSocialActionDao()
        val existing = postDao.getPostById(postId)
        
        // 1. Update Room immediately
        if (existing != null) {
            postDao.upsert(existing.copy(shareCount = existing.shareCount + 1))
        }

        // 2. Queue the action locally
        val action = com.example.data.database.PendingSocialActionEntity(
            localActionId = java.util.UUID.randomUUID().toString(),
            userId = userId,
            targetId = postId,
            actionType = "SHARE",
            payload = null,
            isReel = false
        )
        pendingDao.insertAction(action)

        // 3. Enqueue Background Sync
        com.example.worker.SocialSyncWorker.enqueue(com.example.PanaApplication.instance)
        
        Result.success(Unit)
    }

"""

content = content.replace(
    '    override suspend fun addComment(',
    share_impl + '    override suspend fun addComment('
)

with open('app/src/main/java/com/example/data/repository/FeedRepositoryImpl.kt', 'w') as f:
    f.write(content)
