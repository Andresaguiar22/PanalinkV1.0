package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MessageEntity::class,
        ChatEntity::class,
        ProfileEntity::class,
        DraftEntity::class,
        ReactionEntity::class,
        PendingUploadEntity::class,
        PendingPostEntity::class,
        StateEntity::class,
        com.example.media.model.MediaAssetEntity::class,
        com.example.creative.persistence.CreativeProjectEntity::class,
        com.example.media.audio.AudioTrackEntity::class,
        com.example.media.playlist.PlaylistEntity::class,
        com.example.media.playlist.PlaylistTrackEntity::class,
        com.example.media.playlist.PlaylistCollaboratorEntity::class,
        com.example.media.playlist.PlaylistInvitationEntity::class,
        PublicProfileEntity::class,
        PostEntity::class,
        CommentEntity::class,
        PendingSocialActionEntity::class,
        LocalNotificationEntity::class
    ],
    version = 39,
    exportSchema = true
)
abstract class PanalinkDatabase : RoomDatabase() {
    abstract fun localNotificationDao(): LocalNotificationDao
    abstract fun messageDao(): MessageDao
    abstract fun chatDao(): ChatDao
    abstract fun profileDao(): ProfileDao
    abstract fun publicProfileDao(): PublicProfileDao
    abstract fun draftDao(): DraftDao
    abstract fun reactionDao(): ReactionDao
    abstract fun pendingUploadDao(): PendingUploadDao
    abstract fun pendingPostDao(): PendingPostDao
    abstract fun statesDao(): StatesDao
    abstract fun mediaAssetDao(): MediaAssetDao
    abstract fun creativeProjectDao(): com.example.creative.persistence.CreativeProjectDao
    abstract fun audioDao(): com.example.media.audio.AudioDao
    abstract fun playlistDao(): com.example.media.playlist.PlaylistDao
    abstract fun collaboratorDao(): com.example.media.playlist.CollaboratorDao
    abstract fun invitationDao(): com.example.media.playlist.PlaylistInvitationDao
    abstract fun postDao(): PostDao
    abstract fun commentDao(): CommentDao
    abstract fun pendingSocialActionDao(): PendingSocialActionDao

