package com.ssccscanner.core

/**
 * GS1 mod-10 check digit validation for SSCC (18 digits).
 *
 * The two failure states are deliberately distinct — the UI shows a different
 * message for a wrong-length SSCC than for a check-digit mismatch.
 */
object SsccValidator {

    enum class Status { OK, WRONG_LENGTH, BAD_CHECK_DIGIT }

    /** Strips whitespace, then validates length and GS1 mod-10 check digit. */
    fun status(input: String?): Status {
        val digits = input?.filterNot(Char::isWhitespace).orEmpty()
        if (digits.length != 18 || digits.any { !it.isDigit() }) return Status.WRONG_LENGTH
        return if (checkDigit(digits.substring(0, 17)) == digits[17].digitToInt()) {
            Status.OK
        } else {
            Status.BAD_CHECK_DIGIT
        }
    }

    fun isValid(input: String?): Boolean = status(input) == Status.OK

    /**
     * GS1 mod-10 check digit over a 17-digit body: weight 3 on even 0-based
     * positions, 1 on odd.
     */
    fun checkDigit(body17: String): Int {
        require(body17.length == 17 && body17.all(Char::isDigit)) {
            "check digit requires exactly 17 digits"
        }
        var sum = 0
        for (i in 0..16) {
            val weight = if (i % 2 == 0) 3 else 1
            sum += weight * body17[i].digitToInt()
        }
        return (10 - (sum % 10)) % 10
    }

    /** Generic GS1 mod-10 validation for GTIN-8/12/13/14 (weights from the right). */
    fun isValidGtin(input: String?): Boolean {
        val digits = input?.filterNot(Char::isWhitespace).orEmpty()
        if (digits.length !in intArrayOf(8, 12, 13, 14) || digits.any { !it.isDigit() }) return false
        var sum = 0
        // Excluding the check digit, weight 3 applies to positions counted
        // odd-from-the-right (1-based), same rule for every GTIN length.
        for (i in 0 until digits.length - 1) {
            val posFromRight = digits.length - 1 - i // 1-based distance from check digit
            val weight = if (posFromRight % 2 == 1) 3 else 1
            sum += weight * digits[i].digitToInt()
        }
        return (10 - (sum % 10)) % 10 == digits.last().digitToInt()
    }
}
