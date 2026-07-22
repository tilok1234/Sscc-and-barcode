package com.ssccscanner.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LabelTextParserTest {

    @Test
    fun `typical labeled shipping label`() {
        val text = """
            GS1 LOGISTICS LABEL
            SSCC: 3 06141411 23456789 1
            Batch no: AB-1234
            EAN NO: 00614141123452
            Best before: 31-07-2026
            Quantity: 24
        """.trimIndent()

        val f = LabelTextParser.extractFields(text, ocrConfidencePercent = 88f)
        assertEquals("306141411234567891", f.sscc)
        assertEquals("AB-1234", f.batchNo)
        assertEquals("00614141123452", f.gtin)
        assertEquals("31-07-2026", f.bestBefore)
        assertEquals("24", f.quantity)
        assertEquals(Confidence.HIGH, f.confidence)
        assertEquals(ScanSource.OCR, f.source)
    }

    @Test
    fun `sscc chosen by check digit among noisy candidates`() {
        // First run has a corrupted digit (check digit fails), second validates.
        val text = """
            306141411234567890
            306141411234567891
        """.trimIndent()
        val f = LabelTextParser.extractFields(text, 60f)
        assertEquals("306141411234567891", f.sscc)
        assertEquals(Confidence.MEDIUM, f.confidence) // valid check digit but low OCR confidence
    }

    @Test
    fun `falls back to gs1 application identifiers in text`() {
        val text = "(10)LOT9 (15)260731"
        val f = LabelTextParser.extractFields(text, 70f)
        assertEquals("LOT9", f.batchNo)
        assertEquals("260731", f.bestBefore)
        assertNull(f.sscc)
        assertTrue(f.isUsable)
        assertEquals(Confidence.LOW, f.confidence)
    }

    @Test
    fun `gtin found as free-standing run not inside sscc`() {
        val text = """
            SSCC 306141411234567891
            07350053850019
        """.trimIndent()
        val f = LabelTextParser.extractFields(text, 90f)
        assertEquals("306141411234567891", f.sscc)
        assertEquals("07350053850019", f.gtin)
    }

    @Test
    fun `unusable when nothing found`() {
        val f = LabelTextParser.extractFields("FRAGILE\nTHIS SIDE UP", 95f)
        assertNull(f.sscc)
        assertNull(f.batchNo)
        assertTrue(!f.isUsable)
    }

    @Test
    fun `best before with bbd label and iso date`() {
        val f = LabelTextParser.extractFields("BBD 2026-07-31", 80f)
        assertEquals("2026-07-31", f.bestBefore)
    }
}
