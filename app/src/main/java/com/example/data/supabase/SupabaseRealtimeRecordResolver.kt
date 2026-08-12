package com.example.data.supabase

import org.json.JSONObject

/**
 * Normalizes Supabase Postgres Changes payloads.
 *
 * DELETE payloads expose the previous row under `old_record` while INSERT/UPDATE
 * payloads expose the current row under `record`. Keeping this logic centralized
 * prevents composite-primary-key interactions from being discarded by clients.
 */
object SupabaseRealtimeRecordResolver {
    data class ResolvedRecord(
        val record: JSONObject,
        val recordId: String,
        val statusId: String
    )

    fun resolve(
        payload: JSONObject?,
        data: JSONObject?,
        eventType: String,
        isReel: Boolean
    ): ResolvedRecord? {
        val record = when {
            eventType == "DELETE" ->
                data?.optJSONObject("old_record")
                    ?: payload?.optJSONObject("old_record")
                    ?: data?.optJSONObject("record")
                    ?: payload?.optJSONObject("record")
            else ->
                data?.optJSONObject("record")
                    ?: payload?.optJSONObject("record")
                    ?: data
        } ?: return null

        val statusId = if (isReel) {
            record.optString("reel_id").ifBlank {
                record.optString("status_id")
            }
        } else {
            record.optString("story_id").ifBlank {
                record.optString("status_id")
            }
        }

        if (statusId.isBlank()) return null

        // Prefer a stable database transport ID. For legacy rows without an id,
        // derive the same key from immutable row fields so redelivered Realtime
        // frames are idempotent. created_at distinguishes repeated shares/comments
        // by the same user on the same status while remaining stable on replay.
        val recordId = record.optString("id").ifBlank {
            val userId = record.optString("user_id").ifBlank {
                record.optString("author_id")
            }
            val createdAt = record.optString("created_at")
            val content = record.optString("body")
            val interactionKey = if (isReel) "reel" else "story"
            listOf(interactionKey, statusId, userId, createdAt, content)
                .joinToString(":")
        }

        return ResolvedRecord(record, recordId, statusId)
    }
}
