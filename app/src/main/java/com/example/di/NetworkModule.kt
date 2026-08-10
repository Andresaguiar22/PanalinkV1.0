package com.example.di

import com.example.data.supabase.SupabaseClient

object NetworkModule {
    fun provideSupabaseClient(): SupabaseClient {
        return SupabaseClient
    }
}
