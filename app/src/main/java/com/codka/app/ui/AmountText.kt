package com.codka.app.ui

import com.codka.app.data.PaymentEntity
import com.codka.app.parser.Currency
import com.codka.app.parser.Provider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * How amounts, senders and times are written on screen (design board 21, "Money"):
 * `$10.50`, `25,500 SLSH`. Pure Kotlin so it can be unit tested.
 */
object AmountText {
    /** 4525 USD -> "$45.25", 500 USD -> "$5.00", 2550000 SLSH -> "25,500 SLSH". */
    fun format(currency: Currency, minor: Long): String = when (currency) {
        Currency.USD -> "$" + number(minor, alwaysCents = true)
        Currency.SLSH -> number(minor, alwaysCents = false) + " SLSH"
    }

    fun format(payment: PaymentEntity) = format(payment.currency, payment.amountMinor)

    /** The number alone ("10.50", "25,500"), for the big amount where the currency is drawn apart. */
    fun number(payment: PaymentEntity) = number(
        payment.amountMinor,
        payment.currency == Currency.USD
    )

    private fun number(minor: Long, alwaysCents: Boolean): String {
        val whole = "%,d".format(Locale.US, minor / 100)
        val cents = minor % 100
        return if (cents == 0L &&
            !alwaysCents
        ) {
            whole
        } else {
            "$whole.${cents.toString().padStart(2, '0')}"
        }
    }

    /** "14:05": 24-hour time, as on the phone's clock. */
    fun time(millis: Long, zone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("HH:mm", Locale.US).apply { timeZone = zone }.format(Date(millis))

    /** "14/11": the day, for history older than yesterday. */
    fun day(millis: Long, zone: TimeZone = TimeZone.getDefault()): String =
        SimpleDateFormat("dd/MM", Locale.US).apply { timeZone = zone }.format(Date(millis))

    fun providerName(provider: Provider) = when (provider) {
        Provider.ZAAD -> "Zaad"
        Provider.EDAHAB -> "eDahab"
    }

    /** "633004521" -> "…4521": enough to recognise a regular customer. Null if too short. */
    fun lastDigits(phone: String): String? =
        phone.filter { it in '0'..'9' }.takeIf { it.length >= 4 }?.let { "…" + it.takeLast(4) }

    /** "AXMED CALI FAARAX" -> "Axmed C.": first name and an initial. Null if there is no name. */
    fun shortName(name: String): String? {
        val words = name.trim().split(Regex("\\s+")).filter { w -> w.any { it.isLetter() } }
        if (words.isEmpty()) return null
        val first = words[0].lowercase().replaceFirstChar { it.uppercase() }
        return if (words.size == 1) first else "$first ${words[1].first().uppercaseChar()}."
    }
}
