package com.example.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PanalinkDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun testMigration34To35() {
        // 1. Create the database in version 34 using the exported schema
        var db = helper.createDatabase(TEST_DB, 34)

        // Insert test data for playlist_songs. The schemas of playlists and audio_tracks need to be populated
        // if we have foreign keys, but in V34 playlist_songs doesn't have FKs yet, so we can insert directly.
        // But since we are migrating to V35 which has FKs, the FK constraints will fail if the parent records don't exist!
        // We MUST insert parent records first.
        db.execSQL("INSERT INTO playlists (id, ownerId, name, createdAt, isPublic, updatedAt, isCollaborative, isDirty) VALUES ('p1', 'u1', 'Test Playlist', 123456, 1, 0, 0, 0)")
        db.execSQL("INSERT INTO audio_tracks (id, userId, title, artist, album, durationMs, filePath, createdAt, isFavorite, trackType, genre, isDirty) VALUES ('t1', 'u1', 'Track 1', 'Artist', 'Album', 1000, '/path', 123456, 0, 'MUSIC', 'Desconocido', 0)")
        
        db.execSQL("INSERT INTO playlist_songs (id, playlistId, trackId, orderIndex, addedAt, isDirty) VALUES ('ps1', 'p1', 't1', 1, 123456, 0)")
        db.execSQL("INSERT INTO playlist_songs (id, playlistId, trackId, orderIndex, addedAt, isDirty) VALUES ('ps2', 'p1', 't1', 2, 123457, 1)")

        // Prepare for the next version
        db.close()

        // 2. Re-open the database with version 35 and provide MIGRATION_34_35 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 35, true, PanalinkDatabase.MIGRATION_34_35)

        // Query to check if data is preserved and normalized
        val cursor = db.query("SELECT * FROM playlist_songs")
        
        var foundPs1 = false
        var foundPs2 = false
        
        while (cursor.moveToNext()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
            val orderIndex = cursor.getInt(cursor.getColumnIndexOrThrow("orderIndex"))
            val isDirty = cursor.getInt(cursor.getColumnIndexOrThrow("isDirty"))
            
            if (id == "ps1") {
                foundPs1 = true
                assertEquals(0, isDirty)
            }
            if (id == "ps2") {
                foundPs2 = true
                assertEquals(1, isDirty)
            }
        }
        cursor.close()
        
        assertTrue(foundPs1)
        assertTrue(foundPs2)

        // Check foreign keys
        val fkCursor = db.query("PRAGMA foreign_key_list('playlist_songs')")
        var fkCount = 0
        while (fkCursor.moveToNext()) {
            val table = fkCursor.getString(fkCursor.getColumnIndexOrThrow("table"))
            val onUpdate = fkCursor.getString(fkCursor.getColumnIndexOrThrow("on_update"))
            val onDelete = fkCursor.getString(fkCursor.getColumnIndexOrThrow("on_delete"))
            
            assertEquals("NO ACTION", onUpdate)
            assertEquals("CASCADE", onDelete)
            fkCount++
        }
        fkCursor.close()
        assertEquals(2, fkCount)
        
        // Check indices
        val idxCursor = db.query("PRAGMA index_list('playlist_songs')")
        var idxCount = 0
        while (idxCursor.moveToNext()) {
            val name = idxCursor.getString(idxCursor.getColumnIndexOrThrow("name"))
            if (name == "index_playlist_songs_playlistId" || name == "index_playlist_songs_trackId") {
                idxCount++
            }
        }
        idxCursor.close()
        assertEquals(2, idxCount)
        
        // Also Room's MigrationTestHelper validate the schema under the hood!
        println("MIGRATION_34_35: PASS")
        println("playlist_songs schema: PASS")
        println("foreign keys: PASS")
        println("indices: PASS")
        println("data preservation: PASS")
        println("Room validation: PASS")
    }
}
