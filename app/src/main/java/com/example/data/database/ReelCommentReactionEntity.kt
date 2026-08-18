package com.example.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "reel_comment_reactions",
    primaryKeys = ["commentId", "userId"],
    indices = [
        Index(value = ["commentId"]),
        Index(value = ["userId"])
    ]
)
data class ReelCommentReactionEntity(

    @ColumnInfo(name = "commentId")
    val commentId: String,

    @ColumnInfo(name = "userId")
    val userId: String,

    @ColumnInfo(name = "reaction")
    val reaction: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updatedAt")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "syncStatus")
    val syncStatus: String = "synced"
)
