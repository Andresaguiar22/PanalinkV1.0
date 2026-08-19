import re
from pathlib import Path

TARGET = Path("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt")
content = TARGET.read_text()

# Add only the coroutine operators required by the media-send state observer.
imports = [
    "import kotlinx.coroutines.flow.first",
    "import kotlinx.coroutines.flow.mapNotNull",
    "import kotlinx.coroutines.withTimeoutOrNull",
]
anchor = "import kotlinx.coroutines.flow.asStateFlow\n"
for line in imports:
    if line not in content:
        content = content.replace(anchor, anchor + line + "\n", 1)

# Replace only the current uploadAndSendMedia implementation. The temporary
# Room row is already persisted by MessagesRepository and is reconciled by
# MediaUploadWorker using clientMessageUuid. Waiting on the existing chat Flow
# keeps the UI progress tied to the actual worker lifecycle instead of merely
# to WorkManager enqueue success.
pattern = re.compile(
    r"fun uploadAndSendMedia\(\n.*?\n    }\n    \n    fun editMessage",
    re.DOTALL,
)
replacement = '''fun uploadAndSendMedia(
        uri: android.net.Uri? = null,
        file: java.io.File? = null,
        mimeType: String,
        typeLabel: String,
        replyToId: String?,
        context: android.content.Context,
        fileName: String? = null,
        onProgress: (Boolean) -> Unit
    ) {
        val chatId = currentChatId ?: return

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            onProgress(true)

            val result = messagesRepo.sendMultimediaMessage(
                chatId = chatId,
                context = context,
                sourceUri = uri,
                sourceFile = file,
                mimeType = mimeType,
                typeLabel = typeLabel,
                content = "[$typeLabel]",
                replyToId = replyToId,
                isGhost = _isGhostMode.value,
                receiverId = currentOtherUserId
            )

            val message = result.getOrNull()
            val finalMessage = if (message != null) {
                kotlinx.coroutines.withTimeoutOrNull(120_000L) {
                    messagesRepo.getMessagesFlow(chatId)
                        .mapNotNull { messages ->
                            messages.firstOrNull { candidate ->
                                candidate.clientMessageUuid == message.clientMessageUuid &&
                                    candidate.status in setOf("sent", "delivered", "seen", "failed")
                            }
                        }
                        .first()
                }
            } else {
                null
            }

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onProgress(false)

                when {
                    result.isFailure -> {
                        try {
                            android.widget.Toast.makeText(
                                context,
                                "Error procesando archivo",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } catch (t: Throwable) {}
                    }
                    finalMessage?.status == "failed" -> {
                        try {
                            android.widget.Toast.makeText(
                                context,
                                "No se pudo enviar el archivo",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } catch (t: Throwable) {}
                    }
                    finalMessage == null && message != null -> {
                        // The worker remains persisted in WorkManager. Do not
                        // leave a screen-level spinner running forever if the
                        // device is offline or the upload is unusually slow.
                        Log.w(
                            "ChatViewModel",
                            "Media send observer timed out for ${message.clientMessageUuid}; worker continues in background"
                        )
                    }
                }
            }
        }
    }
    
    fun editMessage'''

match = pattern.search(content)
if not match:
    raise SystemExit("ERROR: uploadAndSendMedia block not found; file left unchanged")

content = content[:match.start()] + replacement + content[match.end():]
TARGET.write_text(content)
print("Patched ChatViewModel.kt: media progress now follows Room/Worker terminal state.")
