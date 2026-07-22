package com.ssccscanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DocumentEntity::class, ScanEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ScannerDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun scanDao(): ScanDao

    companion object {
        fun build(context: Context): ScannerDatabase =
            Room.databaseBuilder(context, ScannerDatabase::class.java, "sscc-scanner.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
