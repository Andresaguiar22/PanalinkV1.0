package com.example.notification.engine.producers.social

import androidx.annotation.Keep
import com.example.notification.engine.model.social.CommentEvent
import com.example.notification.engine.model.social.FollowAction
import com.example.notification.engine.model.social.FollowEvent
import com.example.notification.engine.model.social.PostAction
import com.example.notification.engine.model.social.PostEvent
import com.example.notification.engine.model.social.ReactionEvent
import com.example.notification.engine.model.social.WallEvent

@Keep
object WallNotificationProducer {

    suspend fun publishWallEvent(event: WallEvent) {
        when (event) {
            is PostEvent -> processPostEvent(event)
            is CommentEvent -> processCommentEvent(event)
            is ReactionEvent -> processReactionEvent(event)
            is FollowEvent -> processFollowEvent(event)
        }
    }

    private suspend fun processPostEvent(event: PostEvent) {
        when (event.action) {
            PostAction.CREATE -> PostNotificationAdapter.publishPostCreated(
                postId = event.postId,
                authorId = event.authorId,
                authorName = event.actorName,
                authorAvatarUrl = event.actorAvatarUrl,
                caption = event.postTitleOrCaption,
                previewUrl = event.previewUrl
            )
            PostAction.LIKE -> PostNotificationAdapter.publishPostLike(
                postId = event.postId,
                postAuthorId = event.authorId,
                actorId = event.actorId,
                actorName = event.actorName,
                actorAvatarUrl = event.actorAvatarUrl,
                previewUrl = event.previewUrl
            )
            PostAction.SHARE -> ShareNotificationAdapter.publishPostShared(
                postId = event.postId,
                authorId = event.authorId,
                actorId = event.actorId,
                actorName = event.actorName,
                actorAvatarUrl = event.actorAvatarUrl,
                isRepost = false,
                previewUrl = event.previewUrl
            )
            PostAction.REPOST -> ShareNotificationAdapter.publishPostShared(
                postId = event.postId,
                authorId = event.authorId,
                actorId = event.actorId,
                actorName = event.actorName,
                actorAvatarUrl = event.actorAvatarUrl,
                isRepost = true,
                previewUrl = event.previewUrl
            )
            PostAction.MENTION -> MentionNotificationAdapter.publishMention(
                mentionedUserId = event.authorId,
                postId = event.postId,
                actorId = event.actorId,
                actorName = event.actorName,
                actorAvatarUrl = event.actorAvatarUrl,
                isCommentMention = false,
                contentSnippet = event.postTitleOrCaption,
                previewUrl = event.previewUrl
            )
            else -> { /* Ignore or handle custom actions */ }
        }
    }

    private suspend fun processCommentEvent(event: CommentEvent) {
        CommentNotificationAdapter.publishPostComment(
            postId = event.postId,
            commentId = event.commentId,
            postAuthorId = event.postAuthorId,
            actorId = event.actorId,
            actorName = event.actorName,
            actorAvatarUrl = event.actorAvatarUrl,
            commentText = event.commentText,
            previewUrl = event.mediaPreviewUrl,
            isReply = event.isReply
        )
    }

    private suspend fun processReactionEvent(event: ReactionEvent) {
        ReactionNotificationAdapter.publishReaction(
            entityId = event.entityId,
            entityType = event.entityType,
            authorId = event.authorId,
            actorId = event.actorId,
            actorName = event.actorName,
            actorAvatarUrl = event.actorAvatarUrl,
            reactionEmojiOrType = event.reactionType,
            previewUrl = event.previewUrl
        )
    }

    private suspend fun processFollowEvent(event: FollowEvent) {
        when (event.action) {
            FollowAction.REQUEST -> FollowNotificationAdapter.publishFollowRequest(
                targetUserId = event.targetUserId,
                actorId = event.actorId,
                actorName = event.actorName,
                actorAvatarUrl = event.actorAvatarUrl
            )
            FollowAction.FOLLOW -> FollowNotificationAdapter.publishFollowedYou(
                targetUserId = event.targetUserId,
                actorId = event.actorId,
                actorName = event.actorName,
                actorAvatarUrl = event.actorAvatarUrl
            )
            FollowAction.ACCEPT -> FollowNotificationAdapter.publishAcceptedFollow(
                targetUserId = event.targetUserId,
                actorId = event.actorId,
                actorName = event.actorName,
                actorAvatarUrl = event.actorAvatarUrl
            )
        }
    }
}
