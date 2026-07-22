package com.ssccscanner.core

/**
 * Turns one decoded barcode (raw payload + symbology) into scan fields.
 * Format names follow ML Kit's `Barcode.FORMAT_*` naming but are passed as
 * strings so this module stays free of Android dependencies.
 */
object BarcodeInterpreter {

    /** Symbologies whose payload is (potentially) a GS1 element string. */
    private val GS1_CAPABLE = setOf("CODE_128", "DATA_MATRIX", "QR_CODE", "AZTEC", "GS1_128", "CODABAR", "PDF417")

    /** Symbologies whose payload is a bare GTIN. */
    private val GTIN_FORMATS = setOf("EAN_13", "EAN_8", "UPC_A", "UPC_E", "ITF")

    fun interpret(rawValue: String?, formatName: String, currentYear: Int? = null): ScanFields? {
        val raw = rawValue?.trim().orEmpty()
        if (raw.isEmpty()) return null

        if (formatName in GTIN_FORMATS) {
            val digits = raw.filter(Char::isDigit)
            // ITF-14 on logistics labels carries the trade item GTIN-14; EAN-13 the consumer GTIN.
            return if (digits.length in 8..14) {
                ScanFields(gtin = digits, confidence = Confidence.HIGH, source = ScanSource.BARCODE)
            } else {
                null
            }
        }

        // A bare 18-digit payload that validates as an SSCC (an SSCC encoded
        // without its AI, e.g. in plain Code 128/39) beats AI-guessing.
        val digits = raw.filter(Char::isDigit)
        if (digits.length == 18 && digits.length == raw.length && SsccValidator.isValid(digits)) {
            return ScanFields(sscc = digits, confidence = Confidence.HIGH, source = ScanSource.BARCODE)
        }

        if (formatName in GS1_CAPABLE || raw.startsWith("]") || raw.startsWith("(")) {
            val parsed = if (currentYear != null) Gs1Parser.parse(raw, currentYear) else Gs1Parser.parse(raw)
            val fields = parsed?.fields ?: return null
            return if (fields.isEmpty) null else fields
        }
        return null
    }
}
