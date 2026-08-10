package com.example.data.supabase

import com.example.data.model.*
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface SupabaseApiService {
    // --- Privacy & Entitlements ---
    @GET("rest/v1/user_entitlements")
    suspend fun getUserEntitlements(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("user_id") userIdFilter: String
    ): retrofit2.Response<List<com.example.data.model.UserEntitlementDto>>

    @GET("rest/v1/user_privacy_settings")
    suspend fun getUserPrivacySettings(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("user_id") userIdFilter: String
    ): retrofit2.Response<List<com.example.data.model.UserPrivacySettingDto>>

    @POST("rest/v1/user_privacy_settings?on_conflict=user_id,feature_code")
    suspend fun upsertUserPrivacySetting(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body setting: com.example.data.model.UserPrivacySettingDto
    ): retrofit2.Response<Unit>

    // --- Auth Endpoints ---
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Query("redirect_to") redirectTo: String? = "panalink://verify",
        @Body request: SignUpRequest
    ): Response<ResponseBody>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Body request: SignInRequest
    ): Response<AuthResponse>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(
        @Header("apikey") apiKey: String,
        @Body request: RefreshTokenRequest
    ): Response<AuthResponse>

    @GET("auth/v1/user")
    suspend fun getCurrentUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): Response<UserResponse>

    @POST("auth/v1/resend")
    suspend fun resendEmail(
        @Header("apikey") apiKey: String,
        @Body request: ResendRequest
    ): Response<ResponseBody>

    @POST("auth/v1/verify")
    suspend fun verifyOtp(
        @Header("apikey") apiKey: String,
        @Body request: VerifyOtpRequest
    ): Response<AuthResponse>

    // --- Database (PostgREST) Endpoints ---
    
    // Profiles
    @GET("rest/v1/profiles")
    suspend fun getProfiles(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("display_name") displayNameFilter: String? = null,
        @Query("id") idFilter: String? = null
    ): Response<List<Profile>>

    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<Profile>>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Body profile: UpdateProfileRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Profile>>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfileMap(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Body profile: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @POST("rest/v1/profiles")
    suspend fun insertProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body profile: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @POST("rest/v1/profiles")
    suspend fun insertProfileDto(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body profile: InsertProfileRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Profile>>

    @DELETE("rest/v1/profiles")
    suspend fun deleteProfile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    // Chats
    @GET("rest/v1/chats")
    suspend fun getChats(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*"
    ): Response<List<Chat>>

    @GET("rest/v1/chats")
    suspend fun getPublicChannels(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("chat_type") typeFilter: String = "in.(channel,community)",
        @Query("visibility") visibilityFilter: String = "eq.public",
        @Query("is_archived") archivedFilter: String = "eq.false",
        @Query("select") select: String = "*"
    ): Response<List<Chat>>

    @POST("rest/v1/chats")
    suspend fun createChat(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body chat: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<Chat>>

    @PATCH("rest/v1/chats")
    suspend fun updateChat(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): Response<okhttp3.ResponseBody>

    @GET("rest/v1/chats")
    suspend fun getChat(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<Chat>>

    @PATCH("rest/v1/chat_members")
    suspend fun updateChatParticipant(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("chat_id") chatIdFilter: String,
        @Query("user_id") userIdFilter: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @PATCH("rest/v1/thread_messages")
    suspend fun softDeleteThreadMessagesByChat(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("thread_id") threadIdFilter: String,
        @Body update: Map<String, String> = mapOf("status" to "deleted")
    ): Response<ResponseBody>

    @PATCH("rest/v1/messages")
    suspend fun softDeleteLegacyMessagesByChat(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("chat_id") chatIdFilter: String,
        @Body update: Map<String, String> = mapOf("status" to "deleted")
    ): Response<ResponseBody>


    @PATCH("rest/v1/chats")
    suspend fun softDeleteChat(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Body update: Map<String, String> = mapOf("status" to "deleted")
    ): Response<ResponseBody>

    @DELETE("rest/v1/chats")
    suspend fun hardDeleteChat(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    @GET("rest/v1/chat_members")
    suspend fun getChatParticipant(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("chat_id") chatIdFilter: String,
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<ChatMember>>

    @HEAD("rest/v1/chat_members")
    suspend fun getChatParticipantsCount(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("chat_id") chatIdFilter: String,
        @Header("Prefer") prefer: String = "count=exact"
    ): Response<Void>

    // Chat Members
    @GET("rest/v1/chat_members")
    suspend fun getChatMembers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userIdFilter: String? = null,
        @Query("select") select: String = "*"
    ): Response<List<ChatMember>>

    @POST("rest/v1/chat_members")
    suspend fun createChatMember(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body member: ChatMember
    ): Response<ResponseBody>

    @POST("rest/v1/chat_members")
    suspend fun upsertChatMemberMap(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body memberData: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    // One-to-One Threads
    @GET("rest/v1/one_to_one_threads")
    suspend fun getOneToOneThreads(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("or") orFilter: String? = null
    ): Response<List<OneToOneThread>>

    @POST("rest/v1/one_to_one_threads")
    suspend fun createOneToOneThread(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body thread: Map<String, String>
    ): Response<List<OneToOneThread>>

    // Thread Messages
    @GET("rest/v1/thread_messages")
    suspend fun getThreadMessages(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("thread_id") threadIdFilter: String,
        @Query("created_at") createdAtFilter: String? = null,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.asc",
        @Query("limit") limit: Int = 100
    ): Response<List<ThreadMessage>>

    @GET("rest/v1/thread_messages")
    suspend fun getIncrementalThreadMessages(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("thread_id") threadIdFilter: String,
        @Query("updated_at") updatedAtFilter: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<ThreadMessage>>

    @POST("rest/v1/thread_messages")
    suspend fun createThreadMessage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body message: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ResponseBody>

    @PATCH("rest/v1/thread_messages")
    suspend fun updateThreadMessage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String, // e.g., "eq.uuid"
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ResponseBody>

    @DELETE("rest/v1/thread_messages")
    suspend fun deleteThreadMessage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String
    ): Response<ResponseBody>





    // Message Reactions
    @GET("rest/v1/message_reactions")
    suspend fun getMessageReactions(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("thread_message_id") threadMessageIdFilter: String? = null,
        @Query("channel_message_id") channelMessageIdFilter: String? = null
    ): Response<List<MessageReaction>>

    @POST("rest/v1/message_reactions")
    suspend fun upsertMessageReaction(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body reaction: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ResponseBody>

    @DELETE("rest/v1/message_reactions")
    suspend fun deleteMessageReaction(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("thread_message_id") threadMessageIdFilter: String,
        @Query("user_id") userIdFilter: String
    ): Response<ResponseBody>


    // Messages
    @GET("rest/v1/messages")
    suspend fun getMessages(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("chat_id") chatIdFilter: String,
        @Query("created_at") createdAtFilter: String? = null,
        @Query("order") order: String = "created_at.asc",
        @Query("limit") limit: Int = 100
    ): Response<List<Message>>

    @GET("rest/v1/messages")
    suspend fun getIncrementalMessages(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("chat_id") chatIdFilter: String,
        @Query("updated_at") updatedAtFilter: String,
        @Query("order") order: String = "updated_at.asc"
    ): Response<List<Message>>

    @POST("rest/v1/messages")
    suspend fun createMessage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body message: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ResponseBody>

    @GET("rest/v1/user_reels")
    suspend fun getUserReels(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("author_id") authorFilter: String? = null,
        @Header("Accept-Profile") acceptProfile: String = "social"
    ): Response<List<UserState>>

    @GET("rest/v1/user_stories")
    suspend fun getUserStories(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("expires_at") expiresAtFilter: String? = "gt.now()",
        @Query("author_id") authorFilter: String? = null,
        @Header("Accept-Profile") acceptProfile: String = "social"
    ): Response<List<UserState>>

    @POST("rest/v1/user_reels")
    suspend fun createReel(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body state: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Accept-Profile") acceptProfile: String = "social"
    ): Response<List<UserState>>

    @POST("rest/v1/user_stories")
    suspend fun createStory(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body state: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Accept-Profile") acceptProfile: String = "social"
    ): Response<List<UserState>>

    @GET("rest/v1/contacts")
    suspend fun getContacts(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("owner_user_id") ownerFilter: String? = null
    ): Response<List<ContactEntity>>

    @DELETE("rest/v1/contacts")
    suspend fun deleteContact(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("contact_user_id") contactUserId: String
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/accept_friend_request")
    suspend fun acceptFriendRequest(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/reject_friend_request")
    suspend fun declineFriendRequest(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/send_friend_request")
    suspend fun sendFriendRequest(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @GET("rest/v1/friend_requests")
    suspend fun getFriendRequests(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*,sender:profiles!sender_id(*)",
        @Query("receiver_id") receiverFilter: String,
        @Query("status") statusFilter: String = "eq.pending"
    ): Response<List<FriendRequestEntity>>

    @GET("rest/v1/contacts")
    suspend fun getContactsWithProfiles(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*,profiles!contact_user_id(id,display_name,avatar_url,is_profile_complete,created_at,profile_theme,profile_badges,last_profile_edit,device_fingerprint)",
        @Query("owner_user_id") ownerFilter: String
    ): Response<List<ContactWithProfileEntity>>

    @GET("rest/v1/user_followers")
    suspend fun getFollowers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("follower_id") followerIdFilter: String? = null,
        @Query("followed_id") followedIdFilter: String? = null,
        @Header("Accept-Profile") acceptProfile: String = "social"
    ): Response<List<FollowerDto>>

    @POST("rest/v1/user_followers")
    suspend fun followUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @DELETE("rest/v1/user_followers")
    suspend fun unfollowUser(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("follower_id") followerIdFilter: String,
        @Query("followed_id") followedIdFilter: String,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @POST("rest/v1/user_reels")
    suspend fun createReel(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body state: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @POST("rest/v1/user_stories")
    suspend fun createStory(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body state: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/clear_chat")
    suspend fun clearChatRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/hide_chat")
    suspend fun hideChatRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>
    ): Response<ResponseBody>

    // RPCs
    @POST("rest/v1/rpc/tm_get_or_create_thread")
    suspend fun getOrCreateThread(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/get_my_contact_identifier")
    suspend fun getMyContactIdentifier(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String> = emptyMap()
    ): Response<com.example.data.model.ContactIdentifierResponse>

    @POST("rest/v1/rpc/add_contact_by_identifier")
    suspend fun addContactByPin(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/send_friend_request_by_pin")
    suspend fun sendFriendRequestByPin(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/send_friend_request_by_qr")
    suspend fun sendFriendRequestByQr(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/mark_thread_delivered")
    suspend fun markThreadDelivered(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/mark_thread_read")
    suspend fun markThreadRead(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/get_active_media_statuses")
    suspend fun getActiveMediaStatuses(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, Int>
    ): Response<List<UserState>>

    @POST("rest/v1/rpc/toggle_like")
    suspend fun toggleLikeRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, @JvmSuppressWildcards Any>,
        @Header("Accept-Profile") acceptProfile: String = "social",
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ToggleLikeResponseDto>

    @POST("rest/v1/rpc/toggle_favorite")
    suspend fun toggleFavoriteRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, @JvmSuppressWildcards Any>,
        @Header("Accept-Profile") acceptProfile: String = "social",
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ToggleFavoriteResponseDto>

    @POST("rest/v1/rpc/toggle_story_like")
    suspend fun toggleStoryLikeRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>,
        @Header("Accept-Profile") acceptProfile: String = "social",
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ToggleLikeResponseDto>

    @POST("rest/v1/rpc/toggle_story_favorite")
    suspend fun toggleStoryFavoriteRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>,
        @Header("Accept-Profile") acceptProfile: String = "social",
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ToggleFavoriteResponseDto>

    @POST("rest/v1/rpc/toggle_reel_like")
    suspend fun toggleReelLikeRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>,
        @Header("Accept-Profile") acceptProfile: String = "social",
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ToggleLikeResponseDto>

    @POST("rest/v1/rpc/toggle_reel_favorite")
    suspend fun toggleReelFavoriteRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>,
        @Header("Accept-Profile") acceptProfile: String = "social",
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ToggleFavoriteResponseDto>

    // State interactions (likes, favorites, shares, comments)
    @GET("rest/v1/{table}")
    suspend fun getStateLikes(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @QueryMap filters: Map<String, String>,
        @Query("select") select: String = "*",
        @Header("Accept-Profile") acceptProfile: String = "social"
    ): Response<List<LikeDto>>

    @GET("rest/v1/{table}")
    suspend fun getUserLikes(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @QueryMap filters: Map<String, String>,
        @Query("select") select: String = "*",
        @Header("Accept-Profile") acceptProfile: String = "social"
    ): Response<List<LikeDto>>

    @GET("rest/v1/{table}")
    suspend fun getStateFavorites(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @QueryMap filters: Map<String, String>,
        @Query("select") select: String = "*",
        @Header("Accept-Profile") acceptProfile: String = "social"
    ): Response<List<LikeDto>>

    @GET("rest/v1/{table}")
    suspend fun getUserFavorites(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @QueryMap filters: Map<String, String>,
        @Query("select") select: String = "*",
        @Header("Accept-Profile") acceptProfile: String = "social"
    ): Response<List<LikeDto>>

    @GET("rest/v1/{table}")
    suspend fun getStateComments(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @QueryMap filters: Map<String, String>,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
        @Header("Accept-Profile") acceptProfile: String = "social"
    ): Response<List<StateCommentDto>>

    @GET("rest/v1/{table}")
    suspend fun getStatusViews(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @QueryMap filters: Map<String, String>,
        @Query("select") select: String = "*",
        @Header("Accept-Profile") acceptProfile: String = "social"
    ): Response<List<StatusViewDto>>

    @DELETE("rest/v1/user_reels")
    suspend fun deleteReel(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @DELETE("rest/v1/user_stories")
    suspend fun deleteStory(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @DELETE("rest/v1/{table}")
    suspend fun deleteComment(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @PATCH("rest/v1/{table}")
    suspend fun patchComment(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any?>,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @POST("rest/v1/{table}")
    suspend fun likeState(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @DELETE("rest/v1/{table}")
    suspend fun unlikeState(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @QueryMap filters: Map<String, String>,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @POST("rest/v1/{table}")
    suspend fun favoriteState(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @DELETE("rest/v1/{table}")
    suspend fun unfavoriteState(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @QueryMap filters: Map<String, String>,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @POST("rest/v1/{table}")
    suspend fun shareState(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @POST("rest/v1/{table}")
    suspend fun commentState(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    @POST("rest/v1/{table}")
    suspend fun viewStatus(
        @Path("table") table: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>,
        @Header("Content-Profile") contentProfile: String = "social"
    ): Response<ResponseBody>

    // --- Favorites ---
    @GET("rest/v1/user_favorites")
    suspend fun getThreadMessageFavorites(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userId: String,
        @Query("select") select: String = "*,message:thread_messages(*)"
    ): Response<List<Map<String, Any>>>

    @POST("rest/v1/user_favorites")
    suspend fun addFavoriteMessage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @DELETE("rest/v1/user_favorites")
    suspend fun removeFavoriteMessage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userIdFilter: String,
        @Query("message_id") messageIdFilter: String
    ): Response<ResponseBody>

    // --- Storage (Upload files directly as raw binary) ---
    @POST("storage/v1/object/{bucket}/{path}")
    suspend fun uploadFile(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Path("bucket") bucket: String,
        @Path("path") path: String,
        @Body file: RequestBody,
        @Header("Content-Type") contentType: String
    ): Response<ResponseBody>

    // --- Edge Functions ---
    @GET("rest/v1/notifications")
    suspend fun getNotifications(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): retrofit2.Response<List<com.example.data.model.NotificationDto>>

    @PATCH("rest/v1/notifications")
    suspend fun markNotificationRead(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Body body: Map<String, Boolean>
    ): retrofit2.Response<Unit>

    @POST("rest/v1/notifications")
    suspend fun createNotification(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>
    ): retrofit2.Response<ResponseBody>
    
    @DELETE("rest/v1/notifications")
    suspend fun clearNotification(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String
    ): retrofit2.Response<ResponseBody>

    @DELETE("rest/v1/notifications")
    suspend fun clearAllNotifications(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userIdFilter: String
    ): retrofit2.Response<ResponseBody>

    @POST
    suspend fun callEdgeFunction(
        @Url url: String,
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @POST("functions/v1/search-stickers")
    suspend fun getGiphyStickers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body request: SearchStickersRequest
    ): Response<ResponseBody>

    // --- E2EE User Keys ---
    @GET("rest/v1/user_keys")
    suspend fun getUserKeys(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<UserKeyDto>>

    @POST("rest/v1/user_keys")
    suspend fun upsertUserKey(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body keyData: Map<String, String>
    ): Response<ResponseBody>

    @POST("rest/v1/debug_logs")
    suspend fun createDebugLog(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "return=representation",
        @Body logMap: Map<String, @JvmSuppressWildcards Any?>
    ): Response<ResponseBody>
    @GET("rest/v1/posts")
    suspend fun getFeedPosts(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("privacy") privacyFilter: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("order") order: String = "created_at.desc,id.desc",
        @Query("created_at") createdAtLt: String? = null
    ): Response<List<com.example.data.model.PostDto>>

    @GET("rest/v1/posts")
    suspend fun getPostById(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<com.example.data.model.PostDto>>

    @POST("rest/v1/posts")
    @Headers("Prefer: return=representation")
    suspend fun createPost(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body post: com.example.data.model.PostDto
    ): Response<List<com.example.data.model.PostDto>>

    @POST("rest/v1/post_likes")
    suspend fun addLike(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body like: com.example.data.model.PostLikeDto
    ): Response<ResponseBody>

    @DELETE("rest/v1/post_likes")
    suspend fun removeLike(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("post_id") postIdFilter: String,
        @Query("user_id") userIdFilter: String
    ): Response<ResponseBody>

    @POST("rest/v1/post_comments")
    @Headers("Prefer: return=representation")
    suspend fun addComment(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body comment: com.example.data.model.PostCommentDto
    ): Response<List<com.example.data.model.PostCommentDto>>

    @GET("rest/v1/post_comments?select=*,profiles:user_id(id,display_name,avatar_url)")
    suspend fun getCommentsForPost(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("post_id") postIdFilter: String,
        @Query("order") order: String = "created_at.asc"
    ): Response<List<com.example.data.model.PostCommentDto>>
    
    @GET("rest/v1/post_likes?select=post_id")
    suspend fun getUserLikes(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userIdFilter: String
    ): Response<List<com.example.data.model.PostLikeDto>>

    @DELETE("rest/v1/posts")
    suspend fun deletePost(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String
    ): Response<ResponseBody>

    @PATCH("rest/v1/posts")
    @Headers("Prefer: return=representation")
    suspend fun updatePost(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any?>
    ): Response<List<com.example.data.model.PostDto>>

    // --- Stickers ---
    @POST("rest/v1/rpc/save_sticker")
    suspend fun saveSticker(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/favorite_sticker")
    suspend fun favoriteSticker(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/unfavorite_sticker")
    suspend fun unfavoriteSticker(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/register_sticker_usage")
    suspend fun registerStickerUsage(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/get_saved_stickers")
    suspend fun getSavedStickers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): Response<List<Map<String, Any>>>

    @POST("rest/v1/rpc/get_favorite_stickers")
    suspend fun getFavoriteStickers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): Response<List<Map<String, Any>>>

    @POST("rest/v1/rpc/get_recent_stickers")
    suspend fun getRecentStickers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/blocked_users")
    suspend fun getBlockedUsers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<com.example.data.model.BlockedUser>>

    @POST("rest/v1/blocked_users")
    suspend fun blockUserApi(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>
    ): Response<ResponseBody>

    @DELETE("rest/v1/blocked_users")
    suspend fun unblockUserApi(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userIdFilter: String,
        @Query("blocked_user_id") blockedUserIdFilter: String
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/delete_message_for_me")
    suspend fun deleteMessageForMeRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>
    ): Response<ResponseBody>

    @GET("rest/v1/user_deleted_messages")
    suspend fun getUserDeletedMessages(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userIdFilter: String,
        @Query("select") select: String = "message_id"
    ): Response<List<Map<String, Any>>>

    @POST("rest/v1/rpc/update_chat_mute_status")
    suspend fun updateChatMuteStatusRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @POST("rest/v1/rpc/update_chat_pin_status")
    suspend fun updateChatPinStatusRpc(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @POST("rest/v1/user_presence?on_conflict=user_id")
    suspend fun upsertUserPresence(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body presence: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @GET("rest/v1/user_presence")
    suspend fun getUserPresence(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userIdFilter: String? = null
    ): Response<List<Map<String, Any>>>

    @GET("rest/v1/presence_sessions")
    suspend fun getPresenceSessions(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("user_id") userIdFilter: String,
        @Query("order") order: String = "last_active_at.desc"
    ): Response<List<com.example.data.model.PresenceSession>>

    // --- Music Social Sync ---

    @GET("rest/v1/music_playlists")
    suspend fun getMusicPlaylists(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("owner_id") ownerId: String? = null,
        @Query("updated_at") updatedAtGt: String? = null
    ): Response<List<com.example.data.model.RemoteMusicPlaylist>>

    @POST("rest/v1/music_playlists")
    @Headers("Prefer: return=representation")
    suspend fun upsertMusicPlaylist(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body playlist: com.example.data.model.RemoteMusicPlaylist
    ): Response<List<com.example.data.model.RemoteMusicPlaylist>>

    @GET("rest/v1/music_playlist_tracks")
    suspend fun getMusicPlaylistTracks(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("playlist_id") playlistId: String,
        @Query("select") select: String = "*"
    ): Response<List<com.example.data.model.RemoteMusicPlaylistTrack>>

    @POST("rest/v1/music_playlist_tracks")
    suspend fun upsertMusicPlaylistTracks(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates",
        @Body tracks: List<com.example.data.model.RemoteMusicPlaylistTrack>
    ): Response<Unit>

    @GET("rest/v1/music_playlist_collaborators")
    suspend fun getMusicPlaylistCollaborators(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("playlist_id") playlistId: String
    ): Response<List<com.example.data.model.RemoteMusicPlaylistCollaborator>>

    @GET("rest/v1/music_playlist_shares")
    suspend fun getMusicPlaylistShares(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("shared_to_user_id") userId: String
    ): Response<List<com.example.data.model.RemoteMusicPlaylistShare>>

    // --- P6.7.9 Music Playlist Invitations ---

    @GET("rest/v1/music_playlist_invitations")
    suspend fun getMusicPlaylistInvitations(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("select") select: String = "*",
        @Query("receiver_id") receiverId: String? = null,
        @Query("playlist_id") playlistId: String? = null,
        @Query("status") status: String? = null
    ): Response<List<com.example.data.model.RemoteMusicPlaylistInvitation>>

    @POST("rest/v1/music_playlist_invitations")
    @Headers("Prefer: return=representation")
    suspend fun createMusicPlaylistInvitation(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body invitation: com.example.data.model.RemoteMusicPlaylistInvitation
    ): Response<List<com.example.data.model.RemoteMusicPlaylistInvitation>>

    @PATCH("rest/v1/music_playlist_invitations")
    suspend fun updateMusicPlaylistInvitation(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Query("id") idFilter: String,
        @Body updates: Map<String, String>
    ): Response<Unit>

    @POST("rest/v1/rpc/accept_music_playlist_invitation")
    suspend fun acceptMusicPlaylistInvitation(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>
    ): Response<Unit>

    @POST("rest/v1/rpc/reject_music_playlist_invitation")
    suspend fun rejectMusicPlaylistInvitation(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>
    ): Response<Unit>

    @POST("rest/v1/rpc/revoke_music_playlist_invitation")
    suspend fun revokeMusicPlaylistInvitation(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authorization: String,
        @Body params: Map<String, String>
    ): Response<Unit>
}