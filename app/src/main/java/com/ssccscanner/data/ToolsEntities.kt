package com.ssccscanner.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * Entities for the Tools tab — the experimental toolbox (schedule, article
 * registry, and whatever comes next). Each tool keeps its own tables; nothing
 * here touches the scan/damage records.
 */

/** A planned appointment/meeting with its own timestamped note log. */
@Entity(tableName = "appointments", indices = [Index("at")])
data class AppointmentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val at: Long,                    // when the appointment happens
    val location: String?,
    @ColumnInfo(defaultValue = "0") val done: Boolean = false,
    val createdAt: Long,
)

/** A timestamped free-text note on an appointment. */
@Entity(
    tableName = "appointment_notes",
    foreignKeys = [
        ForeignKey(
            entity = AppointmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["appointmentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("appointmentId")],
)
data class AppointmentNoteEntity(
    @PrimaryKey val id: String,
    val appointmentId: String,
    val text: String,
    val createdAt: Long,
)

data class AppointmentWithNotes(
    @Embedded val appointment: AppointmentEntity,
    @Relation(parentColumn = "id", entityColumn = "appointmentId")
    val notes: List<AppointmentNoteEntity>,
)

/**
 * A registered article: the company article number plus whatever identifies
 * it — name, GTIN, label photos, notes. Reference data, freely editable.
 */
@Entity(tableName = "articles", indices = [Index("articleNo")])
data class ArticleEntity(
    @PrimaryKey val id: String,
    val articleNo: String,
    val name: String?,
    val gtin: String?,
    val createdAt: Long,
)

/** A photo (label, product, shelf tag…) attached to an article. */
@Entity(
    tableName = "article_photos",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("articleId")],
)
data class ArticlePhotoEntity(
    @PrimaryKey val id: String,
    val articleId: String,
    val filePath: String,
    val createdAt: Long,
)

/** A timestamped free-text note on an article. */
@Entity(
    tableName = "article_notes",
    foreignKeys = [
        ForeignKey(
            entity = ArticleEntity::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("articleId")],
)
data class ArticleNoteEntity(
    @PrimaryKey val id: String,
    val articleId: String,
    val text: String,
    val createdAt: Long,
)

data class ArticleWithAttachments(
    @Embedded val article: ArticleEntity,
    @Relation(parentColumn = "id", entityColumn = "articleId")
    val photos: List<ArticlePhotoEntity>,
    @Relation(parentColumn = "id", entityColumn = "articleId")
    val notes: List<ArticleNoteEntity>,
)
