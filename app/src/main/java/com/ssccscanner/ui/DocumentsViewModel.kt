package com.ssccscanner.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssccscanner.SsccApp
import com.ssccscanner.core.ScanFields
import com.ssccscanner.data.CsvExport
import com.ssccscanner.data.DocumentSummary
import com.ssccscanner.data.ScanEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Shared document/scan state for both tabs: the Library screens and the Scan
 * tab's filing pill + document picker.
 */
class DocumentsViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as SsccApp).repository

    val documents: StateFlow<List<DocumentSummary>> =
        repository.documents.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDocumentId: StateFlow<String?> =
        repository.activeDocumentId.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalScanCount: StateFlow<Int> =
        repository.totalScanCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun scansFor(documentId: String): Flow<List<ScanEntity>> = repository.scansFor(documentId)

    fun createDocument(name: String, isBatch: Boolean = false) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.createDocument(name, isBatch) }
    }

    fun setDocumentSummary(id: String, on: Boolean) {
        viewModelScope.launch { repository.setDocumentSummary(id, on) }
    }

    fun setActiveDocument(id: String) {
        viewModelScope.launch { repository.setActiveDocument(id) }
    }

    fun renameDocument(id: String, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repository.renameDocument(id, name) }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch { repository.deleteDocument(id) }
    }

    fun deleteScan(id: String) {
        viewModelScope.launch { repository.deleteScan(id) }
    }

    fun updateScan(id: String, fields: ScanFields) {
        viewModelScope.launch { repository.updateScanFields(id, fields) }
    }

    // --- Notes & photos on scans ---

    fun scanNotes(scanId: String) = repository.scanNotes(scanId)

    /** Append-only edit ledger for a scan (read-only in the UI by design). */
    fun fieldEdits(scanId: String) = repository.fieldEdits(scanId)

    fun addScanNote(scanId: String, text: String) {
        viewModelScope.launch { repository.addScanNote(scanId, text) }
    }

    fun deleteScanNote(id: String) {
        viewModelScope.launch { repository.deleteScanNote(id) }
    }

    fun editScanNote(id: String, text: String) {
        viewModelScope.launch { repository.updateScanNote(id, text) }
    }

    fun scanPhotos(scanId: String) = repository.scanPhotos(scanId)

    fun addScanPhoto(scanId: String, uri: android.net.Uri, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val jpeg = com.ssccscanner.scan.ImageUtils.evidenceJpeg(getApplication(), uri)
            if (jpeg == null) {
                onDone(false)
            } else {
                repository.addScanPhoto(scanId, jpeg)
                onDone(true)
            }
        }
    }

    fun deleteScanPhoto(photo: com.ssccscanner.data.ScanPhotoEntity) {
        viewModelScope.launch { repository.deleteScanPhoto(photo) }
    }

    // --- Retention settings ---

    val compressAfterDays: StateFlow<Int> =
        repository.compressAfterDays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val deleteAfterDays: StateFlow<Int> =
        repository.deleteAfterDays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _photoStorageBytes = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val photoStorageBytes: StateFlow<Long> = _photoStorageBytes

    fun refreshStorageUsage() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _photoStorageBytes.value = repository.photoStorageBytes()
        }
    }

    fun setCompressAfterDays(days: Int) {
        viewModelScope.launch {
            repository.setCompressAfterDays(days)
            repository.runRetentionCleanup()
            refreshStorageUsage()
        }
    }

    fun setDeleteAfterDays(days: Int) {
        viewModelScope.launch {
            repository.setDeleteAfterDays(days)
            repository.runRetentionCleanup()
            refreshStorageUsage()
        }
    }

    /** Builds the CSV in the background, then fires the share sheet. */
    fun exportCsv(context: Context, document: DocumentSummary) {
        viewModelScope.launch {
            val scans = repository.scansForDocumentOnce(document.id)
            if (scans.isEmpty()) return@launch
            val damagedIds = repository.damagedScanIdsOnce()
            val intent = CsvExport.shareIntent(context, document.name, scans, damagedIds)
            context.startActivity(Intent.createChooser(intent, "Export CSV"))
        }
    }
}
