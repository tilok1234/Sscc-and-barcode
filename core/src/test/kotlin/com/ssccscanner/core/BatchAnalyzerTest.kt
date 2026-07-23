package com.ssccscanner.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BatchAnalyzerTest {

    private val today = "2026-07-23"

    private fun scan(batch: String?, bb: String?, qty: String? = null) =
        BatchAnalyzer.ScanInfo(batchNo = batch, bestBefore = bb, quantity = qty)

    @Test
    fun `groups a mixed truckload by batch`() {
        val scans =
            List(20) { scan("A1", "2026-12-01", "24") } +
                List(10) { scan("B2", "2027-01-15", "24") } +
                List(3) { scan("C3", "2027-02-01", "24") }
        val s = BatchAnalyzer.analyze(scans, today)

        assertEquals(3, s.groups.size)
        assertEquals("A1", s.groups[0].batchNo)
        assertEquals(20, s.groups[0].palletCount)
        assertEquals(480, s.groups[0].totalQuantity)
        assertEquals(listOf("2026-12-01"), s.groups[0].bestBefores)
        assertTrue(s.discrepancies.isEmpty())
    }

    @Test
    fun `same batch with different best-before dates is an error`() {
        val s = BatchAnalyzer.analyze(
            listOf(scan("A1", "2026-12-01"), scan("A1", "2027-01-01")),
            today,
        )
        assertEquals(1, s.discrepancies.size)
        assertEquals(BatchAnalyzer.Severity.ERROR, s.discrepancies[0].severity)
        assertTrue(s.discrepancies[0].message.contains("A1"))
        assertTrue(s.discrepancies[0].message.contains("2 different best-before dates"))
    }

    @Test
    fun `expired best-before is flagged`() {
        val s = BatchAnalyzer.analyze(listOf(scan("A1", "2026-07-01")), today)
        assertEquals(1, s.discrepancies.size)
        assertTrue(s.discrepancies[0].message.contains("EXPIRED"))
    }

    @Test
    fun `year-month dates only expire after the month ends`() {
        assertTrue(BatchAnalyzer.isExpired("2026-06", today))
        assertTrue(!BatchAnalyzer.isExpired("2026-07", today))
    }

    @Test
    fun `pallets without batch number are a warning when batches exist`() {
        val s = BatchAnalyzer.analyze(
            listOf(scan("A1", "2026-12-01"), scan(null, "2026-12-01"), scan(null, null)),
            today,
        )
        assertTrue(s.discrepancies.any { it.severity == BatchAnalyzer.Severity.WARNING && it.message.contains("no batch number") })
        // no-batch bucket sorts last
        assertEquals(null, s.groups.last().batchNo)
    }

    @Test
    fun `date formats normalize before comparison`() {
        // Same date written two ways should NOT be a discrepancy
        val s = BatchAnalyzer.analyze(
            listOf(scan("A1", "2026-12-01"), scan("A1", "01-12-2026")),
            today,
        )
        assertTrue(s.discrepancies.isEmpty())
        assertEquals(listOf("2026-12-01"), s.groups[0].bestBefores)
    }

    @Test
    fun `unparseable dates never count as expired`() {
        assertTrue(!BatchAnalyzer.isExpired("soon", today))
        val s = BatchAnalyzer.analyze(listOf(scan("A1", "soon")), today)
        assertTrue(s.discrepancies.isEmpty())
    }

    @Test
    fun `total quantity is null when any pallet lacks a count`() {
        val s = BatchAnalyzer.analyze(listOf(scan("A1", null, "24"), scan("A1", null, null)), today)
        assertEquals(null, s.groups[0].totalQuantity)
    }
}
