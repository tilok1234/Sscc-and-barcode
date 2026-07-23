package com.ssccscanner.scan

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * Feeds live camera frames to ML Kit's on-device barcode scanner and reports
 * every decoded symbol, along with a bitmap of the decoding frame (kept as the
 * scan's label photo). Frame closing is tied to scanner completion so CameraX
 * back-pressure (KEEP_ONLY_LATEST) naturally throttles analysis.
 */
class BarcodeAnalyzer(
    private val onBarcodes: (List<DetectedBarcode>, Bitmap?) -> Unit,
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_ITF,
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_AZTEC,
            )
            .build(),
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    // Convert only frames that decoded something — this frame
                    // becomes the stored photo of the label. Must happen before
                    // the proxy closes (the complete listener runs after us).
                    val frame = runCatching {
                        val raw = imageProxy.toBitmap()
                        val degrees = imageProxy.imageInfo.rotationDegrees
                        if (degrees != 0) {
                            Bitmap.createBitmap(
                                raw, 0, 0, raw.width, raw.height,
                                Matrix().apply { postRotate(degrees.toFloat()) },
                                true,
                            )
                        } else {
                            raw
                        }
                    }.getOrNull()
                    onBarcodes(barcodes.map { it.toDetected() }, frame)
                }
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
