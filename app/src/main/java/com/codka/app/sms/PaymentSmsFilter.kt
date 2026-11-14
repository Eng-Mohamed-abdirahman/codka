package com.codka.app.sms

/**
 * Tells whether an SMS comes from Zaad or eDahab at all (payment or not). Used only to pick
 * which SMS debug builds print as raw samples. Deciding what is a *payment* is the parser's job.
 */
object PaymentSmsFilter {
    enum class Provider { ZAAD, EDAHAB }

    fun providerOf(body: String): Provider? = when {
        body.contains("ZAAD SERVICES") || body.contains("ZAAD SHILLING") -> Provider.ZAAD
        body.contains("ADEEGA SARIFKA") -> Provider.ZAAD
        body.contains("[eDahab Service-") -> Provider.EDAHAB
        else -> null
    }
}
