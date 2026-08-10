package com.example.features.stickers.editor

import android.content.Context
import android.util.Log
import com.example.data.supabase.SessionManager
import com.example.data.supabase.SupabaseClient
import com.example.util.PanalinkMediaManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object StickerCreationRepository {
    private val okHttpClient = okhttp3.OkHttpClient.Builder().build()
    private const val TAG = "StickerCreationRepo"

    suspend fun uploadAndCreateSticker(
        context: Context,
        file: File,
        name: String,
        emoji: String,
        mimeType: String = "image/webp"
    ): Result<String> = withContext(Dispatchers.IO) {
        val userId = SupabaseClient.currentUser?.id ?: return@withContext Result.failure(Exception("User not logged in"))
        val accessToken = SupabaseClient.currentToken ?: return@withContext Result.failure(Exception("No access token"))

        // 1. Upload to Storage using MediaManager or directly
        val uploadResult = PanalinkMediaManager.uploadMediaAndThumbnail(
            context = context,
            mediaFile = file,
            mimeType = mimeType,
            typeLabel = "Sticker",
            userId = userId,
            caption = name
        )

        val url = uploadResult.getOrNull()?.url ?: return@withContext Result.failure(uploadResult.exceptionOrNull() ?: Exception("Upload failed"))
        // Return successful URL to be saved locally or via existing RPC
        return@withContext Result.success(url)
    }

    private suspend fun ensurePersonalPackExists(userId: String, accessToken: String): String? {
        return null
    }
}
