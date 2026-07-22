package com.ssccscanner.scan

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.ssccscanner.core.BarcodeInterpreter
import com.ssccscanner.core.FieldMerger
import com.ssccscanner.core.LabelTextParser
import com.ssccscanner.core.ScanFields
import kotlinx.coroutines.tasks.await

/**
 * The OCR-fallback path for captured or uploaded photos: run barcode detection
 * AND text recognition over the still, then merge (barcode fields win, OCR
 * fills gaps).
 */
object StillImageProcessor {

    suspend fun process(bitmap: Bitmap): ScanFields? {
        val input = InputImage.fromBitmap(bitmap, 0)

        val barcodeFields: ScanFields? = try {
            val barcodes = BarcodeScanning.getClient().process(input).await()
            FieldMerger.combineBarcodes(
                barcodes.mapNotNull { b ->
                    val d = b.toDetected()
                    BarcodeInterpreter.interpret(d.rawValue, d.formatName)
                },
            )
        } catch (_: Exception) {
            null
        }

        val ocrFields: ScanFields? = try {
            val text = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(input).await()
            // ML Kit v2 exposes per-line confidence; average what's available.
            val confidences = text.textBlocks.flatMap { it.lines }.mapNotNull { it.confidence }
            val avgConfidence = if (confidences.isEmpty()) 80f else confidences.average().toFloat() * 100f
            LabelTextParser.extractFields(text.text, avgConfidence).takeUnless { it.isEmpty }
        } catch (_: Exception) {
            null
        }

        if (barcodeFields == null && ocrFields == null) return null
        return FieldMerger.merge(barcodeFields, ocrFields)
    }
}
