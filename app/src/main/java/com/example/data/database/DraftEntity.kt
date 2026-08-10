package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val draftId: String, // Para chats usamos el chatId, para historias un UUID único
    val userId: String,
    val type: String, // "chat" o "story"
    val content: String,
    val mediaUrl: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)
