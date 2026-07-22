package com.ssccscanner.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CsvBuilderTest {

    @Test
    fun `builds header and quote-escaped rows`() {
        val csv = CsvBuilder.build(
            listOf(
                CsvBuilder.Row(
                    sscc = "306141411234567891",
                    batchNo = "AB,12\"3",
                    gtin = null,
                    bestBefore = "2026-07-31",
                    quantity = "24",
                    confidence = "high",
                    source = "barcode",
                    edited = true,
                    scannedAtIso = "2026-07-22T08:00:00Z",
                ),
            ),
        )
        val lines = csv.trim().split('\n')
        assertEquals("SSCC,Batch No,GTIN/EAN,Best Before,Quantity,Confidence,Source,Edited,Scanned At", lines[0])
        assertEquals(
            "306141411234567891,\"AB,12\"\"3\",,2026-07-31,24,high,barcode,yes,2026-07-22T08:00:00Z",
            lines[1],
        )
    }

    @Test
    fun `filename slugging`() {
        assertEquals("truck-7-tuesday.csv", CsvBuilder.fileNameFor("Truck 7 / Tuesday"))
        assertEquals("scans.csv", CsvBuilder.fileNameFor("///"))
    }

    @Test
    fun `empty rows produce just the header`() {
        val csv = CsvBuilder.build(emptyList())
        assertTrue(csv.startsWith("SSCC,"))
        assertEquals(1, csv.trim().split('\n').size)
    }
}
