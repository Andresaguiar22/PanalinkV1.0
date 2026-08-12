import sys

content = open('app/src/main/java/com/example/ui/screen/ViewStateScreen.kt').read()

content = content.replace(
'''    var exoPlayerRef by remember { mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null) }''',
'''    var exoPlayerRef by remember { mutableStateOf<androidx.media3.exoplayer.ExoPlayer?>(null) }
    var hasError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }'''
)

content = content.replace(
'''    var hasError by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }

    Box(modifier = modifier) {''',
'''    Box(modifier = modifier) {'''
)

open('app/src/main/java/com/example/ui/screen/ViewStateScreen.kt', 'w').write(content)
