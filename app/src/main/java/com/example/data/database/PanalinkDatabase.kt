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
    version = 43,
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

        // Existing migrations 11 -> 42 are intentionally preserved.
        val MIGRATION_11_12 = object : Migration(11, 12) { override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS `pending_uploads` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `uploadType` TEXT NOT NULL, `localFilePath` TEXT NOT NULL, `thumbnailPath` TEXT, `mimeType` TEXT NOT NULL, `caption` TEXT, `metadataJson` TEXT, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `retryCount` INTEGER NOT NULL, `errorMessage` TEXT, `remoteUrl` TEXT, PRIMARY KEY(`id`))""")
        }}
        val MIGRATION_12_13 = object : Migration(12, 13) { override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""CREATE TABLE IF NOT EXISTS `pending_posts` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `content` TEXT, `type` TEXT NOT NULL, `mediaUrisJson` TEXT NOT NULL, `privacy` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `progress` REAL NOT NULL DEFAULT 0.0, PRIMARY KEY(`id`))""")
        }}
        val MIGRATION_22_23 = object : Migration(22, 23) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE local_chats ADD COLUMN isMuted INTEGER NOT NULL DEFAULT 0") } }
        val MIGRATION_23_24 = object : Migration(23, 24) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE local_chats ADD COLUMN is_pinned INTEGER NOT NULL DEFAULT 0"); db.execSQL("ALTER TABLE local_chats ADD COLUMN pinned_at TEXT") } }
        val MIGRATION_25_26 = object : Migration(25, 26) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE local_messages ADD COLUMN updatedAt TEXT"); db.execSQL("ALTER TABLE local_messages ADD COLUMN editPending INTEGER NOT NULL DEFAULT 0"); db.execSQL("ALTER TABLE local_messages ADD COLUMN reactionPending INTEGER NOT NULL DEFAULT 0"); db.execSQL("ALTER TABLE local_messages ADD COLUMN deletePending INTEGER NOT NULL DEFAULT 0") } }
        val MIGRATION_26_27 = object : Migration(26, 27) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_27_28 = object : Migration(27, 28) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_28_29 = object : Migration(28, 29) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_29_30 = object : Migration(29, 30) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_30_31 = object : Migration(30, 31) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_31_32 = object : Migration(31, 32) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_32_33 = object : Migration(32, 33) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_33_34 = object : Migration(33, 34) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_34_35 = object : Migration(34, 35) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_35_36 = object : Migration(35, 36) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_36_37 = object : Migration(36, 37) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_37_38 = object : Migration(37, 38) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_38_39 = object : Migration(38, 39) { override fun migrate(db: SupportSQLiteDatabase) { /* preserved in existing schema history */ } }
        val MIGRATION_39_40 = object : Migration(39, 40) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE local_messages ADD COLUMN receiverId TEXT") } }
        val MIGRATION_40_41 = object : Migration(40, 41) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("UPDATE local_messages SET clientMessageUuid = NULL WHERE clientMessageUuid = ''"); db.execSQL("DROP INDEX IF EXISTS index_local_messages_clientMessageUuid"); db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_local_messages_clientMessageUuid ON local_messages (`clientMessageUuid`)") } }
        val MIGRATION_41_42 = object : Migration(41, 42) { override fun migrate(db: SupportSQLiteDatabase) { db.execSQL("ALTER TABLE local_chats ADD COLUMN threadId TEXT DEFAULT NULL") } }

        /** v43 makes the social-action domain explicit while preserving old rows. */
        val MIGRATION_42_43 = object : Migration(42, 43) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE pending_social_actions ADD COLUMN targetType TEXT NOT NULL DEFAULT 'UNKNOWN'")

                // Reconcile historical rows using the authoritative StateEntity when available.
                // user_reels/user_stories are represented locally by StateEntity.type.
                db.execSQL("""
                    UPDATE pending_social_actions
                    SET targetType = CASE
                        WHEN isReel = 1 AND EXISTS (
                            SELECT 1 FROM local_states s WHERE s.id = pending_social_actions.targetId AND s.type = 'reel'
                        ) THEN 'REEL'
                        WHEN isReel = 1 AND EXISTS (
                            SELECT 1 FROM local_states s WHERE s.id = pending_social_actions.targetId AND s.type = 'story'
                        ) THEN 'STORY'
                        WHEN isReel = 1 THEN 'REEL'
                        ELSE 'POST'
                    END
                    WHERE targetType = 'UNKNOWN'
                """.trimIndent())

                db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_social_actions_targetType ON pending_social_actions (targetType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_pending_social_actions_user_target_status ON pending_social_actions (userId, targetId, status)")
            }
        }

        fun getDatabase(context: Context): PanalinkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PanalinkDatabase::class.java,
                    "panalink_database"
                ).addMigrations(
                    MIGRATION_11_12, MIGRATION_12_13, MIGRATION_22_23, MIGRATION_23_24,
                    MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29,
                    MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33,
                    MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37,
                    MIGRATION_37_38, MIGRATION_38_39, MIGRATION_39_40, MIGRATION_40_41,
                    MIGRATION_41_42, MIGRATION_42_43
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
