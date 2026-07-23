package com.ssccscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query(
        """
        SELECT d.id, d.name, d.createdAt, d.isBatch, d.showSummary,
               COUNT(s.id) AS scanCount,
               MAX(s.timestamp) AS lastScanAt,
               (SELECT s2.thumbnail FROM scans s2
                WHERE s2.documentId = d.id AND s2.thumbnail IS NOT NULL
                ORDER BY s2.timestamp DESC LIMIT 1) AS lastThumbnail
        FROM documents d
        LEFT JOIN scans s ON s.documentId = d.id
        GROUP BY d.id
        ORDER BY d.createdAt DESC
        """,
    )
    fun documentSummaries(): Flow<List<DocumentSummary>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun byId(id: String): DocumentEntity?

    @Query("SELECT * FROM documents ORDER BY createdAt ASC LIMIT 1")
    suspend fun oldest(): DocumentEntity?

    @Query("SELECT COUNT(*) FROM documents")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: DocumentEntity)

    @Query("UPDATE documents SET name = :name WHERE id = :id")
    suspend fun rename(id: String, name: String)

    @Query("UPDATE documents SET showSummary = :on WHERE id = :id")
    suspend fun setShowSummary(id: String, on: Boolean)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface ScanDao {

    @Query("SELECT * FROM scans WHERE documentId = :documentId ORDER BY timestamp DESC")
    fun scansForDocument(documentId: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE documentId = :documentId ORDER BY timestamp DESC")
    suspend fun scansForDocumentOnce(documentId: String): List<ScanEntity>

    @Query("SELECT COUNT(*) FROM scans")
    fun totalCount(): Flow<Int>

    @Query("SELECT * FROM scans WHERE id = :id")
    suspend fun byId(id: String): ScanEntity?

    @Query(
        "SELECT thumbnail FROM scans WHERE documentId = :documentId AND thumbnail IS NOT NULL ORDER BY timestamp DESC LIMIT 1",
    )
    suspend fun latestThumbnail(documentId: String): ByteArray?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(scan: ScanEntity)

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun delete(id: String)

    // --- Notes & extra photos on scans ---

    @Query("SELECT * FROM scan_notes WHERE scanId = :scanId ORDER BY createdAt ASC")
    fun notesForScan(scanId: String): Flow<List<ScanNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: ScanNoteEntity)

    @Query("DELETE FROM scan_notes WHERE id = :id")
    suspend fun deleteNote(id: String)

    @Query("UPDATE scan_notes SET text = :text WHERE id = :id")
    suspend fun updateNoteText(id: String, text: String)

    @Query("SELECT * FROM scan_photos WHERE scanId = :scanId ORDER BY createdAt ASC")
    fun photosForScan(scanId: String): Flow<List<ScanPhotoEntity>>

    @Query("SELECT * FROM scan_photos WHERE scanId = :scanId")
    suspend fun photosForScanOnce(scanId: String): List<ScanPhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhoto(photo: ScanPhotoEntity)

    @Query("DELETE FROM scan_photos WHERE id = :id")
    suspend fun deletePhoto(id: String)

    @Query(
        "SELECT sp.filePath FROM scan_photos sp INNER JOIN scans s ON sp.scanId = s.id WHERE s.documentId = :documentId",
    )
    suspend fun photoPathsForDocument(documentId: String): List<String>

    // --- Retention cleanup ---

    @Query("SELECT * FROM scans WHERE labelPhotoPath IS NOT NULL AND timestamp < :cutoff")
    suspend fun scansWithPhotosOlderThan(cutoff: Long): List<ScanEntity>

    @Query("UPDATE scans SET labelPhotoPath = NULL WHERE id = :id")
    suspend fun clearLabelPhoto(id: String)

    @Query("SELECT * FROM scans WHERE timestamp < :cutoff AND id NOT IN (SELECT scanId FROM damage_reports)")
    suspend fun undamagedScansOlderThan(cutoff: Long): List<ScanEntity>
}
