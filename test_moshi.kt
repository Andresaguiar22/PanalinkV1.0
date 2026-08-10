import com.example.data.model.*
import com.squareup.moshi.*
import com.example.data.supabase.*
fun main() {
    val moshi = SupabaseClient.moshi
    val adapter = moshi.adapter(ContactWithProfileEntity::class.java)
    val json = """
    {
      "id": "1",
      "owner_user_id": "a",
      "contact_user_id": "b",
      "profiles": {
        "id": "b",
        "display_name": "Test",
        "avatar_url": null,
        "is_profile_complete": true
      }
    }
    """
    val entity = adapter.fromJson(json)
    println("Entity: " + entity)
    val profile = entity?.getProfile(moshi)
    println("Profile: " + profile)
}
