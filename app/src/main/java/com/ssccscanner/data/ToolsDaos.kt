package com.ssccscanner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {

    @Transaction
    @Query("SELECT * FROM appointments ORDER BY at ASC")
    fun appointmentsWithNotes(): Flow<List<AppointmentWithNotes>>

    @Query("SELECT * FROM appointments WHERE id = :id")
    suspend fun byId(id: String): AppointmentEntity?

    @Query("SELECT COUNT(*) FROM appointments WHERE done = 0 AND at >= :from")
    fun upcomingCount(from: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(appointment: AppointmentEntity)

    @Query("UPDATE appointments SET done = :done WHERE id = :id")
    suspend fun setDone(id: String, done: Boolean)

    @Query("DELETE FROM appointments WHERE id = :id")
    suspend fun delete(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: AppointmentNoteEntity)

    @Query("UPDATE appointment_notes SET text = :text WHERE id = :id")
    suspend fun updateNoteText(id: String, text: String)

    @Query("DELETE FROM appointment_notes WHERE id = :id")
    suspend fun deleteNote(id: String)
}

@Dao
interface ArticleDao {

    @Transaction
    @Query("SELECT * FROM articles ORDER BY articleNo ASC")
    fun articlesWithAttachments(): Flow<List<ArticleWithAttachments>>

    @Query("SELECT * FROM articles WHERE id = :id")
    suspend fun byId(id: String): ArticleEntity?

    @Query("SELECT COUNT(*) FROM articles")
    fun count(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(article: ArticleEntity)

    @Query("DELETE FROM articles WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM article_photos WHERE articleId = :articleId ORDER BY createdAt ASC")
    suspend fun photosFor(articleId: String): List<ArticlePhotoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhoto(photo: ArticlePhotoEntity)

    @Query("DELETE FROM article_photos WHERE id = :id")
    suspend fun deletePhoto(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: ArticleNoteEntity)

    @Query("UPDATE article_notes SET text = :text WHERE id = :id")
    suspend fun updateNoteText(id: String, text: String)

    @Query("DELETE FROM article_notes WHERE id = :id")
    suspend fun deleteNote(id: String)
}
