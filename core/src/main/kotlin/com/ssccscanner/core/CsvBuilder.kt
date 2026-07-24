package com.ssccscanner.core

/**
 * CSV export per the handoff: one file per document, quote-escaped values,
 * ISO timestamps. Column set extended with Source (barcode/OCR provenance).
 */
object CsvBuilder {

    data class Row(
        val sscc: String?,
        val batchNo: String?,
        val gtin: String?,
        val bestBefore: String?,
        val quantity: String?,
        val confidence: String,
        val source: String,
        val edited: Boolean,
        val scannedAtIso: String,
        val damaged: Boolean = false,
        val articleNo: String? = null,
    )

    private val HEADER = listOf(
        "SSCC", "Batch No", "GTIN/EAN", "Article No", "Best Before", "Quantity", "Confidence", "Source", "Edited", "Damaged", "Scanned At",
    )

    fun build(rows: List<Row>): String = buildString {
        appendLine(HEADER.joinToString(","))
        for (r in rows) {
            appendLine(
                listOf(
                    r.sscc.orEmpty(),
                    r.batchNo.orEmpty(),
                    r.gtin.orEmpty(),
                    r.articleNo.orEmpty(),
                    r.bestBefore.orEmpty(),
                    r.quantity.orEmpty(),
                    r.confidence,
                    r.source,
                    if (r.edited) "yes" else "no",
                    if (r.damaged) "yes" else "no",
                    r.scannedAtIso,
                ).joinToString(",") { escape(it) },
            )
        }
    }

    /** Derive a safe filename from a document name, e.g. "Truck 7 / Tuesday" → "truck-7-tuesday.csv". */
    fun fileNameFor(documentName: String): String {
        val slug = documentName.lowercase()
            .replace(Regex("""[^a-z0-9]+"""), "-")
            .trim('-')
            .ifEmpty { "scans" }
        return "$slug.csv"
    }

    private fun escape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
}
