package com.example.rooms.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * REST (PostgREST) del modulo Salas de Voz.
 * Interfaz Retrofit propia: no se toca SupabaseApiService del chat.
 */

@JsonClass(generateAdapter = true)
data class VoiceRoomDto(
    val id: String,
    val name: String,
    @Json(name = "owner_id") val ownerId: String,
    val status: String,
    @Json(name = "max_seats") val maxSeats: Int = 7,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class VoiceRoomMemberDto(
    val id: String,
    @Json(name = "room_id") val roomId: String,
    @Json(name = "user_id") val userId: String,
    val role: String,
    @Json(name = "joined_at") val joinedAt: String,
    @Json(name = "left_at") val leftAt: String? = null
)

@JsonClass(generateAdapter = true)
data class VoiceRoomSeatDto(
    val id: String,
    @Json(name = "room_id") val roomId: String,
    @Json(name = "seat_index") val seatIndex: Int,
    @Json(name = "user_id") val userId: String,
    @Json(name = "is_muted") val isMuted: Boolean = false,
    @Json(name = "joined_at") val joinedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class VoiceRoomMessageDto(
    val id: String,
    @Json(name = "room_id") val roomId: String,
    @Json(name = "sender_id") val senderId: String,
    val content: String,
    @Json(name = "created_at") val createdAt: String
)

interface VoiceRoomApi {

    // --- Salas ---
    @GET("rest/v1/voice_rooms")
    suspend fun listLiveRooms(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("status") status: String = "eq.live",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 50
    ): Response<List<VoiceRoomDto>>

    @POST("rest/v1/voice_rooms")
    @Headers("Prefer: return=representation")
    suspend fun createRoom(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String>
    ): Response<List<VoiceRoomDto>>

    // --- Miembros ---
    @GET("rest/v1/voice_room_members")
    suspend fun getMembers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("room_id") roomId: String,
        @Query("left_at") leftAt: String = "is.null"
    ): Response<List<VoiceRoomMemberDto>>

    @POST("rest/v1/voice_room_members")
    @Headers("Prefer: return=representation")
    suspend fun joinRoom(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String>
    ): Response<List<VoiceRoomMemberDto>>

    @PATCH("rest/v1/voice_room_members")
    suspend fun leaveRoom(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("room_id") roomId: String,
        @Query("user_id") userId: String,
        @Query("left_at") leftAt: String = "is.null",
        @Body body: Map<String, String>
    ): Response<Unit>

    // --- Sillones ---
    @GET("rest/v1/voice_room_seats")
    suspend fun getSeats(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("room_id") roomId: String,
        @Query("order") order: String = "seat_index.asc"
    ): Response<List<VoiceRoomSeatDto>>

    @POST("rest/v1/voice_room_seats")
    @Headers("Prefer: return=representation")
    suspend fun takeSeat(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body body: Map<String, Any>
    ): Response<List<VoiceRoomSeatDto>>

    @PATCH("rest/v1/voice_room_seats")
    suspend fun updateSeat(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("room_id") roomId: String,
        @Query("user_id") userId: String,
        @Body body: Map<String, Any>
    ): Response<Unit>

    @DELETE("rest/v1/voice_room_seats")
    suspend fun leaveSeat(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("room_id") roomId: String,
        @Query("user_id") userId: String
    ): Response<Unit>

    // --- Chat de la sala ---
    @GET("rest/v1/voice_room_messages")
    suspend fun getMessages(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("room_id") roomId: String,
        @Query("order") order: String = "created_at.asc",
        @Query("limit") limit: Int = 100
    ): Response<List<VoiceRoomMessageDto>>

    @POST("rest/v1/voice_room_messages")
    @Headers("Prefer: return=representation")
    suspend fun sendMessage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body body: Map<String, String>
    ): Response<List<VoiceRoomMessageDto>>

    companion object {
        fun create(baseUrl: String, moshi: com.squareup.moshi.Moshi, client: okhttp3.OkHttpClient): VoiceRoomApi {
            val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            return Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(VoiceRoomApi::class.java)
        }
    }
}
