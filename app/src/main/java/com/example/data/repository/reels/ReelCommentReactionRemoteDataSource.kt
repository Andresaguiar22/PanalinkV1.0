package com.example.data.repository.reels

import com.example.data.model.ReelCommentReaction
import com.example.data.supabase.SupabaseClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Supabase REST access for persistent reactions on Reel comments.
 *
 * The table lives in the `social` schema. This class deliberately does not
 * keep UI state; the caller owns optimistic/local persistence and uses this
 * source only for remote reconciliation.
 */
class ReelCommentReactionRemoteDataSource {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val moshi by lazy {
        Moshi.Builder()
            .add(com.example.data.model.EmbeddedProfileAdapter())
            .add(com.example.data.model.ProfileSurrogateAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val adapter by lazy { moshi.adapter(ReelCommentReaction::class.java) }

    private fun baseRequest(path: String): Request.Builder {
        val token = SupabaseClient.currentToken ?: throw IllegalStateException("Session expired")
        return Request.Builder()
            .url("${SupabaseClient.supabaseUrl}/rest/v1/$path")
            .header("apikey", SupabaseClient.supabaseAnonKey)
            .header("Authorization", "Bearer $token")
            .header("Accept-Profile", "social")
            .header("Content-Profile", "social")
            .header("Accept", "application/json")
    }

    suspend fun getForComment(commentId: String): Result<List<ReelCommentReaction>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = baseRequest("reel_comment_reactions")
                .get()
                .url("${SupabaseClient.supabaseUrl}/rest/v1/reel_comment_reactions?comment_id=eq.$commentId&select=*")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}: ${response.body?.string()}")
                val json = response.body?.string().orEmpty()
                val adapterList = moshi.adapter<List<ReelCommentReaction>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, ReelCommentReaction::class.java))
                adapterList.fromJson(json).orEmpty()
            }
        }
    }

    suspend fun set(commentId: String, reaction: String): Result<ReelCommentReaction> = withContext(Dispatchers.IO) {
        runCatching {
            require(reaction.isNotBlank() && reaction.length <= 32) { "Invalid reaction" }
            val body = adapter.toJson(
                ReelCommentReaction(
                    commentId = commentId,
                    userId = SupabaseClient.currentUser?.id ?: error("Not authenticated"),
                    reaction = reaction
                )
            ).toRequestBody("application/json".toMediaType())
            val request = baseRequest("reel_comment_reactions?on_conflict=comment_id,user_id")
                .header("Prefer", "resolution=merge-duplicates,return=representation")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}: ${response.body?.string()}")
                val json = response.body?.string().orEmpty()
                val listAdapter = moshi.adapter<List<ReelCommentReaction>>(com.squareup.moshi.Types.newParameterizedType(List::class.java, ReelCommentReaction::class.java))
                listAdapter.fromJson(json)?.firstOrNull() ?: error("Empty reaction response")
            }
        }
    }

    suspend fun clear(commentId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uid = SupabaseClient.currentUser?.id ?: error("Not authenticated")
            val request = baseRequest("reel_comment_reactions?comment_id=eq.$commentId&user_id=eq.$uid")
                .delete()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 404) {
                    error("HTTP ${response.code}: ${response.body?.string()}")
                }
            }
        }
    }
}
