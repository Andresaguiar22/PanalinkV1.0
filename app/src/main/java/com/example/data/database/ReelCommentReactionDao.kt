package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ReelCommentReactionDao {

    @Query(
        """
        SELECT *
        FROM reel_comment_reactions
        WHERE commentId = :commentId
        AND userId = :userId
        LIMIT 1
        """
    )
    fun observeUserReaction(
        commentId: String,
        userId: String
    ): Flow<ReelCommentReactionEntity?>

    @Query(
        """
        SELECT *
        FROM reel_comment_reactions
        WHERE commentId = :commentId
        AND userId = :userId
        LIMIT 1
        """
    )
    suspend fun getReaction(
        commentId: String,
        userId: String
    ): ReelCommentReactionEntity?

    @Query(
        """
        SELECT *
        FROM reel_comment_reactions
        WHERE commentId IN (:commentIds)
        AND userId = :userId
        """
    )
    fun observeUserReactions(
        commentIds: List<String>,
        userId: String
    ): Flow<List<ReelCommentReactionEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReaction(
        reaction: ReelCommentReactionEntity
    ): Long

    @Query(
        """
        UPDATE reel_comment_reactions
        SET reaction = :reaction,
            updatedAt = :updatedAt
        WHERE commentId = :commentId
        AND userId = :userId
        """
    )
    suspend fun updateReaction(
        commentId: String,
        userId: String,
        reaction: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int

    @Query(
        """
        DELETE FROM reel_comment_reactions
        WHERE commentId = :commentId
        AND userId = :userId
        """
    )
    suspend fun deleteReaction(
        commentId: String,
        userId: String
    ): Int

    @Transaction
    suspend fun upsertReaction(
        reaction: ReelCommentReactionEntity
    ) {
        val inserted = insertReaction(reaction)

        if (inserted == -1L) {
            updateReaction(
                commentId = reaction.commentId,
                userId = reaction.userId,
                reaction = reaction.reaction,
                updatedAt = reaction.updatedAt
            )
        }
    }

    @Transaction
    suspend fun setReaction(
        commentId: String,
        userId: String,
        reaction: String
    ) {
        require(
            reaction == "like" || reaction == "dislike"
        ) {
            "Invalid comment reaction: $reaction"
        }

        val now = System.currentTimeMillis()

        val existing = getReaction(
            commentId = commentId,
            userId = userId
        )

        if (existing == null) {
            insertReaction(
                ReelCommentReactionEntity(
                    commentId = commentId,
                    userId = userId,
                    reaction = reaction,
                    createdAt = now,
                    updatedAt = now
                )
            )
        } else {
            updateReaction(
                commentId = commentId,
                userId = userId,
                reaction = reaction,
                updatedAt = now
            )
        }
    }

    @Query(
        """
        DELETE FROM reel_comment_reactions
        WHERE commentId = :commentId
        """
    )
    suspend fun deleteAllForComment(
        commentId: String
    )
}
