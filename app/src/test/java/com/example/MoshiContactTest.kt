package com.example
import com.example.data.model.*
import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Test
class MoshiContactTest {
    @Test
    fun testMoshi() {
        val moshi = Moshi.Builder()
            .add(ProfileSurrogateAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(ContactEntity::class.java)
        val json = """
        {
          "id": "1",
          "owner_user_id": "a",
          "contact_user_id": "b",
          "created_at": "2023-01-01"
        }
        """
        val entity = adapter.fromJson(json)
        println("SERIALIZED_CONTACT: " + entity)
    }
}
