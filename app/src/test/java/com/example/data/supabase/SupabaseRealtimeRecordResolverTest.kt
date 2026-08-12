package com.example.data.supabase

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SupabaseRealtimeRecordResolverTest {
    @Test
    fun deleteWithCompositePrimaryKeyUsesOldRecordAndStableId() {
        val oldRecord = JSONObject().apply {
            put("reel_id", "reel-42")
            put("user_id", "user-7")
            put("id", "interaction-uuid")
        }
        val data = JSONObject().apply {
            put("old_record", oldRecord)
        }

        val resolved = SupabaseRealtimeRecordResolver.resolve(
            payload = JSONObject(),
            data = data,
            eventType = "DELETE",
            isReel = true
        )

        assertNotNull(resolved)
        assertEquals("reel-42", resolved!!.statusId)
        assertEquals("interaction-uuid", resolved.recordId)
    }

    @Test
    fun deleteWithoutTransportIdUsesStableRowFields() {
        val oldRecord = JSONObject().apply {
            put("story_id", "story-9")
            put("user_id", "user-3")
            put("created_at", "2026-08-12T05:00:00Z")
        }
        val data = JSONObject().apply {
            put("old_record", oldRecord)
        }

        val resolved = SupabaseRealtimeRecordResolver.resolve(
            payload = JSONObject(),
            data = data,
            eventType = "DELETE",
            isReel = false
        )

        assertNotNull(resolved)
        assertEquals("story-9", resolved!!.statusId)
        assertEquals("story:story-9:user-3:2026-08-12T05:00:00Z:", resolved.recordId)
    }

    @Test
    fun replayedShareWithoutTransportIdGetsSameId() {
        val record = JSONObject().apply {
            put("reel_id", "reel-1")
            put("user_id", "user-2")
            put("created_at", "2026-08-12T05:10:00Z")
        }
        val data = JSONObject().apply { put("record", record) }

        val first = SupabaseRealtimeRecordResolver.resolve(
            payload = JSONObject(),
            data = data,
            eventType = "INSERT",
            isReel = true
        )
        val replay = SupabaseRealtimeRecordResolver.resolve(
            payload = JSONObject(),
            data = data,
            eventType = "INSERT",
            isReel = true
        )

        assertNotNull(first)
        assertNotNull(replay)
        assertEquals(first!!.recordId, replay!!.recordId)
    }
}
