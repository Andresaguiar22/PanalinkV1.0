package com.example.di

import android.content.Context
import com.example.data.database.PanalinkDatabase
import com.example.data.database.MessageDao

object DatabaseModule {
    fun provideDatabase(context: Context): PanalinkDatabase {
        return PanalinkDatabase.getDatabase(context)
    }

    fun provideMessageDao(context: Context): MessageDao {
        return provideDatabase(context).messageDao()
    }
}
