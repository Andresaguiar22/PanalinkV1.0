import sys

with open('app/src/main/java/com/example/ui/viewmodel/FeedViewModel.kt', 'r') as f:
    content = f.read()

share_impl = """
    fun sharePost(post: PostDto) {
        val currentUserId = com.example.data.supabase.SupabaseClient.currentUser?.id ?: return
        
        viewModelScope.launch(errorHandler) {
            feedRepository.sharePost(post.id!!, currentUserId)
        }
    }

"""

content = content.replace(
    '    fun createPost(',
    share_impl + '    fun createPost('
)

with open('app/src/main/java/com/example/ui/viewmodel/FeedViewModel.kt', 'w') as f:
    f.write(content)
