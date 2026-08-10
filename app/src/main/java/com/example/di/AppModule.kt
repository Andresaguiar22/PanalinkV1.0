package com.example.di

import android.content.Context
import com.example.core.network.NetworkConnectivityObserver
import com.example.core.network.ConnectivityObserver

object AppModule {
    fun provideConnectivityObserver(context: Context): ConnectivityObserver {
        return NetworkConnectivityObserver(context)
    }
}
