package com.ssccscanner.data

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ssccscanner.core.CsvBuilder
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object CsvExport {

    /** Build the document's CSV, write it to cache, and hand back a share intent. */
    fun shareIntent(
        context: Context,
        documentName: String,
        scans: List<ScanEntity>,
        damagedScanIds: Set<String> = emptySet(),
    ): Intent {
        val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val csv = CsvBuilder.build(
            scans.map { s ->
                CsvBuilder.Row(
                    sscc = s.sscc,
                    batchNo = s.batchNo,
                    gtin = s.gtin,
                    bestBefore = s.bestBefore,
                    quantity = s.quantity,
                    confidence = s.confidence,
                    source = s.source,
                    edited = s.edited,
                    scannedAtIso = iso.format(Date(s.timestamp)),
                    damaged = s.id in damagedScanIds,
                )
            },
        )

        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, CsvBuilder.fileNameFor(documentName))
        file.writeText(csv)

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "$documentName — scans")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
