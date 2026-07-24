package com.ssccscanner.core

/**
 * Merges fields decoded from barcodes with fields extracted from OCR text.
 * Barcode data is authoritative — a decoded symbol has error correction and
 * check digits behind it — OCR only fills the gaps (e.g. a quantity that is
 * printed as plain text but not encoded in any barcode).
 */
object FieldMerger {

    fun merge(barcode: ScanFields?, ocr: ScanFields?): ScanFields {
        if (barcode == null && ocr == null) return ScanFields()
        if (barcode == null) return ocr!!
        if (ocr == null) return barcode

        val merged = ScanFields(
            sscc = barcode.sscc ?: ocr.sscc,
            batchNo = barcode.batchNo ?: ocr.batchNo,
            gtin = barcode.gtin ?: ocr.gtin,
            bestBefore = barcode.bestBefore ?: ocr.bestBefore,
            quantity = barcode.quantity ?: ocr.quantity,
            articleNo = barcode.articleNo ?: ocr.articleNo,
            confidence = if (barcode.confidence.ordinal <= ocr.confidence.ordinal) barcode.confidence else ocr.confidence,
            source = if (usedAnyOcrField(barcode, ocr)) ScanSource.MIXED else ScanSource.BARCODE,
        )
        return merged
    }

    /** Combine multiple barcode results from one frame/photo (labels carry several symbols). */
    fun combineBarcodes(results: List<ScanFields>): ScanFields? {
        if (results.isEmpty()) return null
        return results.reduce { acc, next ->
            acc.copy(
                sscc = acc.sscc ?: next.sscc,
                batchNo = acc.batchNo ?: next.batchNo,
                gtin = acc.gtin ?: next.gtin,
                bestBefore = acc.bestBefore ?: next.bestBefore,
                quantity = acc.quantity ?: next.quantity,
                articleNo = acc.articleNo ?: next.articleNo,
                confidence = if (next.confidence.ordinal < acc.confidence.ordinal) next.confidence else acc.confidence,
            )
        }
    }

    private fun usedAnyOcrField(barcode: ScanFields, ocr: ScanFields): Boolean {
        return (barcode.sscc == null && ocr.sscc != null) ||
            (barcode.batchNo == null && ocr.batchNo != null) ||
            (barcode.gtin == null && ocr.gtin != null) ||
            (barcode.bestBefore == null && ocr.bestBefore != null) ||
            (barcode.quantity == null && ocr.quantity != null) ||
            (barcode.articleNo == null && ocr.articleNo != null)
    }
}
