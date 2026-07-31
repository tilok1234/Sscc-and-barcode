package com.ssccscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DocumentEntity::class, ScanEntity::class,
        DamageReportEntity::class, DamagePhotoEntity::class, DamageNoteEntity::class,
        ScanNoteEntity::class, ScanPhotoEntity::class, FieldEditEntity::class,
        AppointmentEntity::class, AppointmentNoteEntity::class,
        ArticleEntity::class, ArticlePhotoEntity::class, ArticleNoteEntity::class,
    ],
    version = 8,
    exportSchema = false,
)
abstract class ScannerDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun scanDao(): ScanDao
    abstract fun damageDao(): DamageDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun articleDao(): ArticleDao

    companion object {

        /** v1 → v2: damage log tables. Additive — existing scans are untouched. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `damage_reports` (
                        `id` TEXT NOT NULL, `scanId` TEXT NOT NULL,
                        `comment` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`scanId`) REFERENCES `scans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_damage_reports_scanId` ON `damage_reports` (`scanId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `damage_photos` (
                        `id` TEXT NOT NULL, `reportId` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`reportId`) REFERENCES `damage_reports`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_damage_photos_reportId` ON `damage_photos` (`reportId`)")
            }
        }

        /** v2 → v3: full label photo path on scans. Additive. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `scans` ADD COLUMN `labelPhotoPath` TEXT")
            }
        }

        /**
         * v3 → v4: report status, multi-note logs (damage + scans), scan photos.
         * Existing single comments migrate into damage_notes.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `damage_reports` ADD COLUMN `status` TEXT NOT NULL DEFAULT 'open'")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `damage_notes` (
                        `id` TEXT NOT NULL, `reportId` TEXT NOT NULL,
                        `kind` TEXT NOT NULL, `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`reportId`) REFERENCES `damage_reports`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_damage_notes_reportId` ON `damage_notes` (`reportId`)")
                db.execSQL(
                    """
                    INSERT INTO damage_notes (id, reportId, kind, text, createdAt)
                    SELECT id || '-legacy', id, 'note', comment, createdAt
                    FROM damage_reports WHERE comment != ''
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scan_notes` (
                        `id` TEXT NOT NULL, `scanId` TEXT NOT NULL,
                        `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`scanId`) REFERENCES `scans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_scan_notes_scanId` ON `scan_notes` (`scanId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scan_photos` (
                        `id` TEXT NOT NULL, `scanId` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`scanId`) REFERENCES `scans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_scan_photos_scanId` ON `scan_photos` (`scanId`)")
            }
        }

        /** v4 → v5: batch documents, summary toggle, original-quantity history. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `documents` ADD COLUMN `isBatch` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `documents` ADD COLUMN `showSummary` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `scans` ADD COLUMN `originalQuantity` TEXT")
            }
        }

        /** v5 → v6: append-only field-edit ledger. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `field_edits` (
                        `id` TEXT NOT NULL, `scanId` TEXT NOT NULL,
                        `field` TEXT NOT NULL, `oldValue` TEXT, `newValue` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`scanId`) REFERENCES `scans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_field_edits_scanId` ON `field_edits` (`scanId`)")
            }
        }

        /** v6 → v7: article number on scans. */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `scans` ADD COLUMN `articleNo` TEXT")
            }
        }

        /** v7 → v8: Tools tab tables (appointments + article registry). Additive. */
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `appointments` (
                        `id` TEXT NOT NULL, `title` TEXT NOT NULL,
                        `at` INTEGER NOT NULL, `location` TEXT,
                        `done` INTEGER NOT NULL DEFAULT 0, `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_appointments_at` ON `appointments` (`at`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `appointment_notes` (
                        `id` TEXT NOT NULL, `appointmentId` TEXT NOT NULL,
                        `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`appointmentId`) REFERENCES `appointments`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_appointment_notes_appointmentId` ON `appointment_notes` (`appointmentId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `articles` (
                        `id` TEXT NOT NULL, `articleNo` TEXT NOT NULL,
                        `name` TEXT, `gtin` TEXT, `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_articles_articleNo` ON `articles` (`articleNo`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `article_photos` (
                        `id` TEXT NOT NULL, `articleId` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`articleId`) REFERENCES `articles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_article_photos_articleId` ON `article_photos` (`articleId`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `article_notes` (
                        `id` TEXT NOT NULL, `articleId` TEXT NOT NULL,
                        `text` TEXT NOT NULL, `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`articleId`) REFERENCES `articles`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_article_notes_articleId` ON `article_notes` (`articleId`)")
            }
        }

        fun build(context: Context): ScannerDatabase =
            Room.databaseBuilder(context, ScannerDatabase::class.java, "sscc-scanner.db")
                .addMigrations(
                    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
                    MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8,
                )
                .fallbackToDestructiveMigration()
                .build()
    }
}
