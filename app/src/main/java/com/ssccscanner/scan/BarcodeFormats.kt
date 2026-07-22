package com.ssccscanner.scan

import com.google.mlkit.vision.barcode.common.Barcode

/** ML Kit format constants → the symbology names core's BarcodeInterpreter understands. */
fun barcodeFormatName(format: Int): String = when (format) {
    Barcode.FORMAT_CODE_128 -> "CODE_128"
    Barcode.FORMAT_CODE_39 -> "CODE_39"
    Barcode.FORMAT_CODE_93 -> "CODE_93"
    Barcode.FORMAT_CODABAR -> "CODABAR"
    Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
    Barcode.FORMAT_EAN_13 -> "EAN_13"
    Barcode.FORMAT_EAN_8 -> "EAN_8"
    Barcode.FORMAT_ITF -> "ITF"
    Barcode.FORMAT_QR_CODE -> "QR_CODE"
    Barcode.FORMAT_UPC_A -> "UPC_A"
    Barcode.FORMAT_UPC_E -> "UPC_E"
    Barcode.FORMAT_PDF417 -> "PDF417"
    Barcode.FORMAT_AZTEC -> "AZTEC"
    else -> "UNKNOWN"
}

/**
 * A decoded symbol, detached from ML Kit types so it can flow into core logic.
 * rawValue falls back to Latin-1 decoding of rawBytes so embedded GS (0x1D)
 * separators survive.
 */
data class DetectedBarcode(val rawValue: String?, val formatName: String)

fun Barcode.toDetected(): DetectedBarcode {
    val value = rawValue ?: rawBytes?.toString(Charsets.ISO_8859_1)
    return DetectedBarcode(value, barcodeFormatName(format))
}
