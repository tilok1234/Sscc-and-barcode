package com.ssccscanner.core

/**
 * Deterministic field extraction from OCR'd label text.
 *
 * This is a 1:1 Kotlin port of `extractFields` from the offline web prototype
 * (docs/design_handoff/SSCC Scanner (offline on-device version).dc.html) —
 * regex pattern matching over recognized text, with GS1 check-digit scoring to
 * pick the best SSCC candidate out of noisy OCR.
 */
object LabelTextParser {

    /**
     * @param text raw recognized text (newline-separated lines)
     * @param ocrConfidencePercent overall OCR engine confidence, 0–100
     */
    fun extractFields(text: String, ocrConfidencePercent: Float): ScanFields {
        val lines = text.split('\n').map(String::trim).filter(String::isNotEmpty)
        val joined = lines.joinToString("\n")

        // --- SSCC: 18-digit runs (tolerating grouping spaces and an optional
        // leading "(00)"), preferring a candidate whose check digit validates.
        val digitRuns = mutableListOf<String>()
        val runRe = Regex("""\(?00\)?[\s.:]*([0-9][0-9 ]{16,40})|([0-9][0-9 ]{16,40})""")
        for (m in runRe.findAll(joined)) {
            val raw = (m.groupValues[1].ifEmpty { m.groupValues[2] }).replace(Regex("""\s+"""), "")
            var start = 0
            while (start + 18 <= raw.length) {
                digitRuns.add(raw.substring(start, start + 18))
                start++
            }
        }
        var sscc: String? = null
        var ssccValid = false
        val validOnes = digitRuns.filter { SsccValidator.isValid(it) }
        if (validOnes.isNotEmpty()) {
            sscc = validOnes.first()
            ssccValid = true
        } else if (digitRuns.isNotEmpty()) {
            sscc = digitRuns.first()
        }

        fun lineValue(re: Regex): String? {
            for (l in lines) {
                val mm = re.find(l)
                if (mm != null) return mm.groupValues.getOrNull(1)?.trim()?.ifEmpty { null }
            }
            return null
        }

        // --- Batch: labeled line, else GS1 AI (10)
        var batchNo = lineValue(Regex("""batch(?:\s*(?:no|number))?\s*[.:]?\s*([A-Za-z0-9\-/ ]{2,24})""", RegexOption.IGNORE_CASE))
        if (batchNo == null) {
            batchNo = Regex("""\(10\)\s*([A-Za-z0-9\-]{2,20})""").find(joined)?.groupValues?.get(1)
        }
        batchNo = batchNo?.trim()

        // --- GTIN/EAN: labeled line, else GS1 AI (01)/(02), else a free-standing
        // 13/14-digit run that isn't part of the SSCC we already found.
        var gtin = lineValue(Regex("""(?:ean|gtin|content)(?:\s*(?:no|nr|number))?\s*[.:]?\s*([0-9][0-9 ]{7,24})""", RegexOption.IGNORE_CASE))
            ?.replace(Regex("""\s+"""), "")
        if (gtin == null) {
            gtin = Regex("""\(0[12]\)\s*([0-9]{8,14})""").find(joined)?.groupValues?.get(1)
        }
        if (gtin == null) {
            val candidates = Regex("""\b(\d{13,14})\b""").findAll(joined.replace(Regex("""\s+"""), " "))
                .map { it.groupValues[1] }
            gtin = candidates.firstOrNull { sscc == null || !sscc.contains(it) }
        }

        // --- Best before: a date-shaped token on a line with a best-before-ish
        // label, else GS1 AI (15)/(17) YYMMDD.
        val dateRe = Regex("""(\d{1,2}[-./ ]\d{1,2}[-./ ]\d{2,4}|\d{4}[-./]\d{1,2}[-./]\d{1,2}|\d{8})""")
        var bestBefore: String? = null
        for (l in lines) {
            if (Regex("""best\s*before|bbd|expiry|use\s*by|bäst\s*före""", RegexOption.IGNORE_CASE).containsMatchIn(l)) {
                bestBefore = dateRe.find(l)?.groupValues?.get(1)
                break
            }
        }
        if (bestBefore == null) {
            bestBefore = Regex("""\(1[57]\)\s*(\d{6})""").find(joined)?.groupValues?.get(1)
        }

        val quantity = lineValue(Regex("""(?:quantity|qty|count|antal)\s*[.:]?\s*([0-9]+)""", RegexOption.IGNORE_CASE))

        // Article number: "Art.nr", "Artikkel", "Article no", "Item no" — else GS1 AI (240)/(241)
        var articleNo = lineValue(
            Regex(
                """(?:art(?:ikkel|icle)?|item)\s*\.?\s*(?:no|nr|number)?\s*[.:]?\s*([A-Za-z0-9\-]{3,20})""",
                RegexOption.IGNORE_CASE,
            ),
        )
        if (articleNo == null) {
            articleNo = Regex("""\(24[01]\)\s*([A-Za-z0-9\-]{2,25})""").find(joined)?.groupValues?.get(1)
        }

        val confidence = when {
            ssccValid && ocrConfidencePercent >= 80f -> Confidence.HIGH
            ssccValid || ocrConfidencePercent >= 75f -> Confidence.MEDIUM
            else -> Confidence.LOW
        }

        return ScanFields(
            sscc = sscc,
            batchNo = batchNo,
            gtin = gtin,
            bestBefore = bestBefore,
            quantity = quantity,
            articleNo = articleNo,
            confidence = confidence,
            source = ScanSource.OCR,
        )
    }
}
