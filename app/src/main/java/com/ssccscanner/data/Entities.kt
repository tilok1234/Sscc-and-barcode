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
    // Small JPEG only — never the full photo (handoff's storage rule)
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val thumbnail: ByteArray?,
) {
    override fun equals(other: Any?): Boolean = other is ScanEntity && other.id == id
    override fun hashCode(): Int = id.hashCode()
}

/** Row shape for the Library list: document + aggregate scan info. */
data class DocumentSummary(
    val id: String,
    val name: String,
    val createdAt: Long,
    val scanCount: Int,
    val lastScanAt: Long?,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val lastThumbnail: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean =
        other is DocumentSummary && other.id == id && other.name == name &&
            other.scanCount == scanCount && other.lastScanAt == lastScanAt
    override fun hashCode(): Int = id.hashCode()
}
