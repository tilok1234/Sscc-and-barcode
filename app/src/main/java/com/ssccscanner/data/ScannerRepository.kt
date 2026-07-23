package com.ssccscanner.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ssccscanner.core.Confidence
import com.ssccscanner.core.ScanFields
import com.ssccscanner.core.ScanSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.prefs by preferencesDataStore(name = "sscc-scanner-prefs")

class ScannerRepository(private val context: Context, private val db: ScannerDatabase) {

    private val documentDao = db.documentDao()
    private val scanDao = db.scanDao()

    private val activeDocKey = stringPreferencesKey("active_document_id")
    private val batchModeKey = booleanPreferencesKey("batch_mode")

    // --- Documents ---

    val documents: Flow<List<DocumentSummary>> = documentDao.documentSummaries()

    val activeDocumentId: Flow<String?> = context.prefs.data.map { it[activeDocKey] }

    /** Resolved name of the active filing target (falls back to oldest document). */
    val activeDocument: Flow<DocumentSummary?> =
        combine(documents, activeDocumentId) { docs, activeId ->
            docs.firstOrNull { it.id == activeId } ?: docs.minByOrNull { it.createdAt }
        }

    suspend fun ensureDefaultDocument(): DocumentEntity {
        documentDao.oldest()?.let { return it }
        val doc = DocumentEntity(id = newId(), name = "First document", createdAt = System.currentTimeMillis())
        documentDao.upsert(doc)
        setActiveDocument(doc.id)
        return doc
    }

    suspend fun createDocument(name: String): DocumentEntity {
        val doc = DocumentEntity(id = newId(), name = name.trim(), createdAt = System.currentTimeMillis())
        documentDao.upsert(doc)
        setActiveDocument(doc.id)
        return doc
    }

    suspend fun renameDocument(id: String, name: String) = documentDao.rename(id, name.trim())

    suspend fun deleteDocument(id: String) {
        // Cascade removes the scan rows; their label photo files need explicit cleanup.
        scanDao.scansForDocumentOnce(id).forEach { scan ->
            scan.labelPhotoPath?.let { runCatching { java.io.File(it).delete() } }
        }
        documentDao.delete(id) // scans cascade
        val fallback = documentDao.oldest()
        context.prefs.edit { prefs ->
            if (prefs[activeDocKey] == id) {
                if (fallback != null) prefs[activeDocKey] = fallback.id else prefs.remove(activeDocKey)
            }
        }
    }

    suspend fun setActiveDocument(id: String) {
        context.prefs.edit { it[activeDocKey] = id }
    }

    // --- Scans ---

    fun scansFor(documentId: String): Flow<List<ScanEntity>> = scanDao.scansForDocument(documentId)

    suspend fun scansForDocumentOnce(documentId: String): List<ScanEntity> =
        scanDao.scansForDocumentOnce(documentId)

    val totalScanCount: Flow<Int> = scanDao.totalCount()

    suspend fun scan(id: String): ScanEntity? = scanDao.byId(id)

    suspend fun latestThumbnail(documentId: String): ByteArray? = scanDao.latestThumbnail(documentId)

    private fun labelPhotoDir(): java.io.File =
        java.io.File(context.filesDir, "label_photos").apply { mkdirs() }

    /** Files a completed scan into the active document; returns the stored entity. */
    suspend fun addScan(
        fields: ScanFields,
        thumbnail: ByteArray?,
        labelJpeg: ByteArray?,
        timestamp: Long,
    ): ScanEntity {
        val active = ensureActiveDocumentId()
        val id = newId()
        val photoPath = labelJpeg?.let { bytes ->
            val f = java.io.File(labelPhotoDir(), "$id.jpg")
            f.writeBytes(bytes)
            f.absolutePath
        }
        val entity = ScanEntity(
            id = id,
            documentId = active,
            sscc = fields.sscc,
            batchNo = fields.batchNo,
            gtin = fields.gtin,
            bestBefore = fields.bestBefore,
            quantity = fields.quantity,
            confidence = fields.confidence.name.lowercase(),
            source = fields.source.name.lowercase(),
            edited = false,
            timestamp = timestamp,
            thumbnail = thumbnail,
            labelPhotoPath = photoPath,
        )
        scanDao.upsert(entity)
        return entity
    }

    suspend fun updateScanFields(id: String, fields: ScanFields) {
        val existing = scanDao.byId(id) ?: return
        scanDao.upsert(
            existing.copy(
                sscc = fields.sscc,
                batchNo = fields.batchNo,
                gtin = fields.gtin,
                bestBefore = fields.bestBefore,
                quantity = fields.quantity,
                edited = true,
            ),
        )
    }

    suspend fun deleteScan(id: String) {
        scanDao.byId(id)?.labelPhotoPath?.let { runCatching { java.io.File(it).delete() } }
        scanDao.delete(id)
    }

    private suspend fun ensureActiveDocumentId(): String {
        val current = context.prefs.data.first()[activeDocKey]
        // Validate the stored id still exists; fall back to (or create) a default.
        if (current != null && documentDao.byId(current) != null) return current
        return ensureDefaultDocument().id
    }

    // --- Batch mode ---

    val batchMode: Flow<Boolean> = context.prefs.data.map { it[batchModeKey] ?: false }

    suspend fun setBatchMode(on: Boolean) {
        context.prefs.edit { it[batchModeKey] = on }
    }

    // --- Damage log ---

    private val damageDao = db.damageDao()

