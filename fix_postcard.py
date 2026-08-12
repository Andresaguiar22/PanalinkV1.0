import sys

with open('app/src/main/java/com/example/ui/components/FeedPostCard.kt', 'r') as f:
    content = f.read()

content = content.replace(
    '    onAudioPlaylistClick: (PostDto) -> Unit = {}',
    '    onAudioPlaylistClick: (PostDto) -> Unit = {},\n    onShareClick: () -> Unit = {}'
)

# Fix Share button
old_share = """                IconButton(
                    onClick = { 
                        Toast.makeText(context, "Compartiendo...", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share"""

new_share = """                IconButton(
                    onClick = { 
                        onShareClick()
                        
                        // Opcional: Compartir nativo (Android Sharesheet)
                        val shareIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "¡Mira esta publicación en PanaLink!\\n${post.content ?: ""}")
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(shareIntent, "Compartir publicación"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share"""
content = content.replace(old_share, new_share)


# Fix Counters
counters_code_old = """            val likesText = if (post.likesCount == 1) "1 Me gusta" else "${post.likesCount} Me gusta"
            val commentsText = if (post.commentsCount == 1) "1 comentario" else "${post.commentsCount} comentarios"
            
            if (post.likesCount > 0 || post.commentsCount > 0) {
                val countersText = buildString {
                    if (post.likesCount > 0) append(likesText)
                    if (post.likesCount > 0 && post.commentsCount > 0) append(" · ")
                    if (post.commentsCount > 0) append(commentsText)
                }"""

counters_code_new = """            val likesText = if (post.likesCount == 1) "1 Me gusta" else "${post.likesCount} Me gusta"
            val commentsText = if (post.commentsCount == 1) "1 comentario" else "${post.commentsCount} comentarios"
            val sharesText = if (post.sharesCount == 1) "1 vez compartido" else "${post.sharesCount} veces compartido"
            
            if (post.likesCount > 0 || post.commentsCount > 0 || post.sharesCount > 0) {
                val countersText = buildString {
                    val parts = mutableListOf<String>()
                    if (post.likesCount > 0) parts.add(likesText)
                    if (post.commentsCount > 0) parts.add(commentsText)
                    if (post.sharesCount > 0) parts.add(sharesText)
                    append(parts.joinToString(" · "))
                }"""

content = content.replace(counters_code_old, counters_code_new)

with open('app/src/main/java/com/example/ui/components/FeedPostCard.kt', 'w') as f:
    f.write(content)

