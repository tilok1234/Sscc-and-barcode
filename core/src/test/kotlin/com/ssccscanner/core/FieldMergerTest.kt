package com.ssccscanner.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FieldMergerTest {

    @Test
    fun `barcode fields win, ocr fills gaps`() {
        val barcode = ScanFields(
            sscc = "306141411234567891",
            gtin = "00614141123452",
            confidence = Confidence.HIGH,
            source = ScanSource.BARCODE,
        )
        val ocr = ScanFields(
            sscc = "306141411234567899", // OCR misread — must lose to barcode
            batchNo = "AB-1234",
            quantity = "24",
            confidence = Confidence.MEDIUM,
            source = ScanSource.OCR,
        )
        val m = FieldMerger.merge(barcode, ocr)
        assertEquals("306141411234567891", m.sscc)
        assertEquals("00614141123452", m.gtin)
        assertEquals("AB-1234", m.batchNo)
        assertEquals("24", m.quantity)
        assertEquals(Confidence.HIGH, m.confidence)
        assertEquals(ScanSource.MIXED, m.source)
    }

    @Test
    fun `pure barcode result stays barcode-sourced`() {
        val barcode = ScanFields(sscc = "306141411234567891", confidence = Confidence.HIGH, source = ScanSource.BARCODE)
        val ocr = ScanFields(sscc = "306141411234567891", confidence = Confidence.LOW, source = ScanSource.OCR)
        assertEquals(ScanSource.BARCODE, FieldMerger.merge(barcode, ocr).source)
    }

    @Test
    fun `null sides pass through`() {
        val ocr = ScanFields(batchNo = "B1", source = ScanSource.OCR)
        assertEquals(ocr, FieldMerger.merge(null, ocr))
        val barcode = ScanFields(sscc = "306141411234567891", source = ScanSource.BARCODE)
        assertEquals(barcode, FieldMerger.merge(barcode, null))
        assertEquals(ScanFields(), FieldMerger.merge(null, null))
    }

    @Test
    fun `multiple barcodes from one label combine`() {
        val sscc = ScanFields(sscc = "306141411234567891", confidence = Confidence.HIGH, source = ScanSource.BARCODE)
        val gtin = ScanFields(gtin = "6141411234528", confidence = Confidence.HIGH, source = ScanSource.BARCODE)
        val c = FieldMerger.combineBarcodes(listOf(sscc, gtin))!!
        assertEquals("306141411234567891", c.sscc)
        assertEquals("6141411234528", c.gtin)
        assertNull(FieldMerger.combineBarcodes(emptyList()))
    }
}
