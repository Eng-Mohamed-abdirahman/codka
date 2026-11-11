package com.codka.app.sms

/**
 * Task 07 spike: decides whether an SMS body looks like a Zaad or eDahab
 * "money received" message. Deliberately simple; the real parsers come in tasks 10–12.
 */
object PaymentSmsFilter {
    enum class Provider { ZAAD, EDAHAB }

    fun providerOf(body: String): Provider? = when {
        body.contains("ZAAD SERVICES") || body.contains("ZAAD SHILLING") -> Provider.ZAAD
        body.contains("[eDahab Service-") -> Provider.EDAHAB
        else -> null
    }

    fun isPayment(body: String): Boolean {
        val provider = providerOf(body) ?: return false
        return when (provider) {
            Provider.ZAAD -> body.contains("ka heshay")
            Provider.EDAHAB -> body.contains("Ayaad ka Heshay")
        }
    }
}
