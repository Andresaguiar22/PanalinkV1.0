package com.example.data.repository

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class OEmbedResponse(
    @Json(name = "title") val title: String? = null,
    @Json(name = "author_name") val authorName: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "type") val type: String? = null
)

interface YouTubeApiService {
    @GET("oembed")
    suspend fun getOEmbedInfo(
        @Query("url") url: String,
        @Query("format") format: String = "json"
    ): Response<OEmbedResponse>
}

object YouTubeRepository {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.youtube.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(YouTubeApiService::class.java)

    suspend fun getVideoMetadata(videoId: String): OEmbedResponse? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val response = apiService.getOEmbedInfo(url = url)
            if (response.isSuccessful) {
                return@withContext response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
