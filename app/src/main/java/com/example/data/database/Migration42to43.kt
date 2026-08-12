package com.example.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_42_43 = object : Migration(42, 43) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `pending_social_actions` ADD COLUMN `actionFamily` TEXT"
        )
        db.execSQL(
            "ALTER TABLE `pending_social_actions` ADD COLUMN `desiredState` INTEGER"
        )
        db.execSQL(
            "ALTER TABLE `pending_social_actions` ADD COLUMN `revision` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            """
            CREATE UNIQUE INDEX IF NOT EXISTS
            `index_pending_social_actions_userId_targetId_isReel_actionFamily`
            ON `pending_social_actions`
            (`userId`, `targetId`, `isReel`, `actionFamily`)
            """.trimIndent()
        )
    }
}
