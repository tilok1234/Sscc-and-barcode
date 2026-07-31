package com.ssccscanner.ui.tools

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssccscanner.SsccApp
import com.ssccscanner.data.AppointmentWithNotes
import com.ssccscanner.data.ArticlePhotoEntity
import com.ssccscanner.data.ArticleWithAttachments
import com.ssccscanner.scan.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduleViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as SsccApp).repository

    val appointments: StateFlow<List<AppointmentWithNotes>> =
        repository.appointments.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Not-done appointments from the start of today onward, for the hub card. */
    val upcomingCount: StateFlow<Int> =
        repository.upcomingAppointmentCount(startOfToday())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun add(title: String, at: Long, location: String?) {
        viewModelScope.launch { repository.addAppointment(title, at, location) }
    }

    fun update(id: String, title: String, at: Long, location: String?) {
        viewModelScope.launch { repository.updateAppointment(id, title, at, location) }
    }

    fun setDone(id: String, done: Boolean) {
        viewModelScope.launch { repository.setAppointmentDone(id, done) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteAppointment(id) }
    }

    fun addNote(appointmentId: String, text: String) {
        viewModelScope.launch { repository.addAppointmentNote(appointmentId, text) }
    }

    fun editNote(id: String, text: String) {
        viewModelScope.launch { repository.updateAppointmentNote(id, text) }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch { repository.deleteAppointmentNote(id) }
    }

    companion object {
        fun startOfToday(now: Long = System.currentTimeMillis()): Long {
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = now
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}

class ArticlesViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as SsccApp).repository

    val articles: StateFlow<List<ArticleWithAttachments>> =
        repository.articles.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val count: StateFlow<Int> =
        repository.articleCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun add(articleNo: String, name: String?, gtin: String?, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            val article = repository.addArticle(articleNo, name, gtin)
            onCreated(article.id)
        }
    }

    fun update(id: String, articleNo: String, name: String?, gtin: String?) {
        viewModelScope.launch { repository.updateArticle(id, articleNo, name, gtin) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.deleteArticle(id) }
    }

    fun addPhoto(articleId: String, uri: Uri, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val jpeg = ImageUtils.evidenceJpeg(getApplication(), uri)
            if (jpeg == null) {
                onDone(false)
            } else {
                repository.addArticlePhoto(articleId, jpeg)
                onDone(true)
            }
        }
    }

    fun deletePhoto(photo: ArticlePhotoEntity) {
        viewModelScope.launch { repository.deleteArticlePhoto(photo) }
    }

    fun addNote(articleId: String, text: String) {
        viewModelScope.launch { repository.addArticleNote(articleId, text) }
    }

    fun editNote(id: String, text: String) {
        viewModelScope.launch { repository.updateArticleNote(id, text) }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch { repository.deleteArticleNote(id) }
    }
}
