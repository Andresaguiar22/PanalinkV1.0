import sys

with open('app/src/main/java/com/example/worker/SocialSyncWorker.kt', 'r') as f:
    content = f.read()

share_block = """                    "SHARE" -> {
                        val shareDto = com.example.data.model.PostShareDto(postId = action.targetId, userId = action.userId)
                        val response = service.addShare(apiKey, bearer, shareDto)
                        if (response.isSuccessful || response.code() == 409) {
                            success = true
                        }
                    }
"""

content = content.replace(
    '                    "UNLIKE" -> {',
    share_block + '                    "UNLIKE" -> {'
)

with open('app/src/main/java/com/example/worker/SocialSyncWorker.kt', 'w') as f:
    f.write(content)
