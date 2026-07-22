package com.ssccscanner.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BarcodeInterpreterTest {

    @Test
    fun `ean-13 payload becomes gtin`() {
        val f = BarcodeInterpreter.interpret("6141411234528", "EAN_13")!!
        assertEquals("6141411234528", f.gtin)
        assertEquals(Confidence.HIGH, f.confidence)
        assertEquals(ScanSource.BARCODE, f.source)
    }

    @Test
    fun `itf-14 payload becomes gtin`() {
        val f = BarcodeInterpreter.interpret("00614141123452", "ITF")!!
        assertEquals("00614141123452", f.gtin)
    }

    @Test
    fun `bare 18-digit sscc in plain code 128`() {
        val f = BarcodeInterpreter.interpret("306141411234567891", "CODE_128")!!
        assertEquals("306141411234567891", f.sscc)
        assertEquals(Confidence.HIGH, f.confidence)
    }

    @Test
    fun `gs1-128 with aim identifier`() {
        val f = BarcodeInterpreter.interpret("]C100306141411234567891", "CODE_128")!!
        assertEquals("306141411234567891", f.sscc)
    }

    @Test
    fun `gs1 datamatrix payload`() {
        val f = BarcodeInterpreter.interpret(
            "]d20100614141123452" + Gs1Parser.GS + "10LOT7",
            "DATA_MATRIX",
        )!!
        assertEquals("00614141123452", f.gtin)
        assertEquals("LOT7", f.batchNo)
    }

    @Test
    fun `non-gs1 payloads rejected`() {
        assertNull(BarcodeInterpreter.interpret("https://example.com", "QR_CODE"))
        assertNull(BarcodeInterpreter.interpret("hello", "CODE_128"))
        assertNull(BarcodeInterpreter.interpret(null, "CODE_128"))
        assertNull(BarcodeInterpreter.interpret("", "EAN_13"))
    }
}
