package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.launch

object ChatListScrollManager {
    private const val TAG = "ChatListScrollPosMgr"
    private const val PREFS_NAME = "panalink_chat_list_scroll_positions"
    private const val KEY_INDEX = "last_index"
    private const val KEY_OFFSET = "last_offset"

    private var inMemoryIndex: Int? = null
    private var inMemoryOffset: Int? = null

    fun savePosition(context: Context, index: Int, offset: Int) {
        if (index < 0) return
        inMemoryIndex = index
        inMemoryOffset = offset
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putInt(KEY_INDEX, index)
                .putInt(KEY_OFFSET, offset)
                .apply()
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to persist scroll position", e)
        }
    }

    fun getPosition(context: Context): Pair<Int, Int>? {
        // Check in-memory first
        if (inMemoryIndex != null && inMemoryOffset != null) {
            return Pair(inMemoryIndex!!, inMemoryOffset!!)
        }

        // Fallback to SharedPreferences
        return try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.contains(KEY_INDEX)) {
                val index = prefs.getInt(KEY_INDEX, 0)
                val offset = prefs.getInt(KEY_OFFSET, 0)
                inMemoryIndex = index
                inMemoryOffset = offset
                Pair(index, offset)
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to read scroll position", e)
            null
        }
    }
}
