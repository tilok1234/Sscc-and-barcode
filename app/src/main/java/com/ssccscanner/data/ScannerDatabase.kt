package com.ssccscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DocumentEntity::class, ScanEntity::class, DamageReportEntity::class, DamagePhotoEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class ScannerDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun scanDao(): ScanDao
    abstract fun damageDao(): DamageDao

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

        fun build(context: Context): ScannerDatabase =
            Room.databaseBuilder(context, ScannerDatabase::class.java, "sscc-scanner.db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
    }
}
