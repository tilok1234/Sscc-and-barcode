package com.ssccscanner.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class Gs1ParserTest {

    private val GS = Gs1Parser.GS

    @Test
    fun `plain sscc element string`() {
        val r = Gs1Parser.parse("00306141411234567891")
        assertNotNull(r)
        assertEquals("306141411234567891", r.fields.sscc)
        assertEquals(Confidence.HIGH, r.fields.confidence)
        assertEquals(ScanSource.BARCODE, r.fields.source)
    }

    @Test
    fun `gs1-128 payload with aim prefix and multiple ais`() {
        // (01) GTIN, (10) batch (variable, GS-terminated), (37) count
        val r = Gs1Parser.parse("]C10100614141123452" + "10LOT-42" + GS + "3712")
        assertNotNull(r)
        assertEquals("00614141123452", r.fields.gtin)
        assertEquals("LOT-42", r.fields.batchNo)
        assertEquals("12", r.fields.quantity)
        assertNull(r.fields.sscc)
    }

    @Test
    fun `full logistics label payload`() {
        val r = Gs1Parser.parse(
            "00306141411234567891" + "0100614141123452" + "15260731" + "10ABC123",
            currentYear = 2026,
        )
        assertNotNull(r)
        assertEquals("306141411234567891", r.fields.sscc)
        assertEquals("00614141123452", r.fields.gtin)
        assertEquals("2026-07-31", r.fields.bestBefore)
        assertEquals("ABC123", r.fields.batchNo)
        assertEquals(Confidence.HIGH, r.fields.confidence)
    }

    @Test
    fun `parenthesized human readable form`() {
        val r = Gs1Parser.parse("(00)306141411234567891(10)LOT42(17)261231", currentYear = 2026)
        assertNotNull(r)
        assertEquals("306141411234567891", r.fields.sscc)
        assertEquals("LOT42", r.fields.batchNo)
        assertEquals("2026-12-31", r.fields.bestBefore)
    }

    @Test
    fun `expiry 17 used when no best-before 15`() {
        val r = Gs1Parser.parse("17270101" + "10B1", currentYear = 2026)
        assertNotNull(r)
        assertEquals("2027-01-01", r.fields.bestBefore)
    }

    @Test
    fun `date with day 00 renders as year-month`() {
        assertEquals("2027-02", Gs1Parser.formatGs1Date("270200", currentYear = 2026))
    }

    @Test
    fun `year pivot wraps backwards for old dates`() {
        assertEquals("1999-05-01", Gs1Parser.formatGs1Date("990501", currentYear = 2026))
        assertEquals("2049-05-01", Gs1Parser.formatGs1Date("490501", currentYear = 2026))
    }

    @Test
    fun `non-gs1 payload returns null or empty`() {
        assertNull(Gs1Parser.parse("https://example.com/x"))
        assertNull(Gs1Parser.parse(""))
    }

    @Test
    fun `quantity leading zeros trimmed`() {
        val r = Gs1Parser.parse("370012" + GS + "10B2")
        assertNotNull(r)
        assertEquals("12", r.fields.quantity)
    }
}
