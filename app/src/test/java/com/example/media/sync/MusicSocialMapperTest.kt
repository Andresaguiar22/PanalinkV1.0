package com.example.media.sync

import com.example.data.model.RemoteMusicPlaylist
import com.example.media.playlist.PlaylistEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MusicSocialMapperTest {

    @Test
    fun `map remote to local`() {
        val remote = RemoteMusicPlaylist(
            id = "test-uuid",
            owner_id = "user-1",
            title = "My Hits",
            description = "Good vibes",
            cover_cdn_url = "https://cdn.com/cover.jpg"
        )
        
        val local = MusicSocialMapper.toLocalEntity(remote)
        
        assertEquals("test-uuid", local.id)
        assertEquals("user-1", local.ownerId)
        assertEquals("My Hits", local.name)
        assertEquals("Good vibes", local.description)
        assertEquals("https://cdn.com/cover.jpg", local.coverPath)
        assertEquals(false, local.isDirty)
    }

    @Test
    fun `map local to remote`() {
        val local = PlaylistEntity(
            id = "local-id",
            ownerId = "user-1",
            name = "Local Hits",
            description = "Offline vibes",
            coverPath = "local/path/img.jpg",
            remoteId = "remote-uuid",
            isPublic = true,
            isCollaborative = true
        )
        
        val remote = MusicSocialMapper.toRemoteDto(local)
        
        assertEquals("remote-uuid", remote.id)
        assertEquals("user-1", remote.owner_id)
        assertEquals("Local Hits", remote.title)
        assertEquals("Offline vibes", remote.description)
        assertEquals("local/path/img.jpg", remote.cover_cdn_url)
        assertEquals("PUBLIC", remote.privacy)
        assertEquals(true, remote.is_collaborative)
    }
}
