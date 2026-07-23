package com.ssccscanner.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * One damage report per scanned pallet label. Deleting the underlying scan
 * cascades here; deleting the report never touches the scan.
 *
 * [comment] is legacy (v2/v3) — content was migrated into damage_notes.
 */
@Entity(
    tableName = "damage_reports",
    foreignKeys = [
        ForeignKey(
            entity = ScanEntity::class,
            parentColumns = ["id"],
            childColumns = ["scanId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["scanId"], unique = true)],
)
data class DamageReportEntity(
    @PrimaryKey val id: String,
    val scanId: String,
    val comment: String,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "open") val status: String = STATUS_OPEN,
) {
    companion object {
        const val STATUS_OPEN = "open"
        const val STATUS_RESTORED = "restored"
        const val STATUS_SANITIZED = "sanitized"
    }
}

/**
 * A timestamped note on a damage report. [kind] is "note" for regular
 * entries, or "restored"/"sanitized" for the note attached when the report's
 * status was changed.
 */
@Entity(
    tableName = "damage_notes",
    foreignKeys = [
        ForeignKey(
            entity = DamageReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("reportId")],
)
data class DamageNoteEntity(
    @PrimaryKey val id: String,
    val reportId: String,
    val kind: String,
    val text: String,
    val createdAt: Long,
)

/** A photo of the damaged goods, stored as a JPEG file in app-private storage. */
@Entity(
    tableName = "damage_photos",
    foreignKeys = [
        ForeignKey(
            entity = DamageReportEntity::class,
            parentColumns = ["id"],
            childColumns = ["reportId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("reportId")],
)
data class DamagePhotoEntity(
    @PrimaryKey val id: String,
    val reportId: String,
    val filePath: String,
    val createdAt: Long,
)

/** Damage log row: the report joined with its label scan, photos and notes. */
data class DamageReportWithScan(
    @Embedded val report: DamageReportEntity,
    @Relation(parentColumn = "scanId", entityColumn = "id")
    val scan: ScanEntity,
    @Relation(parentColumn = "id", entityColumn = "reportId")
    val photos: List<DamagePhotoEntity>,
    @Relation(parentColumn = "id", entityColumn = "reportId")
    val notes: List<DamageNoteEntity>,
)
