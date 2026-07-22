package com.ssccscanner.core

/** How confident we are in the extracted fields, mirrored from the prototype's 3-level model. */
enum class Confidence { HIGH, MEDIUM, LOW }

/** Where a scan's fields came from. */
enum class ScanSource { BARCODE, OCR, MIXED, MANUAL }

/**
 * The fields we extract from a shipping label. All nullable — a label rarely
 * carries every field.
 */
data class ScanFields(
    val sscc: String? = null,        // 18 digits, no separators
    val batchNo: String? = null,
    val gtin: String? = null,        // 8–14 digits as printed/encoded
    val bestBefore: String? = null,  // as read/typed or ISO-formatted from GS1 date
    val quantity: String? = null,
    val confidence: Confidence = Confidence.LOW,
    val source: ScanSource = ScanSource.OCR,
) {
    val isEmpty: Boolean
        get() = sscc == null && batchNo == null && gtin == null && bestBefore == null && quantity == null

    /** The prototype treats a scan with neither SSCC nor batch as a failed read. */
    val isUsable: Boolean
        get() = sscc != null || batchNo != null
}
