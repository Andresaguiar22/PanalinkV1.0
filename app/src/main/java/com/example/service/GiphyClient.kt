package com.example.service

import com.example.data.supabase.SupabaseClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object GiphyClient {
    private const val BASE_URL = "https://api.giphy.com/"
    
    val apiService: GiphyApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(SupabaseClient.moshi)) // Reuse Moshi
            .build()
            .create(GiphyApiService::class.java)
    }
}
