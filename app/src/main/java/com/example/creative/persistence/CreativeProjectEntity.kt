package com.example.creative.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "creative_projects")
data class CreativeProjectEntity(
    @PrimaryKey val id: String,
    val sourceMedia: String,
    val type: String, // STORY, REEL, POST, CHAT_EDIT, PROFILE, STICKER
    val layersJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val tempExportPath: String? = null
)
