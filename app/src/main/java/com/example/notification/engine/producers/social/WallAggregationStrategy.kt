package com.example.notification.engine.producers.social

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationTypeV2

@Keep
object WallAggregationStrategy {

    fun aggregateLikesAndReactions(actors: List<String>, totalCount: Int, targetTitle: String? = null): String {
        val distinctActors = actors.distinct().filter { it.isNotBlank() }
        val count = if (totalCount < distinctActors.size) distinctActors.size else totalCount
        return when {
            count <= 1 -> "${distinctActors.firstOrNull() ?: "Alguien"} dio Me gusta a tu publicación"
            distinctActors.size == 1 && count == 1 -> "${distinctActors[0]} dio Me gusta a tu publicación"
            distinctActors.size == 2 && count == 2 -> "${distinctActors[0]} y ${distinctActors[1]} dieron Me gusta a tu publicación"
            distinctActors.size >= 3 -> {
                val leading = distinctActors.take(3).joinToString(", ")
                val remaining = count - 3
                if (remaining > 0) {
                    "A $leading y $remaining personas más les gusta tu publicación"
                } else {
                    "A $leading les gusta tu publicación"
                }
            }
            distinctActors.size == 2 -> {
                val leading = distinctActors.joinToString(", ")
                val remaining = count - 2
                "A $leading y $remaining personas más les gusta tu publicación"
            }
            distinctActors.size == 1 -> {
                val leading = distinctActors[0]
                val remaining = count - 1
                "A $leading y $remaining personas más les gusta tu publicación"
            }
            else -> "A $count personas les gusta tu publicación"
        }
    }

    fun aggregateComments(totalCount: Int, lastActorName: String? = null): String {
        return if (totalCount <= 1) {
            "${lastActorName ?: "Alguien"} comentó tu publicación"
        } else {
            "$totalCount nuevos comentarios en tu publicación"
        }
    }

    fun aggregateShares(totalCount: Int, lastActorName: String? = null): String {
        return if (totalCount <= 1) {
            "${lastActorName ?: "Alguien"} compartió tu publicación"
        } else {
            "$totalCount personas compartieron tu publicación"
        }
    }

    fun formatNotificationSummary(
        type: NotificationTypeV2,
        actors: List<String>,
        totalCount: Int,
        targetPreview: String? = null
    ): String {
        return when (type) {
            NotificationTypeV2.POST_LIKE, NotificationTypeV2.POST_REACTION -> aggregateLikesAndReactions(actors, totalCount, targetPreview)
            NotificationTypeV2.POST_COMMENT, NotificationTypeV2.POST_REPLY, NotificationTypeV2.POST_REPLY_COMMENT -> aggregateComments(totalCount, actors.firstOrNull())
            NotificationTypeV2.POST_SHARED, NotificationTypeV2.POST_SHARE, NotificationTypeV2.POST_REPOSTED -> aggregateShares(totalCount, actors.firstOrNull())
            else -> if (totalCount > 1) "$totalCount interacciones en tu publicación" else "${actors.firstOrNull() ?: "Alguien"} interactuó con tu publicación"
        }
    }
}
