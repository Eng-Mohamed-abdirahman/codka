package com.codka.app.data

/** Converts amounts as the SMS writes them ("45.25") to minor units (4525) and back, exactly. */
object Money {
    private val AMOUNT = Regex("""(\d+)(?:\.(\d{0,2}))?""")

    // Far more than any real payment, and keeps whole * 100 from overflowing.
    private const val MAX_WHOLE = 1_000_000_000_000L

    /** "45.25" -> 4525, "12.5" -> 1250, "2,000" -> 200000. Null if it isn't an exact amount. */
    fun toMinor(amount: String): Long? {
        val match = AMOUNT.matchEntire(amount.replace(",", "").trim()) ?: return null
        val whole = match.groupValues[1].toLongOrNull() ?: return null
        if (whole > MAX_WHOLE) return null
        val cents = match.groupValues[2].padEnd(2, '0').toLong()
        return whole * 100 + cents
    }

    /** 4525 -> "45.25", 1250 -> "12.50", 200000 -> "2000". */
    fun fromMinor(minor: Long): String {
        val whole = minor / 100
        val cents = minor % 100
        return if (cents == 0L) "$whole" else "$whole.${cents.toString().padStart(2, '0')}"
    }
}
