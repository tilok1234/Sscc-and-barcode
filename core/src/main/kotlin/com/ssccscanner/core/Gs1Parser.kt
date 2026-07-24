package com.ssccscanner.core

/**
 * Parses GS1 element strings — the data encoded in GS1-128, GS1 DataMatrix and
 * GS1 QR barcodes on shipping labels.
 *
 * Accepts the three shapes a scanner library typically hands back:
 *  1. Raw concatenated element string with ASCII GS (0x1D) separators, e.g.
 *     "0030614141123456789110LOT42<GS>3706"
 *  2. The same with an AIM symbology identifier prefix ("]C1", "]d2", "]Q3", "]e0")
 *  3. Human-readable parenthesized form, e.g. "(00)306141411234567891(10)LOT42"
 */
object Gs1Parser {

    const val GS: Char = '\u001D'

    /** Fixed data lengths for AIs whose values are not GS-terminated (GS1 GenSpec table). */
    private val FIXED_LENGTH_AIS: Map<String, Int> = buildMap {
        put("00", 18) // SSCC
        put("01", 14) // GTIN
        put("02", 14) // GTIN of contained items
        put("03", 14)
        put("11", 6)  // production date YYMMDD
        put("12", 6)  // due date
        put("13", 6)  // packaging date
        put("15", 6)  // best before
        put("16", 6)  // sell by
        put("17", 6)  // expiry
        put("18", 6)
        put("19", 6)
        put("20", 2)  // internal product variant
        put("410", 13); put("411", 13); put("412", 13); put("413", 13)
        put("414", 13); put("415", 13); put("416", 13); put("417", 13)
    }

    /** AIs we surface as scan fields. Everything else is kept in the raw map. */
    data class Result(
        val ais: Map<String, String>,
        val fields: ScanFields,
    )

    /**
     * Parse a raw barcode payload. Returns null when the payload doesn't look
     * like GS1 element data at all (e.g. a plain URL in a QR code).
     */
    fun parse(rawValue: String, currentYear: Int = defaultYear()): Result? {
        if (rawValue.isBlank()) return null
        var s = rawValue.trim()

        // AIM symbology identifier, e.g. "]C1" (GS1-128), "]d2" (GS1 DataMatrix), "]Q3" (GS1 QR)
        if (s.length > 3 && s[0] == ']') s = s.substring(3)
        // Some decoders emit a leading FNC1 as GS
        s = s.trimStart(GS)

        val ais: Map<String, String> = if (s.contains('(') && PAREN_RE.containsMatchIn(s)) {
            parseParenthesized(s)
        } else {
            parseElementString(s)
        }
        if (ais.isEmpty()) return null
        return Result(ais, toFields(ais, currentYear))
    }

    private val PAREN_RE = Regex("""\((\d{2,4})\)\s*([^(]*)""")

    private fun parseParenthesized(s: String): Map<String, String> {
        val out = linkedMapOf<String, String>()
        for (m in PAREN_RE.findAll(s)) {
            val ai = m.groupValues[1]
            val value = m.groupValues[2].trim().trimEnd(GS)
            if (value.isNotEmpty()) out[ai] = value
        }
        return out
    }

    private fun parseElementString(s: String): Map<String, String> {
        val out = linkedMapOf<String, String>()
        var i = 0
        while (i < s.length) {
            while (i < s.length && s[i] == GS) i++
            if (i >= s.length) break

            val ai = matchAi(s, i) ?: return out // stop at anything unparseable, keep what we have
            i += ai.length

            val fixed = fixedLengthFor(ai)
            val value: String
            if (fixed != null) {
                if (i + fixed > s.length) {
                    // Truncated payload — take what's there and stop.
                    value = s.substring(i)
                    i = s.length
                } else {
                    value = s.substring(i, i + fixed)
                    i += fixed
                }
            } else {
                // Variable length: value runs to the next GS separator (or end).
                val end = s.indexOf(GS, i).let { if (it == -1) s.length else it }
                value = s.substring(i, end)
                i = end
            }
            if (value.isNotEmpty()) out[ai] = value
        }
        return out
    }

