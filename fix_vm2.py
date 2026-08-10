import re

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("caption = null", "caption = \"\"")
content = content.replace("com.example.ui.components.chat.voice.VoiceGestureEvent.START", "com.example.ui.components.chat.voice.VoiceGestureEvent.StartRecording")
content = content.replace("com.example.ui.components.chat.voice.VoiceGestureEvent.LOCK", "com.example.ui.components.chat.voice.VoiceGestureEvent.LockRecording")
content = content.replace("com.example.ui.components.chat.voice.VoiceGestureEvent.CANCEL", "com.example.ui.components.chat.voice.VoiceGestureEvent.CancelRecording")
content = content.replace("com.example.ui.components.chat.voice.VoiceGestureEvent.SEND", "com.example.ui.components.chat.voice.VoiceGestureEvent.FinishRecording")

# Add else branch to when(event)
content = content.replace("            com.example.ui.components.chat.voice.VoiceGestureEvent.FinishRecording -> {\n                val file = audioRecorder?.stopRecording()", "            com.example.ui.components.chat.voice.VoiceGestureEvent.FinishRecording -> {\n                val file = audioRecorder?.stopRecording()\n")
content = re.sub(r'(\s+)com.example.ui.components.chat.voice.VoiceGestureEvent.FinishRecording -> \{[\s\S]*?\}\n        \}', r'\1com.example.ui.components.chat.voice.VoiceGestureEvent.FinishRecording -> {\n\1    val file = audioRecorder?.stopRecording()\n\1    _recordState.value = RecordState.IDLE\n\1    if (file != null) {\n\1        uploadAndSendMedia(\n\1            file = file,\n\1            mimeType = "audio/mp4",\n\1            typeLabel = "Audio",\n\1            replyToId = replyToId,\n\1            context = context,\n\1            onProgress = onProgress\n\1        )\n\1    }\n\1}\n\1else -> {} // ignore other states\n        }', content)

with open("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt", "w") as f:
    f.write(content)
