package com.example

import com.example.data.model.UpdateProfileRequest
import com.example.data.supabase.SupabaseClient
import org.junit.Test

class MoshiSerializationTest {
    @Test
    fun testMoshi() {
        val req = UpdateProfileRequest(
            displayName = "Pana",
            avatarUrl = null,
            isProfileComplete = true,
            firstName = "John",
            lastName = "Doe",
            status = "Active",
            birthDate = null,
            sex = "Male",
            interests = listOf("Coding"),
            coverUrl = null
        )
        val adapter = SupabaseClient.moshi.adapter(UpdateProfileRequest::class.java)
        println("SERIALIZED_JSON: " + adapter.toJson(req))
    }
}