    companion object {
        @Volatile
        private var INSTANCE: PanalinkDatabase? = null

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_uploads` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `uploadType` TEXT NOT NULL,
                        `localFilePath` TEXT NOT NULL,
                        `thumbnailPath` TEXT,
                        `mimeType` TEXT NOT NULL,
                        `caption` TEXT,
                        `metadataJson` TEXT,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `errorMessage` TEXT,
                        `remoteUrl` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_posts` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `content` TEXT,
                        `type` TEXT NOT NULL,
                        `mediaUrisJson` TEXT NOT NULL,
                        `privacy` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `progress` REAL NOT NULL DEFAULT 0.0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_chats ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_chats ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_chats ADD COLUMN pinned_at TEXT")
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_messages ADD COLUMN updatedAt TEXT")
                db.execSQL("ALTER TABLE local_messages ADD COLUMN editPending INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_messages ADD COLUMN reactionPending INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_messages ADD COLUMN deletePending INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_26_27 = object : Migration(26, 27) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `notifications_v2` (
                        `id` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `interruptiveness` TEXT NOT NULL,
                        `actorId` TEXT,
                        `actorName` TEXT,
                        `actorUsername` TEXT,
                        `actorAvatarUrl` TEXT,
                        `actorIsVerified` INTEGER NOT NULL DEFAULT 0,
                        `targetEntityId` TEXT,
                        `targetEntityType` TEXT,
                        `targetParentEntityId` TEXT,
                        `targetTitle` TEXT,
                        `targetPreviewText` TEXT,
                        `deepLinkUrl` TEXT,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `attachmentsJson` TEXT,
                        `payloadJson` TEXT,
                        `groupingKey` TEXT,
                        `groupSummaryText` TEXT,
                        `isGrouped` INTEGER NOT NULL DEFAULT 0,
                        `isRead` INTEGER NOT NULL DEFAULT 0,
                        `timestamp` INTEGER NOT NULL,
                        `expiresAt` INTEGER,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_v2_type` ON `notifications_v2` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_v2_domain` ON `notifications_v2` (`domain`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_v2_isRead` ON `notifications_v2` (`isRead`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_v2_timestamp` ON `notifications_v2` (`timestamp`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_notifications_v2_groupingKey` ON `notifications_v2` (`groupingKey`)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `activity_feed_v2` (
                        `id` TEXT NOT NULL,
                        `domain` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `actorId` TEXT,
                        `actorName` TEXT,
                        `actorAvatarUrl` TEXT,
                        `targetEntityId` TEXT,
                        `targetEntityType` TEXT,
                        `title` TEXT NOT NULL,
                        `body` TEXT NOT NULL,
                        `mediaPreviewUrl` TEXT,
                        `timestamp` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_feed_v2_domain` ON `activity_feed_v2` (`domain`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_feed_v2_type` ON `activity_feed_v2` (`type`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_feed_v2_timestamp` ON `activity_feed_v2` (`timestamp`)")
            }
        }

        val MIGRATION_27_28 = object : Migration(27, 28) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE local_profiles ADD COLUMN avatarLocalPath TEXT")
                db.execSQL("ALTER TABLE local_profiles ADD COLUMN coverLocalPath TEXT")
                db.execSQL("ALTER TABLE local_profiles ADD COLUMN updatedAt TEXT")
                db.execSQL("ALTER TABLE local_profiles ADD COLUMN lastSyncedAt INTEGER")
                db.execSQL("ALTER TABLE local_profiles ADD COLUMN syncVersion INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_profiles ADD COLUMN isDirty INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE local_profiles ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
            }
        }
        
        val MIGRATION_28_29 = object : Migration(28, 29) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `media_assets` (
                        `id` TEXT NOT NULL,
                        `ownerId` TEXT,
                        `type` TEXT NOT NULL,
                        `remoteUrl` TEXT,
                        `localPath` TEXT,
                        `thumbnailPath` TEXT,
                        `mimeType` TEXT,
                        `sizeBytes` INTEGER NOT NULL,
                        `width` INTEGER,
                        `height` INTEGER,
                        `durationMs` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `lastSyncedAt` INTEGER NOT NULL,
                        `syncState` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `creative_projects` (
                        `id` TEXT NOT NULL,
                        `sourceMedia` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `layersJson` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `tempExportPath` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_29_30 = object : Migration(29, 30) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `audio_tracks` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `album` TEXT NOT NULL,
                        `coverPath` TEXT,
                        `durationMs` INTEGER NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `playlistId` TEXT,
                        `isFavorite` INTEGER NOT NULL DEFAULT 0,
                        `trackType` TEXT NOT NULL DEFAULT 'MUSIC',
                        `fileHash` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `playlists` (
                        `id` TEXT NOT NULL,
                        `ownerId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `coverImage` TEXT,
                        `description` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `isPublic` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `playlist_songs` (
                        `id` TEXT NOT NULL,
                        `playlistId` TEXT NOT NULL,
                        `trackId` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL DEFAULT 0,
                        `addedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_30_31 = object : Migration(30, 31) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Update Playlists table
                db.execSQL("ALTER TABLE `playlists` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `playlists` ADD COLUMN `isCollaborative` INTEGER NOT NULL DEFAULT 0")
                
                // Rename coverImage to coverPath if it exists (SQLITE ALTER RENAME is limited, so we add and copy if needed, or just add if it's new)
                // Since 29_30 added coverImage, we'll try to keep it simple.
                // SQLite 3.25.0+ supports RENAME COLUMN.
                try {
                    db.execSQL("ALTER TABLE `playlists` RENAME COLUMN `coverImage` TO `coverPath`")
                } catch (e: Exception) {
                    // Fallback: add coverPath if rename fails
                    db.execSQL("ALTER TABLE `playlists` ADD COLUMN `coverPath` TEXT")
                }
            }
        }

        val MIGRATION_31_32 = object : Migration(31, 32) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `playlist_collaborators` (
                        `id` TEXT NOT NULL,
                        `playlistId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `isDirty` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_playlist_collaborators_playlistId_userId` ON `playlist_collaborators` (`playlistId`, `userId`)")
            }
        }

        val MIGRATION_32_33 = object : Migration(32, 33) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `playlist_invitations` (
                        `id` TEXT NOT NULL,
                        `playlistId` TEXT NOT NULL,
                        `senderId` TEXT NOT NULL,
                        `receiverId` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `expiresAt` INTEGER,
                        `isDirty` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_invitations_receiverId_status` ON `playlist_invitations` (`receiverId`, `status`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_invitations_playlistId` ON `playlist_invitations` (`playlistId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_invitations_isDirty` ON `playlist_invitations` (`isDirty`)")
            }
        }

        val MIGRATION_33_34 = object : Migration(33, 34) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add missing column 'musicPlaylistId' to 'local_messages'
                try {
                    db.execSQL("ALTER TABLE `local_messages` ADD COLUMN `musicPlaylistId` TEXT")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 2. Add missing column 'isDirty' to 'playlist_songs'
                try {
                    db.execSQL("ALTER TABLE `playlist_songs` ADD COLUMN `isDirty` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 3. Add missing columns to 'audio_tracks' (genre, remoteId, lastSyncAt, isDirty)
                try {
                    db.execSQL("ALTER TABLE `audio_tracks` ADD COLUMN `genre` TEXT NOT NULL DEFAULT 'Desconocido'")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    db.execSQL("ALTER TABLE `audio_tracks` ADD COLUMN `remoteId` TEXT")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    db.execSQL("ALTER TABLE `audio_tracks` ADD COLUMN `lastSyncAt` INTEGER")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    db.execSQL("ALTER TABLE `audio_tracks` ADD COLUMN `isDirty` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // 4. Add missing columns to 'playlists' (remoteId, lastSyncAt, isDirty)
                try {
                    db.execSQL("ALTER TABLE `playlists` ADD COLUMN `remoteId` TEXT")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    db.execSQL("ALTER TABLE `playlists` ADD COLUMN `lastSyncAt` INTEGER")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    db.execSQL("ALTER TABLE `playlists` ADD COLUMN `isDirty` INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        val MIGRATION_34_35 = object : Migration(34, 35) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create the new table with the exact schema Room expects
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `playlist_songs_new` (
                        `id` TEXT NOT NULL,
                        `playlistId` TEXT NOT NULL,
                        `trackId` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL,
                        `isDirty` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`trackId`) REFERENCES `audio_tracks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())

                // 2. Copy the data from the old table
                db.execSQL("""
                    INSERT INTO `playlist_songs_new` (`id`, `playlistId`, `trackId`, `orderIndex`, `addedAt`, `isDirty`)
                    SELECT `id`, `playlistId`, `trackId`, `orderIndex`, `addedAt`, IFNULL(`isDirty`, 0)
                    FROM `playlist_songs`
                """.trimIndent())

                // 3. Drop the old table
                db.execSQL("DROP TABLE `playlist_songs`")

                // 4. Rename the new table
                db.execSQL("ALTER TABLE `playlist_songs_new` RENAME TO `playlist_songs`")

                // 5. Create indices
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId` ON `playlist_songs` (`playlistId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_playlist_songs_trackId` ON `playlist_songs` (`trackId`)")
            }
        }

        val MIGRATION_35_36 = object : Migration(35, 36) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `public_profiles` (
                        `id` TEXT NOT NULL,
                        `displayName` TEXT,
                        `firstName` TEXT,
                        `lastName` TEXT,
                        `avatarUrl` TEXT,
                        `updatedAt` TEXT,
                        `lastSyncedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_36_37 = object : Migration(36, 37) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `local_posts` (
                        `id` TEXT NOT NULL,
                        `authorId` TEXT NOT NULL,
                        `type` TEXT,
                        `content` TEXT,
                        `mediaUrlsJson` TEXT,
                        `audioUrl` TEXT,
                        `privacy` TEXT,
                        `likesCount` INTEGER NOT NULL,
                        `commentsCount` INTEGER NOT NULL,
                        `shareCount` INTEGER NOT NULL DEFAULT 0,
                        `currentUserLiked` INTEGER NOT NULL,
                        `visibility` TEXT,
                        `deletedAt` TEXT,
                        `createdAt` TEXT,
                        `updatedAt` TEXT,
                        `syncedAt` INTEGER NOT NULL,
                        `previewMetadataJson` TEXT,
                        `customMediaIdsJson` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_37_38 = object : Migration(37, 38) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `local_comments` (
                        `id` TEXT NOT NULL,
                        `targetId` TEXT NOT NULL,
                        `authorId` TEXT NOT NULL,
                        `authorName` TEXT NOT NULL,
                        `authorAvatarUrl` TEXT,
                        `content` TEXT NOT NULL,
                        `createdAt` TEXT NOT NULL,
                        `isReel` INTEGER NOT NULL,
                        `parentCommentId` TEXT,
                        `deletedAt` TEXT,
                        `syncStatus` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `pending_social_actions` (
                        `localActionId` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `targetId` TEXT NOT NULL,
                        `actionType` TEXT NOT NULL,
                        `payload` TEXT,
                        `isReel` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `retryCount` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        PRIMARY KEY(`localActionId`)
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_38_39 = object : Migration(38, 39) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `local_notifications` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `actorId` TEXT NOT NULL,
                        `actorName` TEXT NOT NULL,
                        `actorAvatarUrl` TEXT,
                        `timestamp` TEXT NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `actionText` TEXT NOT NULL,
                        `previewText` TEXT,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())
            }
        }

        fun getDatabase(context: Context): PanalinkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PanalinkDatabase::class.java,
                    "panalink_database"
                )
                .addMigrations(
                    MIGRATION_11_12, MIGRATION_12_13, MIGRATION_22_23, MIGRATION_23_24,
                    MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29,
                    MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33,
                    MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37,
                    MIGRATION_37_38, MIGRATION_38_39
                )
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
