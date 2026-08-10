package com.example.media.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.RemoteMusicPlaylist
import com.example.data.supabase.SupabaseApiService
import com.example.media.audio.AudioRepository
import com.example.media.playlist.PlaylistEntity
import com.example.media.playlist.PlaylistRepository
import com.example.media.playlist.PlaylistInvitationRepository
import androidx.room.Room
import com.example.data.database.PanalinkDatabase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MusicSocialSyncManagerTest {

    private lateinit var db: PanalinkDatabase
    private lateinit var syncManager: MusicSocialSyncManager
    private lateinit var fakePlaylistRepo: PlaylistRepository
    private lateinit var fakeAudioRepo: AudioRepository
    private lateinit var fakeSupabaseApi: FakeSupabaseApi
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, PanalinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        fakePlaylistRepo = PlaylistRepository(db.playlistDao(), db.collaboratorDao())
        fakeAudioRepo = AudioRepository(db.audioDao())
        fakeSupabaseApi = FakeSupabaseApi()
        
        val invitationRepo = PlaylistInvitationRepository(
            db.invitationDao(),
            fakeSupabaseApi,
            "test-api-key"
        )
        
        syncManager = MusicSocialSyncManager(
            context = context,
            supabaseApi = fakeSupabaseApi,
            playlistRepo = fakePlaylistRepo,
            audioRepo = fakeAudioRepo,
            invitationRepo = invitationRepo,
            apiKey = "test-api-key"
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `test syncing unsynced local playlists pushes to remote`() = runBlocking {
        // 1. Create a local dirty playlist
        val local = PlaylistEntity(
            id = "local-1",
            ownerId = "user-123",
            name = "My New Playlist",
            isDirty = true
        )
        fakePlaylistRepo.createPlaylist(local)
        
        // 2. Run sync
        syncManager.syncFull("user-123", "token-abc")
        
        // 3. Verify it was pushed to fake remote
        assertEquals(1, fakeSupabaseApi.upsertedPlaylists.size)
        assertEquals("My New Playlist", fakeSupabaseApi.upsertedPlaylists[0].title)
        
        // 4. Verify local is no longer dirty
        val updatedLocal = fakePlaylistRepo.getPlaylistById("local-1")
        assertEquals(false, updatedLocal?.isDirty)
    }

    @Test
    fun `test syncing from remote updates local db`() = runBlocking {
        // 1. Setup remote data
        fakeSupabaseApi.playlists.add(
            RemoteMusicPlaylist(
                id = "remote-1",
                owner_id = "user-123",
                title = "Remote Hits"
            )
        )
        
        // 2. Run sync
        syncManager.syncFull("user-123", "token-abc")
        
        // 3. Verify local DB has the playlist
        val local = fakePlaylistRepo.getPlaylistById("remote-1")
        assertEquals("Remote Hits", local?.name)
    }
}
