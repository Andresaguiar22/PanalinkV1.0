import re

with open("app/src/main/java/com/example/features/stickers/editor/StickerCreationRepository.kt", "r") as f:
    content = f.read()

content = content.replace(
    "suspend fun uploadAndCreateSticker(\n        context: Context,\n        file: File,\n        name: String,\n        emoji: String,\n        mimeType: String = \"image/webp\"\n    ): String? = withContext(Dispatchers.IO) {",
    "suspend fun uploadAndCreateSticker(\n        context: Context,\n        file: File,\n        name: String,\n        emoji: String,\n        mimeType: String = \"image/webp\"\n    ): Result<String> = withContext(Dispatchers.IO) {"
)

content = content.replace("val userId = SupabaseClient.currentUser?.id ?: return@withContext null", "val userId = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception(\"User not logged in\"))")
content = content.replace("val accessToken = SupabaseClient.currentToken ?: return@withContext null", "val accessToken = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception(\"No access token\"))")

content = content.replace(
    "val url = uploadResult.getOrNull()?.url ?: return@withContext null",
    "val url = uploadResult.getOrNull()?.url ?: return@withContext Result.failure(uploadResult.exceptionOrNull() ?: Exception(\"Upload failed\"))"
)

content = content.replace("return@withContext null", "return@withContext Result.failure(Exception(\"Unknown error\"))")

content = content.replace(
    "Log.e(TAG, \"Failed to insert sticker: ${response.body?.string()}\")\n                return@withContext Result.failure(Exception(\"Unknown error\"))",
    "val err = response.body?.string()\n                Log.e(TAG, \"Failed to insert sticker: $err\")\n                return@withContext Result.failure(Exception(\"DB Insert failed: $err\"))"
)

content = content.replace(
    "return@withContext url",
    "return@withContext Result.success(url)"
)

with open("app/src/main/java/com/example/features/stickers/editor/StickerCreationRepository.kt", "w") as f:
    f.write(content)
