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
        val rotation = imageProxy.imageInfo.rotationDegrees
        val input = InputImage.fromMediaImage(mediaImage, rotation)
        // Upright frame dimensions (ML Kit reports boxes in rotated coordinates)
        val frameW = if (rotation % 180 != 0) imageProxy.height else imageProxy.width
        val frameH = if (rotation % 180 != 0) imageProxy.width else imageProxy.height
        scanner.process(input)
            .addOnSuccessListener { all ->
                // Aim gating: only accept symbols whose center falls in the
                // middle of the frame (≈ the on-screen reticle). Lets the user
                // pick one barcode out of several on a dense label wall.
                val barcodes = all.filter { b ->
                    val box = b.boundingBox ?: return@filter true
                    val cx = box.exactCenterX()
                    val cy = box.exactCenterY()
                    cx >= frameW * 0.12f && cx <= frameW * 0.88f &&
                        cy >= frameH * 0.20f && cy <= frameH * 0.80f
                }
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
