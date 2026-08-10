package com.example.media.sync

import com.example.data.model.*
import com.example.data.supabase.SupabaseApiService
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response

class FakeSupabaseApi : SupabaseApiService {
    var playlists = mutableListOf<RemoteMusicPlaylist>()
    var tracks = mutableMapOf<String, MutableList<RemoteMusicPlaylistTrack>>()
    
    var upsertedPlaylists = mutableListOf<RemoteMusicPlaylist>()
    var upsertedTracks = mutableListOf<RemoteMusicPlaylistTrack>()

    override suspend fun getUserEntitlements(apiKey: String, authHeader: String, userIdFilter: String): Response<List<UserEntitlementDto>> = TODO()
    override suspend fun getUserPrivacySettings(apiKey: String, authHeader: String, userIdFilter: String): Response<List<UserPrivacySettingDto>> = TODO()
    override suspend fun upsertUserPrivacySetting(apiKey: String, authHeader: String, prefer: String, setting: UserPrivacySettingDto): Response<Unit> = TODO()
    override suspend fun signUp(apiKey: String, redirectTo: String?, request: SignUpRequest): Response<ResponseBody> = TODO()
    override suspend fun signIn(apiKey: String, request: SignInRequest): Response<AuthResponse> = TODO()
    override suspend fun refreshToken(apiKey: String, request: RefreshTokenRequest): Response<AuthResponse> = TODO()
    override suspend fun getCurrentUser(apiKey: String, authorization: String): Response<UserResponse> = TODO()
    override suspend fun resendEmail(apiKey: String, request: ResendRequest): Response<ResponseBody> = TODO()
    override suspend fun verifyOtp(apiKey: String, request: VerifyOtpRequest): Response<AuthResponse> = TODO()
    override suspend fun getProfiles(apiKey: String, authorization: String, select: String, displayNameFilter: String?, idFilter: String?): Response<List<Profile>> = TODO()
    override suspend fun getPublicProfiles(apiKey: String, authorization: String, idFilter: String?, select: String): Response<List<PublicProfileDto>> = TODO()
    override suspend fun getProfile(apiKey: String, authorization: String, idFilter: String, select: String): Response<List<Profile>> = TODO()
    override suspend fun updateProfile(apiKey: String, authorization: String, idFilter: String, profile: UpdateProfileRequest, prefer: String): Response<List<Profile>> = TODO()
    override suspend fun updateProfileMap(apiKey: String, authorization: String, idFilter: String, profile: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody> = TODO()
    override suspend fun insertProfile(apiKey: String, authorization: String, profile: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody> = TODO()
    override suspend fun insertProfileDto(apiKey: String, authorization: String, profile: InsertProfileRequest, prefer: String): Response<List<Profile>> = TODO()
    override suspend fun deleteProfile(apiKey: String, authorization: String, idFilter: String): Response<ResponseBody> = TODO()
    override suspend fun getChats(apiKey: String, authorization: String, select: String): Response<List<Chat>> = TODO()
    override suspend fun getPublicChannels(apiKey: String, authorization: String, typeFilter: String, visibilityFilter: String, archivedFilter: String, select: String): Response<List<Chat>> = TODO()
    override suspend fun createChat(apiKey: String, authorization: String, prefer: String, chat: Map<String, @JvmSuppressWildcards Any>): Response<List<Chat>> = TODO()
    override suspend fun updateChat(apiKey: String, authorization: String, idFilter: String, updates: Map<String, @JvmSuppressWildcards Any?>): Response<ResponseBody> = TODO()
    override suspend fun getChat(apiKey: String, authorization: String, idFilter: String, select: String): Response<List<Chat>> = TODO()
    override suspend fun updateChatParticipant(apiKey: String, authorization: String, chatIdFilter: String, userIdFilter: String, updates: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody> = TODO()
    override suspend fun softDeleteThreadMessagesByChat(apiKey: String, authorization: String, threadIdFilter: String, update: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun softDeleteLegacyMessagesByChat(apiKey: String, authorization: String, chatIdFilter: String, update: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun softDeleteChat(apiKey: String, authorization: String, idFilter: String, update: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun hardDeleteChat(apiKey: String, authorization: String, idFilter: String): Response<ResponseBody> = TODO()
    override suspend fun getChatParticipant(apiKey: String, authorization: String, chatIdFilter: String, userIdFilter: String, select: String): Response<List<ChatMember>> = TODO()
    override suspend fun getChatParticipantsCount(apiKey: String, authorization: String, chatIdFilter: String, prefer: String): Response<Void> = TODO()
    override suspend fun getChatMembers(apiKey: String, authorization: String, userIdFilter: String?, select: String): Response<List<ChatMember>> = TODO()
    override suspend fun createChatMember(apiKey: String, authorization: String, member: ChatMember): Response<ResponseBody> = TODO()
    override suspend fun upsertChatMemberMap(apiKey: String, authorization: String, prefer: String, memberData: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody> = TODO()
    override suspend fun getOneToOneThreads(apiKey: String, authorization: String, select: String, orFilter: String?): Response<List<OneToOneThread>> = TODO()
    override suspend fun createOneToOneThread(apiKey: String, authorization: String, prefer: String, thread: Map<String, String>): Response<List<OneToOneThread>> = TODO()
    override suspend fun getThreadMessages(apiKey: String, authorization: String, threadIdFilter: String, createdAtFilter: String?, select: String, order: String, limit: Int): Response<List<ThreadMessage>> = TODO()
    override suspend fun getIncrementalThreadMessages(apiKey: String, authorization: String, threadIdFilter: String, updatedAtFilter: String, select: String, order: String): Response<List<ThreadMessage>> = TODO()
    override suspend fun createThreadMessage(apiKey: String, authorization: String, prefer: String, message: Map<String, @JvmSuppressWildcards Any?>): Response<ResponseBody> = TODO()
    override suspend fun updateThreadMessage(apiKey: String, authorization: String, idFilter: String, updates: Map<String, @JvmSuppressWildcards Any?>): Response<ResponseBody> = TODO()
    override suspend fun deleteThreadMessage(apiKey: String, authorization: String, idFilter: String): Response<ResponseBody> = TODO()
    override suspend fun getMessageReactions(apiKey: String, authorization: String, threadMessageIdFilter: String?, channelMessageIdFilter: String?): Response<List<MessageReaction>> = TODO()
    override suspend fun upsertMessageReaction(apiKey: String, authorization: String, prefer: String, reaction: Map<String, @JvmSuppressWildcards Any?>): Response<ResponseBody> = TODO()
    override suspend fun deleteMessageReaction(apiKey: String, authorization: String, threadMessageIdFilter: String, userIdFilter: String): Response<ResponseBody> = TODO()
    override suspend fun getMessages(apiKey: String, authorization: String, chatIdFilter: String, createdAtFilter: String?, order: String, limit: Int): Response<List<Message>> = TODO()
    override suspend fun getIncrementalMessages(apiKey: String, authorization: String, chatIdFilter: String, updatedAtFilter: String, order: String): Response<List<Message>> = TODO()
    override suspend fun createMessage(apiKey: String, authorization: String, prefer: String, message: Map<String, @JvmSuppressWildcards Any?>): Response<ResponseBody> = TODO()
    override suspend fun getUserReels(apiKey: String, authorization: String, select: String, authorFilter: String?, acceptProfile: String): Response<List<UserState>> = TODO()
    override suspend fun getUserStories(apiKey: String, authorization: String, select: String, expiresAtFilter: String?, authorFilter: String?, acceptProfile: String): Response<List<UserState>> = TODO()
    override suspend fun createReel(apiKey: String, authorization: String, state: Map<String, @JvmSuppressWildcards Any?>, prefer: String, acceptProfile: String): Response<List<UserState>> = TODO()
    override suspend fun createStory(apiKey: String, authorization: String, state: Map<String, @JvmSuppressWildcards Any?>, prefer: String, acceptProfile: String): Response<List<UserState>> = TODO()
    override suspend fun getContacts(apiKey: String, authorization: String, select: String, ownerFilter: String?): Response<List<ContactEntity>> = TODO()
    override suspend fun deleteContact(apiKey: String, authorization: String, contactUserId: String): Response<ResponseBody> = TODO()
    override suspend fun acceptFriendRequest(apiKey: String, authorization: String, body: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun declineFriendRequest(apiKey: String, authorization: String, body: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun sendFriendRequest(apiKey: String, authorization: String, body: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun getFriendRequests(apiKey: String, authorization: String, select: String, receiverFilter: String, statusFilter: String): Response<List<FriendRequestEntity>> = TODO()
    override suspend fun getContactsWithProfiles(apiKey: String, authorization: String, select: String, ownerFilter: String): Response<List<ContactWithProfileEntity>> = TODO()
    override suspend fun getFollowers(apiKey: String, authorization: String, select: String, followerIdFilter: String?, followedIdFilter: String?, acceptProfile: String): Response<List<FollowerDto>> = TODO()
    override suspend fun followUser(apiKey: String, authorization: String, body: Map<String, String>, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun unfollowUser(apiKey: String, authorization: String, followerIdFilter: String, followedIdFilter: String, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun createReel(apiKey: String, authorization: String, state: Map<String, @JvmSuppressWildcards Any?>, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun createStory(apiKey: String, authorization: String, state: Map<String, @JvmSuppressWildcards Any?>, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun clearChatRpc(apiKey: String, authorization: String, params: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun hideChatRpc(apiKey: String, authorization: String, params: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun getOrCreateThread(apiKey: String, authorization: String, params: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun getMyContactIdentifier(apiKey: String, authorization: String, body: Map<String, String>): Response<ContactIdentifierResponse> = TODO()
    override suspend fun addContactByPin(apiKey: String, authorization: String, params: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun sendFriendRequestByPin(apiKey: String, authorization: String, params: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun sendFriendRequestByQr(apiKey: String, authorization: String, params: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun markThreadDelivered(apiKey: String, authorization: String, params: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun markThreadRead(apiKey: String, authorization: String, params: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun getActiveMediaStatuses(apiKey: String, authorization: String, params: Map<String, Int>): Response<List<UserState>> = TODO()
    override suspend fun toggleLikeRpc(apiKey: String, authorization: String, params: Map<String, @JvmSuppressWildcards Any>, acceptProfile: String, contentProfile: String): Response<ToggleLikeResponseDto> = TODO()
    override suspend fun toggleFavoriteRpc(apiKey: String, authorization: String, params: Map<String, @JvmSuppressWildcards Any>, acceptProfile: String, contentProfile: String): Response<ToggleFavoriteResponseDto> = TODO()
    override suspend fun toggleStoryLikeRpc(apiKey: String, authorization: String, params: Map<String, String>, acceptProfile: String, contentProfile: String): Response<ToggleLikeResponseDto> = TODO()
    override suspend fun toggleStoryFavoriteRpc(apiKey: String, authorization: String, params: Map<String, String>, acceptProfile: String, contentProfile: String): Response<ToggleFavoriteResponseDto> = TODO()
    override suspend fun toggleReelLikeRpc(apiKey: String, authorization: String, params: Map<String, String>, acceptProfile: String, contentProfile: String): Response<ToggleLikeResponseDto> = TODO()
    override suspend fun toggleReelFavoriteRpc(apiKey: String, authorization: String, params: Map<String, String>, acceptProfile: String, contentProfile: String): Response<ToggleFavoriteResponseDto> = TODO()
    override suspend fun getStateLikes(table: String, apiKey: String, authorization: String, filters: Map<String, String>, select: String, acceptProfile: String): Response<List<LikeDto>> = TODO()
    override suspend fun getUserLikes(table: String, apiKey: String, authorization: String, filters: Map<String, String>, select: String, acceptProfile: String): Response<List<LikeDto>> = TODO()
    override suspend fun getStateFavorites(table: String, apiKey: String, authorization: String, filters: Map<String, String>, select: String, acceptProfile: String): Response<List<LikeDto>> = TODO()
    override suspend fun getUserFavorites(table: String, apiKey: String, authorization: String, filters: Map<String, String>, select: String, acceptProfile: String): Response<List<LikeDto>> = TODO()
    override suspend fun getStateComments(table: String, apiKey: String, authorization: String, filters: Map<String, String>, select: String, order: String, acceptProfile: String): Response<List<StateCommentDto>> = TODO()
    override suspend fun getStatusViews(table: String, apiKey: String, authorization: String, filters: Map<String, String>, select: String, acceptProfile: String): Response<List<StatusViewDto>> = TODO()
    override suspend fun deleteReel(apiKey: String, authorization: String, idFilter: String, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun deleteStory(apiKey: String, authorization: String, idFilter: String, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun deleteComment(table: String, apiKey: String, authorization: String, idFilter: String, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun patchComment(table: String, apiKey: String, authorization: String, idFilter: String, updates: Map<String, @JvmSuppressWildcards Any?>, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun likeState(table: String, apiKey: String, authorization: String, body: Map<String, String>, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun unlikeState(table: String, apiKey: String, authorization: String, filters: Map<String, String>, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun favoriteState(table: String, apiKey: String, authorization: String, body: Map<String, String>, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun unfavoriteState(table: String, apiKey: String, authorization: String, filters: Map<String, String>, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun shareState(table: String, apiKey: String, authorization: String, body: Map<String, String>, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun commentState(table: String, apiKey: String, authorization: String, body: Map<String, String>, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun viewStatus(table: String, apiKey: String, authorization: String, body: Map<String, String>, contentProfile: String): Response<ResponseBody> = TODO()
    override suspend fun getThreadMessageFavorites(apiKey: String, authorization: String, userId: String, select: String): Response<List<Map<String, Any>>> = TODO()
    override suspend fun addFavoriteMessage(apiKey: String, authorization: String, body: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun removeFavoriteMessage(apiKey: String, authorization: String, userIdFilter: String, messageIdFilter: String): Response<ResponseBody> = TODO()
    override suspend fun uploadFile(apiKey: String, authorization: String, bucket: String, path: String, file: okhttp3.RequestBody, contentType: String): retrofit2.Response<okhttp3.ResponseBody> = TODO()
    override suspend fun getUserDeletedMessages(apiKey: String, authorization: String, userIdFilter: String, select: String): retrofit2.Response<List<Map<String, Any>>> = TODO()
    override suspend fun getNotifications(apiKey: String, authorization: String, userIdFilter: String, select: String, order: String): Response<List<NotificationDto>> = TODO()
    override suspend fun markNotificationRead(apiKey: String, authorization: String, idFilter: String, body: Map<String, Boolean>): Response<Unit> = TODO()
    override suspend fun createNotification(apiKey: String, authorization: String, body: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun clearNotification(apiKey: String, authorization: String, idFilter: String): Response<ResponseBody> = TODO()
    override suspend fun clearAllNotifications(apiKey: String, authorization: String, userIdFilter: String): Response<ResponseBody> = TODO()
    override suspend fun callEdgeFunction(url: String, apiKey: String, authorization: String, body: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun getGiphyStickers(apiKey: String, authorization: String, request: SearchStickersRequest): Response<ResponseBody> = TODO()
    override suspend fun getUserKeys(apiKey: String, authorization: String, userIdFilter: String, select: String): Response<List<UserKeyDto>> = TODO()
    override suspend fun upsertUserKey(apiKey: String, authorization: String, prefer: String, keyData: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun createDebugLog(apiKey: String, authorization: String, prefer: String, logMap: Map<String, @JvmSuppressWildcards Any?>): Response<ResponseBody> = TODO()
    override suspend fun getFeedPosts(apiKey: String, authorization: String, select: String, privacyFilter: String?, limit: Int, order: String, createdAtLt: String?): Response<List<PostDto>> = TODO()
    override suspend fun getPostById(
        apiKey: String,
        authorization: String,
        idFilter: String,
        select: String
    ): Response<List<PostDto>> {
        return Response.success(emptyList())
    }

    override suspend fun createPost(apiKey: String, authorization: String, post: PostDto): Response<List<PostDto>> = TODO()
    override suspend fun addLike(apiKey: String, authorization: String, like: PostLikeDto): Response<ResponseBody> = TODO()
    override suspend fun removeLike(apiKey: String, authorization: String, postIdFilter: String, userIdFilter: String): Response<ResponseBody> = TODO()
    override suspend fun addComment(apiKey: String, authorization: String, comment: PostCommentDto): Response<List<PostCommentDto>> = TODO()
    override suspend fun getCommentsForPost(apiKey: String, authorization: String, postIdFilter: String, order: String): Response<List<PostCommentDto>> = TODO()
    override suspend fun getUserLikes(apiKey: String, authorization: String, userIdFilter: String): Response<List<PostLikeDto>> = TODO()
    override suspend fun deletePost(apiKey: String, authorization: String, idFilter: String): Response<ResponseBody> = TODO()
    override suspend fun updatePost(apiKey: String, authorization: String, idFilter: String, updates: Map<String, @JvmSuppressWildcards Any?>): Response<List<PostDto>> = TODO()
    override suspend fun saveSticker(apiKey: String, authorization: String, params: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody> = TODO()
    override suspend fun favoriteSticker(apiKey: String, authorization: String, params: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody> = TODO()
    override suspend fun unfavoriteSticker(apiKey: String, authorization: String, params: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody> = TODO()
    override suspend fun registerStickerUsage(apiKey: String, authorization: String, params: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody> = TODO()
    override suspend fun getSavedStickers(apiKey: String, authorization: String): Response<List<Map<String, Any>>> = TODO()
    override suspend fun getFavoriteStickers(apiKey: String, authorization: String): Response<List<Map<String, Any>>> = TODO()
    override suspend fun getRecentStickers(apiKey: String, authorization: String): Response<List<Map<String, Any>>> = TODO()
    override suspend fun getBlockedUsers(apiKey: String, authorization: String, userIdFilter: String, select: String): Response<List<BlockedUser>> = TODO()
    override suspend fun blockUserApi(apiKey: String, authorization: String, body: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun unblockUserApi(apiKey: String, authorization: String, userIdFilter: String, blockedUserIdFilter: String): Response<ResponseBody> = TODO()
    override suspend fun deleteMessageForMeRpc(apiKey: String, authorization: String, params: Map<String, String>): Response<ResponseBody> = TODO()
    override suspend fun updateChatMuteStatusRpc(apiKey: String, authorization: String, params: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody> = TODO()
    override suspend fun updateChatPinStatusRpc(apiKey: String, authorization: String, params: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody> = TODO()
    override suspend fun upsertUserPresence(apiKey: String, authorization: String, prefer: String, presence: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody> = TODO()
    override suspend fun getUserPresence(apiKey: String, authorization: String, userIdFilter: String?): Response<List<Map<String, Any>>> = TODO()
    override suspend fun getPresenceSessions(apiKey: String, authorization: String, userIdFilter: String, order: String): Response<List<PresenceSession>> = TODO()

    // --- Music Social Sync Implementation ---

    override suspend fun getMusicPlaylists(
        apiKey: String,
        authorization: String,
        select: String,
        ownerId: String?,
        updatedAtGt: String?
    ): Response<List<RemoteMusicPlaylist>> {
        return Response.success(playlists)
    }

    override suspend fun upsertMusicPlaylist(
        apiKey: String,
        authorization: String,
        playlist: RemoteMusicPlaylist
    ): Response<List<RemoteMusicPlaylist>> {
        upsertedPlaylists.add(playlist)
        val synced = playlist.copy(id = playlist.id ?: "remote-id-${System.currentTimeMillis()}")
        return Response.success(listOf(synced))
    }

    override suspend fun getMusicPlaylistTracks(
        apiKey: String,
        authorization: String,
        playlistId: String,
        select: String
    ): Response<List<RemoteMusicPlaylistTrack>> {
        return Response.success(tracks[playlistId] ?: emptyList())
    }

    override suspend fun upsertMusicPlaylistTracks(
        apiKey: String,
        authorization: String,
        prefer: String,
        tracks: List<RemoteMusicPlaylistTrack>
    ): Response<Unit> {
        upsertedTracks.addAll(tracks)
        return Response.success(Unit)
    }

    override suspend fun getMusicPlaylistCollaborators(
        apiKey: String,
        authorization: String,
        playlistId: String
    ): Response<List<RemoteMusicPlaylistCollaborator>> {
        return Response.success(emptyList())
    }

    override suspend fun getMusicPlaylistShares(
        apiKey: String,
        authorization: String,
        userId: String
    ): Response<List<RemoteMusicPlaylistShare>> {
        return Response.success(emptyList())
    }

    override suspend fun getMusicPlaylistInvitations(
        apiKey: String,
        authorization: String,
        select: String,
        receiverId: String?,
        playlistId: String?,
        status: String?
    ): Response<List<RemoteMusicPlaylistInvitation>> {
        return Response.success(emptyList())
    }

    override suspend fun createMusicPlaylistInvitation(
        apiKey: String,
        authorization: String,
        invitation: RemoteMusicPlaylistInvitation
    ): Response<List<RemoteMusicPlaylistInvitation>> {
        return Response.success(listOf(invitation))
    }

    override suspend fun updateMusicPlaylistInvitation(
        apiKey: String,
        authorization: String,
        idFilter: String,
        updates: Map<String, String>
    ): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun acceptMusicPlaylistInvitation(
        apiKey: String,
        authorization: String,
        params: Map<String, String>
    ): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun rejectMusicPlaylistInvitation(
        apiKey: String,
        authorization: String,
        params: Map<String, String>
    ): Response<Unit> {
        return Response.success(Unit)
    }

    override suspend fun revokeMusicPlaylistInvitation(
        apiKey: String,
        authorization: String,
        params: Map<String, String>
    ): Response<Unit> {
        return Response.success(Unit)
    }
}
