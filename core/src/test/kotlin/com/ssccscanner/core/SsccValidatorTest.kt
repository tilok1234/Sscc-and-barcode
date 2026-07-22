package com.ssccscanner.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SsccValidatorTest {

    // Extension digit 3, GS1 company prefix 0614141, serial 123456789 → check digit 1
    private val validSscc = "306141411234567891"

    @Test
    fun `valid sscc passes`() {
        assertEquals(SsccValidator.Status.OK, SsccValidator.status(validSscc))
        assertTrue(SsccValidator.isValid(validSscc))
    }

    @Test
    fun `whitespace is tolerated`() {
        assertEquals(SsccValidator.Status.OK, SsccValidator.status("3 06141411 23456789 1"))
    }

    @Test
    fun `bad check digit is distinct from wrong length`() {
        assertEquals(SsccValidator.Status.BAD_CHECK_DIGIT, SsccValidator.status("306141411234567890"))
        assertEquals(SsccValidator.Status.WRONG_LENGTH, SsccValidator.status("30614141123456789"))
        assertEquals(SsccValidator.Status.WRONG_LENGTH, SsccValidator.status("3061414112345678911"))
        assertEquals(SsccValidator.Status.WRONG_LENGTH, SsccValidator.status(""))
        assertEquals(SsccValidator.Status.WRONG_LENGTH, SsccValidator.status(null))
        assertEquals(SsccValidator.Status.WRONG_LENGTH, SsccValidator.status("30614141123456789A"))
    }

    @Test
    fun `check digit computation`() {
        assertEquals(1, SsccValidator.checkDigit("30614141123456789"))
    }

    @Test
    fun `every single-digit corruption is caught`() {
        for (pos in 0 until 18) {
            for (delta in 1..9) {
                val corrupted = validSscc.toCharArray()
                corrupted[pos] = ('0' + ((corrupted[pos] - '0' + delta) % 10))
                assertFalse(SsccValidator.isValid(String(corrupted)), "corruption at $pos+$delta not caught")
            }
        }
    }

    @Test
    fun `gtin check digits validate`() {
        assertTrue(SsccValidator.isValidGtin("00614141123452"))  // GTIN-14 (padded UPC-A)
        assertTrue(SsccValidator.isValidGtin("614141123452"))    // UPC-A / GTIN-12
        assertFalse(SsccValidator.isValidGtin("614141123453"))
        assertFalse(SsccValidator.isValidGtin("12345"))
    }
}
