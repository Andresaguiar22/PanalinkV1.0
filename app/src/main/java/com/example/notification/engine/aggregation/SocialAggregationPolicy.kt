package com.example.notification.engine.aggregation

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationTypeV2

@Keep
object SocialAggregationPolicy {

    val defaultRules = mapOf(
        NotificationTypeV2.POST_LIKE to AggregationRule(
            type = NotificationTypeV2.POST_LIKE,
            windowType = AggregationWindowType.SLIDING_ONE_HOUR,
            minCountToAggregate = 2,
            maxActorsInTitle = 3
        ),
        NotificationTypeV2.POST_REACTION to AggregationRule(
            type = NotificationTypeV2.POST_REACTION,
            windowType = AggregationWindowType.SLIDING_ONE_HOUR,
            minCountToAggregate = 2,
            maxActorsInTitle = 3
        ),
        NotificationTypeV2.POST_COMMENT to AggregationRule(
            type = NotificationTypeV2.POST_COMMENT,
            windowType = AggregationWindowType.SLIDING_ONE_MINUTE,
            minCountToAggregate = 2,
            maxActorsInTitle = 2
        ),
        NotificationTypeV2.USER_FOLLOWED_YOU to AggregationRule(
            type = NotificationTypeV2.USER_FOLLOWED_YOU,
            windowType = AggregationWindowType.DAILY,
            minCountToAggregate = 3,
            maxActorsInTitle = 2
        )
    )

    fun formatSocialText(
        type: NotificationTypeV2,
        actors: List<String>,
        totalCount: Int
    ): Pair<String, String> {
        val distinctActors = actors.distinct().filter { it.isNotBlank() }
        val count = maxOf(distinctActors.size, totalCount)

        return when (type) {
            NotificationTypeV2.POST_LIKE, NotificationTypeV2.POST_REACTION -> {
                val title = "Reacciones en el Muro"
                val body = when {
                    count <= 1 -> "${distinctActors.firstOrNull() ?: "Alguien"} dio Me gusta a tu publicación"
                    distinctActors.size == 2 && count == 2 -> "${distinctActors[0]} y ${distinctActors[1]} dieron Me gusta a tu publicación"
                    distinctActors.size >= 3 -> {
                        val first3 = distinctActors.take(3).joinToString(", ")
                        val remaining = count - 3
                        if (remaining > 0) "A $first3 y $remaining personas más les gustó tu publicación"
                        else "A $first3 les gustó tu publicación"
                    }
                    else -> "A $count personas les gustó tu publicación"
                }
                title to body
            }
            NotificationTypeV2.POST_COMMENT, NotificationTypeV2.POST_REPLY, NotificationTypeV2.POST_REPLY_COMMENT -> {
                val title = "Comentarios"
                val body = if (count <= 1) {
                    "${distinctActors.firstOrNull() ?: "Alguien"} comentó tu publicación"
                } else {
                    "${distinctActors.take(2).joinToString(" y ")} comentaron tu publicación"
                }
                title to body
            }
            NotificationTypeV2.USER_FOLLOWED_YOU -> {
                val title = "Nuevos Seguidores"
                val body = if (count <= 1) {
                    "${distinctActors.firstOrNull() ?: "Alguien"} comenzó a seguirte"
                } else {
                    "$count personas comenzaron a seguirte"
                }
                title to body
            }
            else -> {
                val title = "PanaLink Social"
                val body = if (count <= 1) "${distinctActors.firstOrNull() ?: "Alguien"} interactuó con tu contenido"
                else "$count interacciones en tu contenido"
                title to body
            }
        }
    }
}
