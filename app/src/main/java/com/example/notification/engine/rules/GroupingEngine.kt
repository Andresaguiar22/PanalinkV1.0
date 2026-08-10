package com.example.notification.engine.rules

import androidx.annotation.Keep
import com.example.notification.engine.model.NotificationEvent
import com.example.notification.engine.model.NotificationTypeV2

@Keep
class GroupingEngine {

    private val groupStore = mutableMapOf<String, MutableList<NotificationEvent>>()
    private val lock = Any()

    data class GroupingAnalysis(
        val groupingKey: String,
        val totalCount: Int,
        val isGrouped: Boolean,
        val summaryText: String
    )

    fun analyze(event: NotificationEvent): GroupingAnalysis {
        val groupKey = event.effectiveGroupingKey()

        synchronized(lock) {
            val list = groupStore.getOrPut(groupKey) { mutableListOf() }
            list.add(event)
            val count = list.size

            val actorName = event.actor?.name ?: "Alguien"
            val isGrouped = count > 1

            val summaryText = when (event.type) {
                NotificationTypeV2.CHAT_MESSAGE,
                NotificationTypeV2.CHAT_REPLY -> {
                    if (count == 1) "$actorName envió un mensaje"
                    else "$actorName te envió $count mensajes nuevos"
                }

                NotificationTypeV2.POST_LIKE,
                NotificationTypeV2.REEL_LIKE -> {
                    if (count == 1) "$actorName reaccionó a tu publicación"
                    else "$actorName y ${count - 1} personas más reaccionaron"
                }

                NotificationTypeV2.POST_COMMENT,
                NotificationTypeV2.REEL_COMMENT -> {
                    if (count == 1) "$actorName comentó tu publicación"
                    else "$actorName y ${count - 1} personas más comentaron"
                }

                else -> {
                    if (count == 1) event.title
                    else "$actorName y ${count - 1} interacciones más"
                }
            }

            return GroupingAnalysis(
                groupingKey = groupKey,
                totalCount = count,
                isGrouped = isGrouped,
                summaryText = summaryText
            )
        }
    }

    fun clearGroup(groupingKey: String) {
        synchronized(lock) {
            groupStore.remove(groupingKey)
        }
    }

    fun clearAll() {
        synchronized(lock) {
            groupStore.clear()
        }
    }
}
