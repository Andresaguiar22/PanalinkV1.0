package com.example.util

import android.content.Context
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.launch

object ChatScrollPositionManager {
    private const val TAG = "ChatScrollPosMgr"
    private const val PREFS_NAME = "panalink_chat_scroll_positions"

    private val inMemoryPositions = ConcurrentHashMap<String, Pair<Int, Int>>()

    fun savePosition(context: Context, chatId: String, index: Int, offset: Int) {
        if (chatId.isBlank() || index < 0) return
        inMemoryPositions[chatId] = Pair(index, offset)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt("${chatId}_index", index)
                .putInt("${chatId}_offset", offset)
                .apply()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to persist scroll position to SharedPreferences", e)
        }
    }

    fun getPosition(context: Context, chatId: String): Pair<Int, Int>? {
        if (chatId.isBlank()) return null
        
        // Check in-memory first
        inMemoryPositions[chatId]?.let { return it }

        // Fallback to SharedPreferences
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.contains("${chatId}_index")) {
                val index = prefs.getInt("${chatId}_index", 0)
                val offset = prefs.getInt("${chatId}_offset", 0)
                val pos = Pair(index, offset)
                inMemoryPositions[chatId] = pos
                pos
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to read scroll position from SharedPreferences", e)
            null
        }
    }

    fun clearPosition(context: Context, chatId: String) {
        inMemoryPositions.remove(chatId)
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .remove("${chatId}_index")
                .remove("${chatId}_offset")
                .apply()
        } catch (e: Throwable) {}
    }
}
