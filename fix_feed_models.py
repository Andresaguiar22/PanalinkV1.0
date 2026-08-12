import sys

with open('app/src/main/java/com/example/data/model/FeedModels.kt', 'r') as f:
    content = f.read()

# Add sharesCount to PostDto
content = content.replace(
    '@Json(name = "comments_count") val commentsCount: Int = 0,',
    '@Json(name = "comments_count") val commentsCount: Int = 0,\n    @Json(name = "shares_count") val sharesCount: Int = 0,'
)

# Add PostShareDto at the end
post_share_dto = """

@Immutable
@JsonClass(generateAdapter = true)
data class PostShareDto(
    @Json(name = "post_id") val postId: String,
    @Json(name = "user_id") val userId: String
)
"""
content += post_share_dto

with open('app/src/main/java/com/example/data/model/FeedModels.kt', 'w') as f:
    f.write(content)
