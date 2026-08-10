package com.example.ui.components.chat.state

import android.content.Context
import android.content.SharedPreferences

object ConversationStateManager {
    private const val PREFS_NAME = "panalink_conv_states"
    private const val KEY_LAST_READ_MSG = "last_read_msg_"
    private const val KEY_SCROLL_INDEX = "scroll_index_"
    private const val KEY_SCROLL_OFFSET = "scroll_offset_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun setLastReadMessageId(context: Context, chatId: String, messageId: String) {
        getPrefs(context).edit().putString(KEY_LAST_READ_MSG + chatId, messageId).apply()
    }

    fun getLastReadMessageId(context: Context, chatId: String): String? {
        return getPrefs(context).getString(KEY_LAST_READ_MSG + chatId, null)
    }

    fun saveScrollPosition(context: Context, chatId: String, index: Int, offset: Int) {
        getPrefs(context).edit()
            .putInt(KEY_SCROLL_INDEX + chatId, index)
            .putInt(KEY_SCROLL_OFFSET + chatId, offset)
            .apply()
    }

    fun getScrollPosition(context: Context, chatId: String): Pair<Int, Int>? {
        val prefs = getPrefs(context)
        if (!prefs.contains(KEY_SCROLL_INDEX + chatId)) return null
        val index = prefs.getInt(KEY_SCROLL_INDEX + chatId, 0)
        val offset = prefs.getInt(KEY_SCROLL_OFFSET + chatId, 0)
        return index to offset
    }
}
