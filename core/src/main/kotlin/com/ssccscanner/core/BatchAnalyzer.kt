package com.ssccscanner.core

/**
 * Groups a document's pallets by batch number and flags discrepancies:
 * one batch carrying different best-before dates, expired dates, and pallets
 * with no batch number at all.
 *
 * Used for batch-mode documents (a truckload scanned in one go) and,
 * optionally, any library document.
 */
object BatchAnalyzer {

    data class ScanInfo(
        val batchNo: String?,
        val bestBefore: String?,
        val quantity: String?,
    )

    data class BatchGroup(
        val batchNo: String?,          // null = no batch number on the label
        val palletCount: Int,
        val bestBefores: List<String>, // distinct, sorted
        val totalQuantity: Int?,       // sum when every pallet has a numeric qty
    )

    enum class Severity { WARNING, ERROR }

    data class Discrepancy(val severity: Severity, val message: String)

    data class Summary(
        val groups: List<BatchGroup>,
        val discrepancies: List<Discrepancy>,
    ) {
        val batchCount: Int get() = groups.count { it.batchNo != null }
    }

    /**
     * @param todayIso today's date as "yyyy-MM-dd" for expiry checks (kept as
     * a parameter so the logic stays deterministic and testable).
     */
    fun analyze(scans: List<ScanInfo>, todayIso: String): Summary {
        val groups = scans
            .groupBy { it.batchNo?.trim()?.ifEmpty { null } }
            .map { (batch, members) ->
                val dates = members.mapNotNull { normalizeDate(it.bestBefore) }.distinct().sorted()
                val quantities = members.map { it.quantity?.trim()?.toIntOrNull() }
                BatchGroup(
                    batchNo = batch,
                    palletCount = members.size,
                    bestBefores = dates,
                    totalQuantity = if (quantities.isNotEmpty() && quantities.all { it != null }) {
                        quantities.filterNotNull().sum()
                    } else {
                        null
                    },
                )
            }
            // Largest batches first; the "no batch" bucket last
            .sortedWith(compareBy({ it.batchNo == null }, { -it.palletCount }))

        val discrepancies = mutableListOf<Discrepancy>()

        for (g in groups) {
            if (g.batchNo != null && g.bestBefores.size > 1) {
                discrepancies.add(
                    Discrepancy(
                        Severity.ERROR,
                        "Batch ${g.batchNo} has ${g.bestBefores.size} different best-before dates: " +
                            g.bestBefores.joinToString(", "),
                    ),
                )
            }
            val expired = g.bestBefores.filter { isExpired(it, todayIso) }
            if (expired.isNotEmpty()) {
                val label = g.batchNo?.let { "Batch $it" } ?: "Pallets without batch number"
                discrepancies.add(
                    Discrepancy(Severity.ERROR, "$label has EXPIRED best-before: ${expired.joinToString(", ")}"),
                )
            }
        }

        // A missing batch number is NOT flagged: unbatched goods (cups,
        // glasses, storage materials) are a normal part of loads. The
        // "No batch no." group row in the summary is visibility enough.
        return Summary(groups, discrepancies)
    }

    /**
     * Normalize the loosely-formatted best-before strings we store into
     * comparable "yyyy-MM-dd" (or "yyyy-MM") when possible; otherwise return
     * the raw trimmed value so grouping still works on equal strings.
     */
    fun normalizeDate(raw: String?): String? {
        val s = raw?.trim()?.ifEmpty { null } ?: return null

        Regex("""^(\d{4})[-./](\d{1,2})[-./](\d{1,2})$""").find(s)?.let { m ->
            val (y, mo, d) = m.destructured
            return "%04d-%02d-%02d".format(y.toInt(), mo.toInt(), d.toInt())
        }
        Regex("""^(\d{1,2})[-./](\d{1,2})[-./](\d{4})$""").find(s)?.let { m ->
            val (d, mo, y) = m.destructured
            return "%04d-%02d-%02d".format(y.toInt(), mo.toInt(), d.toInt())
        }
        Regex("""^(\d{4})[-./](\d{1,2})$""").find(s)?.let { m ->
            val (y, mo) = m.destructured
            return "%04d-%02d".format(y.toInt(), mo.toInt())
        }
        Regex("""^(\d{2})(\d{2})(\d{2})$""").find(s)?.let { m ->
            // GS1 YYMMDD that slipped through unformatted
            return Gs1Parser.formatGs1Date(s) ?: s
        }
        return s
    }

    /** ISO-shaped dates compare lexically; unparseable values never count as expired. */
    fun isExpired(normalizedDate: String, todayIso: String): Boolean {
        if (!Regex("""^\d{4}-\d{2}(-\d{2})?$""").matches(normalizedDate)) return false
        // "yyyy-MM" (end-of-month per GS1): only expired once the month has passed.
        return if (normalizedDate.length == 7) {
            normalizedDate < todayIso.substring(0, 7)
        } else {
            normalizedDate < todayIso
        }
    }
}