    /**
     * Match the AI at position [i]. AIs are prefix-free; try 2 then 3 then 4
     * digits, accepting the first that is a known AI shape.
     */
    private fun matchAi(s: String, i: Int): String? {
        if (i + 2 > s.length) return null
        val two = s.substring(i, i + 2)
        if (two.any { !it.isDigit() }) return null

        // Known 2-digit AIs (fixed table above plus common variable ones)
        if (two in FIXED_LENGTH_AIS || two in VARIABLE_2DIGIT_AIS) return two

        // Measures 310x–369x and other 4-digit AIs with a decimal-point digit
        if (i + 4 <= s.length) {
            val four = s.substring(i, i + 4)
            if (four.all(Char::isDigit) && four.substring(0, 2).toInt() in 31..36) return four
        }
        // 3-digit AIs
        if (i + 3 <= s.length) {
            val three = s.substring(i, i + 3)
            if (three.all(Char::isDigit) &&
                (three in FIXED_LENGTH_AIS || three in VARIABLE_3DIGIT_AIS)
            ) {
                return three
            }
        }
        // Fall back to accepting the 2-digit AI as variable-length; keeps the
        // parser permissive for AIs outside our table.
        return two
    }

    private val VARIABLE_2DIGIT_AIS = setOf("10", "21", "22", "30", "37", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99")
    private val VARIABLE_3DIGIT_AIS = setOf("240", "241", "242", "250", "251", "253", "254", "255", "400", "401", "402", "403", "420", "421", "422", "423", "424", "425", "426")

    private fun fixedLengthFor(ai: String): Int? {
        FIXED_LENGTH_AIS[ai]?.let { return it }
        // Metric measures 31xx–36xx carry 6 data digits
        if (ai.length == 4 && ai.substring(0, 2).toIntOrNull() in 31..36) return 6
        return null
    }

    /** Map parsed AIs onto our scan fields. */
    fun toFields(ais: Map<String, String>, currentYear: Int = defaultYear()): ScanFields {
        val sscc = ais["00"]?.filter(Char::isDigit)
        val gtin = (ais["01"] ?: ais["02"])?.filter(Char::isDigit)
        val batch = ais["10"]?.trim()
        // Prefer explicit best-before (15); fall back to expiry (17), then sell-by (16)
        val dateRaw = ais["15"] ?: ais["17"] ?: ais["16"]
        val bestBefore = dateRaw?.let { formatGs1Date(it, currentYear) }
        val quantity = ais["37"]?.trimStart('0')?.ifEmpty { "0" }
        // (240)/(241) = additional/customer product identification — the usual
        // home of a company article number on relabeled pallets
        val articleNo = (ais["240"] ?: ais["241"])?.trim()

        val ssccValid = sscc != null && SsccValidator.isValid(sscc)
        val confidence = when {
            sscc != null && ssccValid -> Confidence.HIGH
            sscc != null -> Confidence.MEDIUM // decoded but check digit fails (rare; usually mis-encoded label)
            gtin != null || batch != null -> Confidence.HIGH // barcode decode is reliable even without an SSCC
            else -> Confidence.LOW
        }

        return ScanFields(
            sscc = sscc,
            batchNo = batch,
            gtin = gtin,
            bestBefore = bestBefore,
            quantity = quantity,
            articleNo = articleNo,
            confidence = confidence,
            source = ScanSource.BARCODE,
        )
    }

    /**
     * GS1 YYMMDD → "YYYY-MM-DD" (or "YYYY-MM" when DD is 00, which GS1 defines
     * as "end of month"). Year pivot follows GS1: the year is interpreted
     * within the window (currentYear - 49)..(currentYear + 50).
     */
    fun formatGs1Date(yymmdd: String, currentYear: Int = defaultYear()): String? {
        val d = yymmdd.filter(Char::isDigit)
        if (d.length != 6) return null
        val yy = d.substring(0, 2).toInt()
        val mm = d.substring(2, 4).toInt()
        val dd = d.substring(4, 6).toInt()
        if (mm !in 1..12 || dd > 31) return null

        val century = currentYear / 100
        var year = century * 100 + yy
        if (year - currentYear > 50) year -= 100
        if (currentYear - year > 49) year += 100

        return if (dd == 0) {
            "%04d-%02d".format(year, mm)
        } else {
            "%04d-%02d-%02d".format(year, mm, dd)
        }
    }

    private fun defaultYear(): Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
}
