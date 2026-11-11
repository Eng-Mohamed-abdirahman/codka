package com.codka.app.parser

/**
 * Tasks 11–12: turns a Zaad or eDahab payment SMS into a [ParsedPayment].
 * Returns null for anything that isn't a known payment format.
 */
class PaymentParser(private val formats: List<SmsFormat>) {
    fun parse(body: String): ParsedPayment? {
        for (format in formats) {
            if (!body.contains(format.marker)) continue
            val match = format.pattern.find(body) ?: continue
            val (amount, name, phone) = match.destructured
            return ParsedPayment(
                provider = format.provider,
                currency = format.currency,
                amount = amount.replace(",", ""),
                senderName = name.trim().replace(WHITESPACE, " "),
                senderPhone = phone
            )
        }
        return null
    }

    private companion object {
        val WHITESPACE = Regex("""\s+""")
    }
}
