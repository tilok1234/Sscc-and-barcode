package com.ssccscanner.ui.damage

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssccscanner.SsccApp
import com.ssccscanner.data.DamagePhotoEntity
import com.ssccscanner.data.DamageReportWithScan
import com.ssccscanner.scan.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DamageViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as SsccApp).repository

    val reports: StateFlow<List<DamageReportWithScan>> =
        repository.damageReports.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val count: StateFlow<Int> =
        repository.damageCount.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Scan ids that already have a damage report (drives "View damage report" states). */
    val damagedScanIds: StateFlow<List<String>> =
        repository.damagedScanIds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Cross-tab navigation request: set when "Report damage" is tapped on a
     * scan; AppRoot switches to the Damage tab and DamageScreen opens the
     * report, then clears it.
     */
    val openRequest = MutableStateFlow<String?>(null)

    fun consumeOpenRequest() {
        openRequest.value = null
    }

    /** Create (or find) the report for a scan and request navigation to it. */
    fun reportDamage(scanId: String) {
        viewModelScope.launch {
            val reportId = repository.ensureDamageReport(scanId)
            openRequest.value = reportId
        }
    }

    fun addNote(reportId: String, text: String, kind: String = "note") {
        viewModelScope.launch { repository.addDamageNote(reportId, text, kind) }
    }

    fun deleteNote(id: String) {
        viewModelScope.launch { repository.deleteDamageNote(id) }
    }

    fun setStatus(reportId: String, status: String) {
        viewModelScope.launch { repository.setDamageStatus(reportId, status) }
    }

    fun addPhoto(reportId: String, uri: Uri, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val jpeg = ImageUtils.evidenceJpeg(getApplication(), uri)
            if (jpeg == null) {
                onDone(false)
            } else {
                repository.addDamagePhoto(reportId, jpeg)
                onDone(true)
            }
        }
    }

    fun deletePhoto(photo: DamagePhotoEntity) {
        viewModelScope.launch { repository.deleteDamagePhoto(photo) }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch { repository.deleteDamageReport(reportId) }
    }
}
