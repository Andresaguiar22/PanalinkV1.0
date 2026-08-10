package com.example.features.stickers.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.features.stickers.domain.Sticker
import com.example.features.stickers.domain.StickerPack
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object StickerRepository {
    private const val TAG = "StickerRepository"
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private var cachedPacks: List<StickerPack>? = null

    suspend fun getCatalog(context: Context): List<StickerPack> = withContext(Dispatchers.IO) {
        cachedPacks?.let { return@withContext it }

        val serverUrl = try {
            val field = BuildConfig::class.java.getField("SERVER_URL")
            field.get(null) as? String ?: "https://panalink.app"
        } catch (e: Exception) {
            "https://panalink.app"
        }

        val catalogUrl = "$serverUrl/stickers/catalog.json"
        try {
            val request = Request.Builder()
                .url(catalogUrl)
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string()
                if (!jsonStr.isNullOrBlank()) {
                    val type = Types.newParameterizedType(List::class.java, StickerPack::class.java)
                    val adapter = moshi.adapter<List<StickerPack>>(type)
                    val packs = adapter.fromJson(jsonStr)
                    if (!packs.isNullOrEmpty()) {
                        Log.d(TAG, "Successfully loaded ${packs.size} sticker packs from catalog")
                        cachedPacks = packs
                        return@withContext packs
                    }
                }
            } else {
                Log.w(TAG, "Catalog request returned HTTP code ${response.code}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load catalog.json from server, loading default PanaLink pack", e)
        }

        val defaultPacks = listOf(getDefaultPanalinkPack(serverUrl))
        cachedPacks = defaultPacks
        defaultPacks
    }

    suspend fun getStickersForPack(context: Context, packId: String): List<Sticker> = withContext(Dispatchers.IO) {
        val packs = getCatalog(context)
        packs.find { it.id == packId }?.stickers ?: emptyList()
    }

    suspend fun getAllStickers(context: Context): List<Sticker> = withContext(Dispatchers.IO) {
        getCatalog(context).flatMap { it.stickers }
    }

    private fun getDefaultPanalinkPack(baseUrl: String): StickerPack {
        val packId = "panalink_pack"
        val stickers = listOf(
            Sticker(id = "pl_001", name = "Pana Hello", imageUrl = "$baseUrl/stickers/panalink_pack/001.webp", emoji = "👋", packId = packId),
            Sticker(id = "pl_002", name = "Pana Like", imageUrl = "$baseUrl/stickers/panalink_pack/002.webp", emoji = "👍", packId = packId),
            Sticker(id = "pl_003", name = "Pana Love", imageUrl = "$baseUrl/stickers/panalink_pack/003.webp", emoji = "❤️", packId = packId),
            Sticker(id = "pl_004", name = "Pana Laugh", imageUrl = "$baseUrl/stickers/panalink_pack/004.webp", emoji = "😂", packId = packId),
            Sticker(id = "pl_005", name = "Pana Fire", imageUrl = "$baseUrl/stickers/panalink_pack/005.webp", emoji = "🔥", packId = packId),
            Sticker(id = "pl_006", name = "Pana Cool", imageUrl = "$baseUrl/stickers/panalink_pack/006.webp", emoji = "😎", packId = packId),
            Sticker(id = "pl_007", name = "Pana Party", imageUrl = "$baseUrl/stickers/panalink_pack/007.webp", emoji = "🎉", packId = packId),
            Sticker(id = "pl_008", name = "Pana Mindblown", imageUrl = "$baseUrl/stickers/panalink_pack/008.webp", emoji = "🤯", packId = packId)
        )
        return StickerPack(
            id = packId,
            name = "PanaLink Oficial",
            coverUrl = "$baseUrl/stickers/panalink_pack/cover.webp",
            stickers = stickers
        )
    }
}
