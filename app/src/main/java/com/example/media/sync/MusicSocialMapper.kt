package com.example.media.sync

import com.example.data.model.RemoteMusicPlaylist
import com.example.data.model.RemoteMusicPlaylistCollaborator
import com.example.data.model.RemoteMusicPlaylistInvitation
import com.example.media.playlist.PlaylistCollaboratorEntity
import com.example.media.playlist.PlaylistEntity
import com.example.media.playlist.PlaylistInvitationEntity
import java.util.*

/**
 * P6.7.6B - Music Social Mapper
 * Decouples mapping logic for easier testing.
 */
object MusicSocialMapper {

    fun toLocalEntity(remote: RemoteMusicPlaylist): PlaylistEntity {
        return PlaylistEntity(
            id = remote.id ?: "",
            ownerId = remote.owner_id,
            name = remote.title,
            description = remote.description,
            coverPath = remote.cover_cdn_url,
            remoteId = remote.id,
            lastSyncAt = System.currentTimeMillis(),
            updatedAt = parseIsoTimestamp(remote.updated_at),
            isDirty = false
        )
    }

    fun toRemoteDto(local: PlaylistEntity): RemoteMusicPlaylist {
        return RemoteMusicPlaylist(
            id = local.remoteId,
            owner_id = local.ownerId,
            title = local.name,
            description = local.description,
            cover_cdn_url = local.coverPath,
            privacy = if (local.isPublic) "PUBLIC" else "PRIVATE",
            is_collaborative = local.isCollaborative,
            updated_at = formatToIso(local.updatedAt)
        )
    }

    private fun parseIsoTimestamp(iso: String?): Long {
        if (iso == null) return 0L
        return try {
            val cleanedIso = iso.replace("Z", "+0000")
            val pattern = if (cleanedIso.contains(".")) {
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"
            } else {
                "yyyy-MM-dd'T'HH:mm:ssZ"
            }
            java.text.SimpleDateFormat(pattern, Locale.US).parse(cleanedIso)?.time ?: 0L
        } catch (e: Exception) {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).parse(iso?.replace("Z", "+0000") ?: "")?.time ?: 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }

    private fun formatToIso(timestamp: Long): String {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date(timestamp))
    }

    fun toLocal(remote: RemoteMusicPlaylistCollaborator): PlaylistCollaboratorEntity {
        return PlaylistCollaboratorEntity(
            id = remote.id ?: UUID.randomUUID().toString(),
            playlistId = remote.playlist_id,
            userId = remote.user_id,
            role = remote.role,
            updatedAt = parseIsoTimestamp(remote.created_at),
            isDirty = false
        )
    }

    fun toRemote(local: PlaylistCollaboratorEntity): RemoteMusicPlaylistCollaborator {
        return RemoteMusicPlaylistCollaborator(
            id = local.id,
            playlist_id = local.playlistId,
            user_id = local.userId,
            role = local.role,
            created_at = formatToIso(local.updatedAt)
        )
    }

    fun toLocal(remote: RemoteMusicPlaylistInvitation): PlaylistInvitationEntity {
        return PlaylistInvitationEntity(
            id = remote.id ?: UUID.randomUUID().toString(),
            playlistId = remote.playlist_id,
            senderId = remote.sender_id,
            receiverId = remote.receiver_id,
            role = remote.role,
            status = remote.status,
            createdAt = parseIsoTimestamp(remote.created_at),
            updatedAt = parseIsoTimestamp(remote.updated_at),
            expiresAt = parseIsoTimestamp(remote.expires_at).takeIf { it > 0 },
            isDirty = false
        )
    }

    fun toRemote(local: PlaylistInvitationEntity): RemoteMusicPlaylistInvitation {
        return RemoteMusicPlaylistInvitation(
            id = local.id,
            playlist_id = local.playlistId,
            sender_id = local.senderId,
            receiver_id = local.receiverId,
            role = local.role,
            status = local.status,
            created_at = formatToIso(local.createdAt),
            updated_at = formatToIso(local.updatedAt),
            expires_at = local.expiresAt?.let { formatToIso(it) }
        )
    }
}
