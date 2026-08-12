import sys

with open('app/src/main/java/com/example/ui/components/FeedPostCard.kt', 'r') as f:
    content = f.read()

# 1. Add imports
imports_to_add = """import com.example.ui.screen.parseStateMetadata
import com.example.ui.screen.RenderOverlays
import com.example.media.ui.VideoPreviewPlayer
"""

content = content.replace('import com.example.data.model.PostDto', imports_to_add + 'import com.example.data.model.PostDto')

# 2. Add isVideoUrl and isAudioUrl at the top level
utils_to_add = """
fun isVideoUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val lower = url.lowercase()
    return lower.contains(".mp4") || lower.contains(".mov") || lower.contains(".webm") || lower.contains(".mkv") || lower.contains("video")
}

fun isAudioUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val lower = url.lowercase()
    return lower.contains(".mp3") || lower.contains(".wav") || lower.contains(".ogg") || lower.contains(".m4a") || lower.contains("audio")
}

@Composable
"""

content = content.replace('@Composable\nfun FeedPostCard(', utils_to_add + 'fun FeedPostCard(')

with open('app/src/main/java/com/example/ui/components/FeedPostCard.kt', 'w') as f:
    f.write(content)

