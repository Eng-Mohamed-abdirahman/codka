package com.codka.app.ui

import com.codka.app.data.PaymentEntity
import com.codka.app.parser.Currency
import com.codka.app.parser.Provider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** How amounts and times are written on screen. Pure Kotlin so it can be unit tested. */
object AmountText {
    /** 4525 USD -> "$45.25", 200000 USD -> "$2,000", 125000000 SLSH -> "SLSH 1,250,000". */
    fun format(currency: Currency, minor: Long): String {
        val whole = "%,d".format(Locale.US, minor / 100)
        val cents = minor % 100
        val number = if (cents == 0L) whole else "$whole.${cents.toString().padStart(2, '0')}"
        return when (currency) {
            Currency.USD -> "$$number"
            Currency.SLSH -> "SLSH $number"
        }
    }

    fun format(payment: PaymentEntity) = format(payment.currency, payment.amountMinor)

    /** "14:05": 24-hour time, as on the phone's clock. */
    fun time(millis: Long, zone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = zone }.format(Date(millis))

    fun providerName(provider: Provider) = when (provider) {
        Provider.ZAAD -> "ZAAD"
        Provider.EDAHAB -> "eDahab"
    }

    /**
     * Today's totals, one per currency (dollars first), e.g. "$165.25 · SLSH 25,000".
     * Suspicious payments are left out: they may not be real money. Null if nothing came in.
     */
    fun totals(payments: List<PaymentEntity>): String? {
        val sums =
            payments
                .filterNot { it.isSuspicious }
                .groupBy { it.currency }
                .mapValues { (_, list) -> list.sumOf { it.amountMinor } }
        return Currency.entries
            .mapNotNull { currency -> sums[currency]?.let { format(currency, it) } }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(" · ")
    }
}
