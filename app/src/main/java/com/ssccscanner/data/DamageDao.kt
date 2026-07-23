package com.ssccscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DamageDao {

    @Transaction
    @Query("SELECT * FROM damage_reports ORDER BY createdAt DESC")
    fun reportsWithScans(): Flow<List<DamageReportWithScan>>

    @Transaction
    @Query("SELECT * FROM damage_reports WHERE id = :id")
    suspend fun reportWithScan(id: String): DamageReportWithScan?

    @Query("SELECT * FROM damage_reports WHERE scanId = :scanId")
    suspend fun reportForScan(scanId: String): DamageReportEntity?

    @Query("SELECT COUNT(*) FROM damage_reports")
    fun count(): Flow<Int>

    @Query("SELECT scanId FROM damage_reports")
    fun damagedScanIds(): Flow<List<String>>

    @Query("SELECT scanId FROM damage_reports")
    suspend fun damagedScanIdsOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReport(report: DamageReportEntity)

    @Query("UPDATE damage_reports SET comment = :comment WHERE id = :id")
    suspend fun setComment(id: String, comment: String)

    @Query("DELETE FROM damage_reports WHERE id = :id")
    suspend fun deleteReport(id: String)

    @Query("UPDATE damage_reports SET status = :status WHERE id = :id")
    suspend fun setStatus(id: String, status: String)

    @Query("SELECT * FROM damage_photos WHERE reportId = :reportId ORDER BY createdAt ASC")
    suspend fun photosFor(reportId: String): List<DamagePhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: DamagePhotoEntity)

    @Query("DELETE FROM damage_photos WHERE id = :id")
    suspend fun deletePhoto(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: DamageNoteEntity)

    @Query("DELETE FROM damage_notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("UPDATE damage_notes SET text = :text WHERE id = :id")
    suspend fun updateNoteText(id: String, text: String)
}
