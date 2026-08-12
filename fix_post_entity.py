import sys

with open('app/src/main/java/com/example/data/database/PostEntity.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'commentsCount = commentsCount,\n            createdAt = createdAt',
    'commentsCount = commentsCount,\n            sharesCount = shareCount,\n            createdAt = createdAt'
)

content = content.replace(
    'commentsCount = dto.commentsCount,\n                currentUserLiked = dto.isLikedByMe',
    'commentsCount = dto.commentsCount,\n                shareCount = dto.sharesCount,\n                currentUserLiked = dto.isLikedByMe'
)

with open('app/src/main/java/com/example/data/database/PostEntity.kt', 'w') as f:
    f.write(content)
