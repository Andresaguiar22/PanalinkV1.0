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

        // Stable UUID transport IDs are now present on likes/favorites. For older
        // rows or comments/shares, fall back to a deterministic composite key.
        val recordId = record.optString("id").ifBlank {
            val userId = record.optString("user_id")
            val interactionKey = if (isReel) "reel" else "story"
            "$interactionKey:$statusId:$userId"
        }

        return ResolvedRecord(record, recordId, statusId)
    }
}
