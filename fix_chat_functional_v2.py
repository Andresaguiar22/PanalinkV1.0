from pathlib import Path

DAO = Path("app/src/main/java/com/example/data/database/MessageDao.kt")
REPO = Path("app/src/main/java/com/example/data/repository/MessagesRepository.kt")
VM = Path("app/src/main/java/com/example/ui/viewmodel/ChatViewModel.kt")

for p in (DAO, REPO, VM):
    if not p.exists():
        raise SystemExit(f"ERROR: no existe {p}")

def replace_once(text, old, new, label):
    n = text.count(old)
    if n != 1:
        raise SystemExit(f"ERROR: {label}: esperaba 1 coincidencia y encontré {n}. No se modificó ningún archivo.")
    return text.replace(old, new, 1)

dao = DAO.read_text()
repo = REPO.read_text()
vm = VM.read_text()

# 1. Resolve realtime thread_id -> canonical local chat id.
# Current ChatEntity already has threadId, so no schema change is needed.
old = '''    @Query("SELECT * FROM local_messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?
'''
new = '''    @Query("SELECT * FROM local_messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?

    @Query("SELECT id FROM local_chats WHERE threadId = :threadId LIMIT 1")
    suspend fun getChatIdByThreadId(threadId: String): String?
'''
dao = replace_once(dao, old, new, "DAO threadId lookup")

old = '''    @Transaction
    suspend fun insertMessage(message: MessageEntity) {
        val existingById = getMessageById(message.id)
        val existingByUuid = if (!message.clientMessageUuid.isNullOrBlank()) {
            getMessagesByUuid(message.clientMessageUuid!!).firstOrNull()
        } else {
            null
        }

        insertMessageRaw(message)
'''
new = '''    @Transaction
    suspend fun insertMessage(message: MessageEntity) {
        val normalizedMessage = if (hasChat(message.chatId) > 0) {
            message
        } else {
            val localChatId = getChatIdByThreadId(message.chatId)
            if (!localChatId.isNullOrBlank()) message.copy(chatId = localChatId) else message
        }

        val existingById = getMessageById(normalizedMessage.id)
        val existingByUuid = if (!normalizedMessage.clientMessageUuid.isNullOrBlank()) {
            getMessagesByUuid(normalizedMessage.clientMessageUuid!!).firstOrNull()
        } else {
            null
        }

        insertMessageRaw(normalizedMessage)
'''
dao = replace_once(dao, old, new, "insertMessage normalization")

old = '''        updateChatMetadataForMessage(message, shouldIncrementUnread)
    }

    @Transaction
    suspend fun insertMessages(messages: List<MessageEntity>) {
        messages.forEach { message ->
            val existingById = getMessageById(message.id)
            val existingByUuid = if (!message.clientMessageUuid.isNullOrBlank()) {
                getMessagesByUuid(message.clientMessageUuid!!).firstOrNull()
            } else {
                null
            }

            insertMessageRaw(message)

            val shouldIncrementUnread = existingById == null && existingByUuid == null
            updateChatMetadataForMessage(message, shouldIncrementUnread)
        }
    }
'''
new = '''        updateChatMetadataForMessage(normalizedMessage, shouldIncrementUnread)
    }

    @Transaction
    suspend fun insertMessages(messages: List<MessageEntity>) {
        messages.forEach { message ->
            val normalizedMessage = if (hasChat(message.chatId) > 0) {
                message
            } else {
                val localChatId = getChatIdByThreadId(message.chatId)
                if (!localChatId.isNullOrBlank()) message.copy(chatId = localChatId) else message
            }

            val existingById = getMessageById(normalizedMessage.id)
            val existingByUuid = if (!normalizedMessage.clientMessageUuid.isNullOrBlank()) {
                getMessagesByUuid(normalizedMessage.clientMessageUuid!!).firstOrNull()
            } else {
                null
            }

            insertMessageRaw(normalizedMessage)

            val shouldIncrementUnread = existingById == null && existingByUuid == null
            updateChatMetadataForMessage(normalizedMessage, shouldIncrementUnread)
        }
    }
'''
dao = replace_once(dao, old, new, "insertMessages normalization")

# 2. Realtime path resolves the canonical chat identity before filtering/merging.
old = '''                    try {
                        val decryptedMsg = com.example.util.CryptoManager.decryptMessageIfNeeded(msg)
                        val effectiveClearedAt = getEffectiveClearedAt(decryptedMsg.chatId, null)
'''
new = '''                    try {
                        val decryptedMsg = com.example.util.CryptoManager.decryptMessageIfNeeded(msg)
                        val identity = resolveChatIdentity(decryptedMsg.chatId)
                        val normalizedMsg = if (!identity.chatId.isNullOrBlank()) {
                            decryptedMsg.copy(chatId = identity.chatId)
                        } else {
                            decryptedMsg
                        }
                        val effectiveClearedAt = getEffectiveClearedAt(normalizedMsg.chatId, null)
'''
repo = replace_once(repo, old, new, "Realtime canonical chat identity")

old = '''                            messageId = decryptedMsg.id,
                            messageClientUuid = decryptedMsg.clientMessageUuid,
                            messageCreatedAt = decryptedMsg.createdAt,
'''
new = '''                            messageId = normalizedMsg.id,
                            messageClientUuid = normalizedMsg.clientMessageUuid,
                            messageCreatedAt = normalizedMsg.createdAt,
'''
repo = replace_once(repo, old, new, "Realtime filter normalized message")

old = '''                            messageDao.mergeAndSaveMessage(com.example.data.database.MessageEntity.fromMessage(decryptedMsg))
                            Log.d(TAG, "MessagesRepository (Realtime): Merged message ${decryptedMsg.id} into Room")
'''
new = '''                            messageDao.mergeAndSaveMessage(com.example.data.database.MessageEntity.fromMessage(normalizedMsg))
                            Log.d(TAG, "MessagesRepository (Realtime): Merged message ${normalizedMsg.id} into Room chat=${normalizedMsg.chatId}")
'''
repo = replace_once(repo, old, new, "Realtime merge normalized message")

# 3. Preserve composer text if sendMessage reports a real failure.
old = '''        viewModelScope.launch {
            messagesRepo.sendMessage(chatId, text, replyToId = replyToId, receiverUid = otherId, isGhost = _isGhostMode.value)
            _inputMessage.value = ""
        }
'''
new = '''        viewModelScope.launch {
            val result = messagesRepo.sendMessage(
                chatId,
                text,
                replyToId = replyToId,
                receiverUid = otherId,
                isGhost = _isGhostMode.value
            )
            if (result.isSuccess) {
                _inputMessage.value = ""
            } else {
                Log.w("ChatViewModel", "Message send failed; preserving composer text")
            }
        }
'''
if old in vm:
    vm = replace_once(vm, old, new, "composer preservation")
else:
    # The current VM may already use a different send path. Do not guess.
    print("WARN: composer block differs from expected current version; leaving ChatViewModel unchanged")

# Commit only after every required assertion passed.
DAO.write_text(dao)
REPO.write_text(repo)
VM.write_text(vm)

print("OK: v2 chat identity fix applied")
print(" - MessageDao resolves threadId to local chatId")
print(" - Realtime messages are normalized before Room merge")
print(" - Existing unread-count protections are preserved")
print(" - Composer is preserved on a reported send failure when the current VM block matches")
