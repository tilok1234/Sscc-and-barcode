package com.ssccscanner.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    // Created via batch mode — gets a BATCH chip and an always-on summary
    @ColumnInfo(defaultValue = "0") val isBatch: Boolean = false,
    // Batch summary opt-in for normal documents
    @ColumnInfo(defaultValue = "0") val showSummary: Boolean = false,
)

@Entity(
    tableName = "scans",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("documentId"), Index("timestamp")],
)
data class ScanEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val sscc: String?,
    val batchNo: String?,
    val gtin: String?,
    val bestBefore: String?,
    val quantity: String?,
    val confidence: String,          // high | medium | low
    val source: String,              // barcode | ocr | mixed | manual
    val edited: Boolean,
    val timestamp: Long,
    // Small JPEG for list rows; the full label photo lives on disk (below)
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val thumbnail: ByteArray?,
    // Viewing-quality JPEG of the scanned label in app-private storage
    val labelPhotoPath: String? = null,
    // Set once, on the first quantity edit — preserves the as-scanned count
    val originalQuantity: String? = null,
) {
    override fun equals(other: Any?): Boolean = other is ScanEntity && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/** A timestamped free-text note on a regular scan (Library). */
@Entity(
    tableName = "scan_notes",
    foreignKeys = [
        ForeignKey(
            entity = ScanEntity::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("scanId")],
)
data class ScanNoteEntity(
    @PrimaryKey val id: String,
    val scanId: String,
    val text: String,
    val createdAt: Long,
)

/** An extra photo attached to a regular scan (Library). */
@Entity(
    tableName = "scan_photos",
    foreignKeys = [
        ForeignKey(
            entity = ScanEntity::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("scanId")],
)
data class ScanPhotoEntity(
    @PrimaryKey val id: String,
    val scanId: String,
    val filePath: String,
    val createdAt: Long,
)

/**
 * Append-only ledger of manual field edits on a scan. Rows are written by the
 * repository on every change and are deliberately never editable or deletable
 * from the UI — the as-scanned truth stays on record.
 */
@Entity(
    tableName = "field_edits",
    foreignKeys = [
        ForeignKey(
            entity = ScanEntity::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("scanId")],
)
data class FieldEditEntity(
    @PrimaryKey val id: String,
    val scanId: String,
    val field: String,      // sscc | batchNo | gtin | bestBefore | quantity
    val oldValue: String?,
    val newValue: String?,
    val createdAt: Long,
)

/** Row shape for the Library list: document + aggregate scan info. */
data class DocumentSummary(
    val id: String,
    val name: String,
    val createdAt: Long,
    val isBatch: Boolean,
    val showSummary: Boolean,
    val scanCount: Int,
    val lastScanAt: Long?,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val lastThumbnail: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is DocumentSummary && other.id == id && other.name == name &&
            other.scanCount == scanCount && other.lastScanAt == lastScanAt &&
            other.isBatch == isBatch && other.showSummary == showSummary
    override fun hashCode(): Int = id.hashCode()
}
