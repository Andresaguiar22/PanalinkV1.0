package com.example
import com.example.data.model.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Test
class PureMoshiTest {
    @Test
    fun testMoshi() {
        val moshi = Moshi.Builder()
            .add(ProfileSurrogateAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(ContactWithProfileEntity::class.java)
        val json = """
        {
          "id": "1",
          "owner_user_id": "a",
          "contact_user_id": "b",
          "created_at": "2023-01-01",
          "profiles": {
            "id": "b",
            "display_name": "Test",
            "avatar_url": null,
            "is_profile_complete": true
          }
        }
        """
        val entity = adapter.fromJson(json)
        println("SERIALIZED_ENTITY: " + entity)
        val profile = entity?.getProfile(moshi)
        println("PROFILE: " + profile)
    }
}