    private fun photoDir(): java.io.File =
        java.io.File(context.filesDir, "damage_photos").apply { mkdirs() }

    val damageReports: Flow<List<DamageReportWithScan>> = damageDao.reportsWithScans()

    val damageCount: Flow<Int> = damageDao.count()

    val damagedScanIds: Flow<List<String>> = damageDao.damagedScanIds()

    suspend fun damagedScanIdsOnce(): Set<String> = damageDao.damagedScanIdsOnce().toSet()

    suspend fun damageReport(id: String): DamageReportWithScan? = damageDao.reportWithScan(id)

    /** Creates a report for the scan if none exists yet; returns the report id. */
    suspend fun ensureDamageReport(scanId: String): String {
        damageDao.reportForScan(scanId)?.let { return it.id }
        val report = DamageReportEntity(
            id = newId(),
            scanId = scanId,
            comment = "",
            createdAt = System.currentTimeMillis(),
        )
        damageDao.insertReport(report)
        // IGNORE conflict strategy: if a concurrent insert won, read it back.
        return damageDao.reportForScan(scanId)?.id ?: report.id
    }

    suspend fun setDamageComment(reportId: String, comment: String) =
        damageDao.setComment(reportId, comment.trim())

    /** Persist a photo of the damaged goods as a JPEG file + DB row. */
    suspend fun addDamagePhoto(reportId: String, jpegBytes: ByteArray): DamagePhotoEntity {
        val photo = DamagePhotoEntity(
            id = newId(),
            reportId = reportId,
            filePath = java.io.File(photoDir(), "${newId()}.jpg").absolutePath,
            createdAt = System.currentTimeMillis(),
        )
        java.io.File(photo.filePath).writeBytes(jpegBytes)
        damageDao.insertPhoto(photo)
        return photo
    }

    suspend fun deleteDamagePhoto(photo: DamagePhotoEntity) {
        damageDao.deletePhoto(photo.id)
        runCatching { java.io.File(photo.filePath).delete() }
    }

    suspend fun deleteDamageReport(reportId: String) {
        val photos = damageDao.photosFor(reportId)
        damageDao.deleteReport(reportId) // photo rows cascade
        photos.forEach { runCatching { java.io.File(it.filePath).delete() } }
    }

    // --- Retention: auto-compress / auto-delete old entries (0 = off) ---

    private val compressAfterDaysKey = androidx.datastore.preferences.core.intPreferencesKey("compress_after_days")
    private val deleteAfterDaysKey = androidx.datastore.preferences.core.intPreferencesKey("delete_after_days")

    val compressAfterDays: Flow<Int> = context.prefs.data.map { it[compressAfterDaysKey] ?: 0 }
    val deleteAfterDays: Flow<Int> = context.prefs.data.map { it[deleteAfterDaysKey] ?: 0 }

    suspend fun setCompressAfterDays(days: Int) {
        context.prefs.edit { it[compressAfterDaysKey] = days }
    }

    suspend fun setDeleteAfterDays(days: Int) {
        context.prefs.edit { it[deleteAfterDaysKey] = days }
    }

    /**
     * Applies the retention settings. Compression drops the stored label photo
     * (data + thumbnail stay); deletion removes whole scans — but never scans
     * that have a damage report attached. Runs at app start.
     */
    suspend fun runRetentionCleanup() {
        val prefs = context.prefs.data.first()
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        val compressDays = prefs[compressAfterDaysKey] ?: 0
        if (compressDays > 0) {
            scanDao.scansWithPhotosOlderThan(now - compressDays * dayMs).forEach { scan ->
                scan.labelPhotoPath?.let { runCatching { java.io.File(it).delete() } }
                scanDao.clearLabelPhoto(scan.id)
            }
        }

        val deleteDays = prefs[deleteAfterDaysKey] ?: 0
        if (deleteDays > 0) {
            scanDao.undamagedScansOlderThan(now - deleteDays * dayMs).forEach { scan ->
                scan.labelPhotoPath?.let { runCatching { java.io.File(it).delete() } }
                scanDao.delete(scan.id)
            }
        }
    }

    /** Total bytes of stored label + damage photos, for the settings sheet. */
    fun photoStorageBytes(): Long {
        var total = 0L
        for (dir in listOf(labelPhotoDir(), photoDir())) {
            dir.listFiles()?.forEach { total += it.length() }
        }
        return total
    }

    // --- Scan mode (barcode = live auto-scan, label = shutter-driven full-label read) ---

    private val scanModeKey = stringPreferencesKey("scan_mode")

    val scanMode: Flow<String> = context.prefs.data.map { it[scanModeKey] ?: "barcode" }

    suspend fun setScanMode(mode: String) {
        context.prefs.edit { it[scanModeKey] = mode }
    }

    private fun newId(): String = UUID.randomUUID().toString()

    companion object {
        fun confidenceOf(scan: ScanEntity): Confidence =
            runCatching { Confidence.valueOf(scan.confidence.uppercase()) }.getOrDefault(Confidence.LOW)

        fun sourceOf(scan: ScanEntity): ScanSource =
            runCatching { ScanSource.valueOf(scan.source.uppercase()) }.getOrDefault(ScanSource.OCR)

        fun fieldsOf(scan: ScanEntity): ScanFields = ScanFields(
            sscc = scan.sscc,
            batchNo = scan.batchNo,
            gtin = scan.gtin,
            bestBefore = scan.bestBefore,
            quantity = scan.quantity,
            confidence = confidenceOf(scan),
            source = sourceOf(scan),
        )
    }
}
