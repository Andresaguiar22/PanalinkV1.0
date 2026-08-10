package com.example.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.example.data.model.MessageReaction

@Entity(tableName = "message_reactions", primaryKeys = ["thread_message_id", "user_id"])
data class ReactionEntity(
    @ColumnInfo(name = "thread_message_id") val threadMessageId: String,
    @ColumnInfo(name = "user_id") val userId: String,
    val emoji: String,
    @ColumnInfo(name = "created_at") val createdAt: String? = null
) {
    fun toModel(): MessageReaction = MessageReaction(
        threadMessageId = threadMessageId,
        userId = userId,
        emoji = emoji,
        createdAt = createdAt
    )

    companion object {
        fun fromModel(reaction: MessageReaction): ReactionEntity = ReactionEntity(
            threadMessageId = reaction.threadMessageId,
            userId = reaction.userId,
            emoji = reaction.emoji,
            createdAt = reaction.createdAt
        )
    }
}
