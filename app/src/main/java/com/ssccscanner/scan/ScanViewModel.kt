package com.ssccscanner.scan

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ssccscanner.SsccApp
import com.ssccscanner.core.BarcodeInterpreter
import com.ssccscanner.core.FieldMerger
import com.ssccscanner.core.ScanFields
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One completed scan as shown on the result screen. */
data class PendingScan(
    val scanId: String?,           // Room id once persisted
    val fields: ScanFields,
    val thumbnail: ByteArray?,
    val labelPhoto: ByteArray?,    // viewing-quality label image (also persisted to disk)
    val timestamp: Long,
)

/**
 * BARCODE: live viewfinder auto-decodes symbols the moment they're visible.
 * LABEL: live decoding is off — the user frames the whole label and presses
 * the shutter; the photo goes through barcode + OCR together. Prevents a
 * stray barcode from firing the scanner while aiming at the full label.
 */
enum class ScanMode { BARCODE, LABEL }

sealed interface ScanFlow {
    data object Ready : ScanFlow
    data object Processing : ScanFlow
    data class Result(val scan: PendingScan) : ScanFlow
    data class Error(val message: String) : ScanFlow
}

data class ScanUiState(
    val flow: ScanFlow = ScanFlow.Ready,
    val scanMode: ScanMode = ScanMode.BARCODE,
    val batchMode: Boolean = false,
    val batchCount: Int = 0,
    val activeDocumentName: String = "…",
)

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as SsccApp).repository

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state

    // --- Live-scan aggregation: GS1 labels usually carry several barcodes
    // (SSCC in one symbol; GTIN/date/batch in another). Collect symbols across
    // frames for a short window so one "scan" captures the whole label.
    private var aggregate: ScanFields? = null
    private var aggregateStartedAt = 0L
    private var lastDecodedFrame: Bitmap? = null

    private val errorMessage =
        "Couldn't find an SSCC or batch number on this label. Try getting closer and keeping the text in focus."

    init {
        viewModelScope.launch {
            repository.ensureDefaultDocument()
        }
        viewModelScope.launch {
            repository.batchMode.collect { on ->
                _state.update { if (it.batchMode == on) it else it.copy(batchMode = on) }
            }
        }
        viewModelScope.launch {
            repository.activeDocument.collect { doc ->
                _state.update { it.copy(activeDocumentName = doc?.name ?: "…") }
            }
        }
        viewModelScope.launch {
            repository.scanMode.collect { stored ->
                val mode = if (stored == "label") ScanMode.LABEL else ScanMode.BARCODE
                _state.update { if (it.scanMode == mode) it else it.copy(scanMode = mode) }
            }
        }
    }

    fun setScanMode(mode: ScanMode) {
        resetAggregation()
        _state.update { it.copy(scanMode = mode) }
        viewModelScope.launch { repository.setScanMode(if (mode == ScanMode.LABEL) "label" else "barcode") }
    }

    fun onBarcodesDetected(barcodes: List<DetectedBarcode>, frameBitmap: Bitmap?) {
        if (_state.value.flow != ScanFlow.Ready) return
        if (_state.value.scanMode != ScanMode.BARCODE) return

        val decoded = barcodes.mapNotNull { BarcodeInterpreter.interpret(it.rawValue, it.formatName) }
        if (decoded.isNotEmpty()) {
            val frame = FieldMerger.combineBarcodes(decoded)
            aggregate = FieldMerger.combineBarcodes(listOfNotNull(aggregate, frame))
            if (aggregateStartedAt == 0L) aggregateStartedAt = System.currentTimeMillis()
            if (frameBitmap != null) lastDecodedFrame = frameBitmap
        }

        val agg = aggregate ?: return
        val complete = agg.sscc != null && agg.gtin != null && agg.batchNo != null
        val windowElapsed =
            aggregateStartedAt != 0L && System.currentTimeMillis() - aggregateStartedAt > AGGREGATION_WINDOW_MS
        if (complete || (windowElapsed && !agg.isEmpty)) {
            finalizeScan(agg, sourceBitmap = lastDecodedFrame)
        }
    }

    /** A photo captured in-app or picked from the gallery → barcode + OCR merge. */
    fun processStillImage(uri: Uri) {
        _state.update { it.copy(flow = ScanFlow.Processing) }
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = ImageUtils.loadScaled(getApplication(), uri)
            if (bitmap == null) {
                _state.update { it.copy(flow = ScanFlow.Error("Couldn't open that image. Please try again.")) }
                return@launch
            }
            val fields = StillImageProcessor.process(bitmap)
            // Accept when OCR found the label's key fields (SSCC or batch), or
            // when any barcode on the photo decoded — same bar as live scanning.
            val barcodeDecoded = fields != null && fields.source != com.ssccscanner.core.ScanSource.OCR && !fields.isEmpty
            if (fields == null || (!fields.isUsable && !barcodeDecoded)) {
                _state.update { it.copy(flow = ScanFlow.Error(errorMessage)) }
            } else {
                finalizeScan(fields, bitmap)
            }
        }
    }

    private fun finalizeScan(fields: ScanFields, sourceBitmap: Bitmap?) {
        resetAggregation()
        val timestamp = System.currentTimeMillis()
        val thumbnail = sourceBitmap?.let { ImageUtils.thumbnailJpeg(it) }
        val labelPhoto = sourceBitmap?.let { ImageUtils.labelJpeg(it) }
        // Show the result immediately; persistence completes in the background.
        _state.update {
            it.copy(
                flow = ScanFlow.Result(PendingScan(null, fields, thumbnail, labelPhoto, timestamp)),
                batchCount = if (it.batchMode) it.batchCount + 1 else it.batchCount,
            )
        }
        viewModelScope.launch {
            val saved = repository.addScan(fields, thumbnail, labelPhoto, timestamp)
            _state.update { s ->
                val flow = s.flow
                if (flow is ScanFlow.Result && flow.scan.timestamp == timestamp) {
                    s.copy(flow = ScanFlow.Result(flow.scan.copy(scanId = saved.id)))
                } else {
                    s
                }
            }
        }
    }

    /** Manual corrections from the result screen's edit mode. */
    fun saveEdits(fields: ScanFields) {
        val current = _state.value.flow
        if (current !is ScanFlow.Result) return
        val updated = current.scan.copy(fields = fields)
        _state.update { it.copy(flow = ScanFlow.Result(updated)) }
        updated.scanId?.let { id ->
            viewModelScope.launch { repository.updateScanFields(id, fields) }
        }
    }

    fun scanAgain() {
        resetAggregation()
        _state.update { it.copy(flow = ScanFlow.Ready) }
    }

    fun setBatchMode(on: Boolean) {
        _state.update { it.copy(batchMode = on, batchCount = 0) }
        viewModelScope.launch { repository.setBatchMode(on) }
    }

    fun dismissError() = scanAgain()

    private fun resetAggregation() {
        aggregate = null
        aggregateStartedAt = 0L
        lastDecodedFrame = null
    }

    companion object {
        const val AGGREGATION_WINDOW_MS = 1200L

        fun decodeThumbnail(bytes: ByteArray?): Bitmap? =
            bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }
}
